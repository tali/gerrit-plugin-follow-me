// Copyright (C) 2022 Siemens Mobility GmbH
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.reviewtarget;

import com.google.common.flogger.FluentLogger;

import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.update.UpdateException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.time.Instant;
import java.time.ZoneId;
import org.eclipse.jgit.dircache.DirCache;
import org.eclipse.jgit.dircache.DirCacheBuilder;
import org.eclipse.jgit.dircache.DirCacheEntry;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.ignore.FastIgnoreRule;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.NameConflictTreeWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.errors.ConfigInvalidException;

import static java.util.Objects.requireNonNull;


class UpdateTree implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Change change;
  private final Repository repo;
  private final RevWalk rw;
  private final ObjectReader reader;
  private final ObjectInserter inserter;
  private final UpdateUtil updateUtil;
  private RevCommit current;
  private RevCommit target;
  private RevCommit followBranch;
  private String reviewTarget;
  boolean validReviewTarget;
  private List<FastIgnoreRule> reviewRules;
  private String reviewFiles;
  private ObjectId updatedTree;

  UpdateTree(Repository repo, Change change, UpdateUtil updateUtil) {
    this.change = requireNonNull(change);
    this.updateUtil = requireNonNull(updateUtil);
    this.repo = requireNonNull(repo);
    this.inserter = requireNonNull(repo.newObjectInserter());
    this.reader = requireNonNull(inserter.newReader());
    this.rw = new RevWalk(reader);
  }

  public void close() {
    rw.close();
    reader.close();
    inserter.close();
    repo.close();
  }

  public void newReviewTarget(String targetName) throws IOException {
    reviewTarget = targetName;
    current = UpdateUtil.getCurrentCommit(repo, rw, change);
    target = updateUtil.getReferenceCommit(repo, rw, reviewTarget);
    validReviewTarget = target != null;
  }

  public void useReviewTargetFooter(String footerName) throws IOException {
    current = UpdateUtil.getCurrentCommit(repo, rw, change);
    List<String> footerLines = current.getFooterLines(footerName);
    if (footerLines.size() == 0) {
      validReviewTarget = false;
      return;
    }
    reviewTarget = footerLines.get(0);
    target = updateUtil.getReferenceCommit(repo, rw, reviewTarget);
    validReviewTarget = target != null;
  }

  public void useFollowBranch(String branchName) throws IOException {
    followBranch = updateUtil.getReferenceCommit(repo, rw, branchName);
    if (followBranch == null) {
      logger.atWarning().log("followBranch %s does not exist", branchName);
    }
  }

  public String getReviewTarget() {
    return reviewTarget;
  }

  public boolean isValidReviewTarget() {
    return validReviewTarget;
  }

  private void newReviewFileRules(List<String> lines) {
    List<FastIgnoreRule> rules = new ArrayList<>();
    for (String line : lines) {
      line = line.strip();
      if (line.isEmpty()) continue;
      rules.add(new FastIgnoreRule(line));
    }
    reviewRules = rules;
  }

  public void newReviewFiles(String lines) {
    reviewFiles = lines;
    newReviewFileRules(Arrays.asList(lines.split("\n")));
  }

  /**
   * populate `reviewFiles` based on the commit message.
   */
  public void useReviewFilesFooter(String footerName) {
    List<String> lines = current.getFooterLines(footerName);
    reviewFiles = String.join("\n", lines);
    newReviewFileRules(lines);
  }

  public String getReviewFiles() {
    return reviewFiles;
  }

  enum Selected { NO_MATCH, POSITIVE, NEGATIVE }

  /**
   * check if this path matches our given filter
   */
  Selected isPathToBeReviewed(String path, boolean isDirectory) {
    // Parse rules in the reverse order that they were read because later
    // rules have higher priority
    for (int i = reviewRules.size() - 1; i > -1; i--) {
      FastIgnoreRule rule = reviewRules.get(i);
      if (rule.isMatch(path, isDirectory, true)) {
        return rule.getResult() ? Selected.POSITIVE : Selected.NEGATIVE;
      }
    }
    // no rule matches
    return Selected.NO_MATCH;
  }

  /**
   * Walk all paths and choose elements from either the parent or the target tree
   */
  void rewritePaths() throws IOException {
    RevTree targetTree = rw.parseTree(target.getTree());
    if (reviewRules.size() == 0) {
      // Without a Review-Files specification, use the whole Review-Target
      this.updatedTree = targetTree;
      return;
    }

    RevCommit parent = rw.parseCommit(current.getParent(0));
    RevTree parentTree = rw.parseTree(parent.getTree());

    DirCache cache = DirCache.newInCore();
    DirCacheBuilder builder = cache.builder();

    MutableObjectId oid = new MutableObjectId();

    TreeWalk walk = new NameConflictTreeWalk(repo, reader);
    int idPar = walk.addTree(parentTree);
    int idTar = walk.addTree(targetTree);

    while (walk.next()) {

      int id;
      boolean isSubtree = walk.isSubtree();
      String path = walk.getPathString();
      Selected selected = isPathToBeReviewed(path, isSubtree);

      if (selected == Selected.POSITIVE) {
        id = idTar;
      } else {
        id = idPar;
      }
      if (isSubtree && selected == Selected.NO_MATCH) {
        // not decided yet, have to check individual contents of tree
        walk.enterSubtree();
      } else if (isSubtree) {
        // add whole directory
        walk.getObjectId(oid, id);
        builder.addTree(walk.getRawPath(), 0, reader, oid);
      } else {
        // add individual file
        FileMode mode = walk.getFileMode(id);
        if (!mode.equals(FileMode.TYPE_MISSING)) {
          DirCacheEntry e = new DirCacheEntry(walk.getRawPath(), 0);
          walk.getObjectId(oid, id);
          e.setObjectId(oid);
          e.setFileMode(mode);
          builder.add(e);
        }
      }
    }
    builder.finish();

    this.updatedTree = cache.writeTree(inserter);
  }

  boolean hasCurrentPaths() throws IOException {
    RevTree currentTree = rw.parseTree(current.getTree());
    return this.updatedTree.equals(currentTree);
  }
  /**
   * Walk all paths and TBD
   */
  void getChangedPaths(List<String> added, List<String> updated, List<String> removed) throws IOException {
    current = UpdateUtil.getCurrentCommit(repo, rw, change);
    RevCommit parent = rw.parseCommit(current.getParent(0));

    RevTree currentTree = rw.parseTree(current.getTree());
    RevTree parentTree = rw.parseTree(parent.getTree());

    TreeWalk walk = new NameConflictTreeWalk(repo, reader);
    int idOld = walk.addTree(currentTree);
    int idNew = walk.addTree(updatedTree);
    int idPar = walk.addTree(parentTree);
    walk.setRecursive(true);

    while (walk.next()) {

      FileMode modeOld = walk.getFileMode(idOld);
      FileMode modeNew = walk.getFileMode(idNew);

      if (walk.idEqual(idOld, idNew) && (modeOld == modeNew)) {
        // not changed
        continue;
      }

      FileMode modePar = walk.getFileMode(idPar);

      boolean sameOld = walk.idEqual(idOld, idPar) && (modeOld == modePar);
      boolean sameNew = walk.idEqual(idNew, idPar) && (modeNew == modePar);
      String path = walk.getPathString();

      if (sameOld) {
        added.add(path);
      } else if (sameNew) {
        removed.add(path);
      } else {
        updated.add(path);
      }
    }
  }

  private String _getVersion(RevCommit commit, String prefix, String dropPrefix) throws IOException {
    assert commit != null;
    for (Ref ref : repo.getRefDatabase().getTipsWithSha1(commit)) {
      var name = ref.getName();
      if (name.startsWith(prefix)) {
        if (name.startsWith(dropPrefix)) {
          name = name.substring(dropPrefix.length());
        }
        return name;
      }
    }

    return reviewTarget;
  }

  public String getTargetVersion(String prefix, String dropPrefix) throws IOException {
    return _getVersion(target, prefix, dropPrefix);
  }

  public String getFollowVersion(String prefix, String dropPrefix) throws IOException {
    if (followBranch == null) {
      return "";
    }
    return _getVersion(followBranch, prefix, dropPrefix);
  }

  public int createPatchSet(
        CurrentUser user, String reviewTargetFooter, String reviewFilesFooter, ChangeNotes notes
  ) throws IOException, ConfigInvalidException, UpdateException, RestApiException {
    String currentMessage = current.getFullMessage();
    String message = getUpdatedMessage(currentMessage, reviewTargetFooter, reviewFilesFooter);
    boolean sameMsg = message.equals(current.getFullMessage());
    boolean sameTree = updatedTree.equals(current.getTree());

    if (sameMsg && sameTree) {
      return 0;
    }

    RevCommit updated = getUpdatedCommit(user, message);
    String patchSetMsg = getPatchSetMessage(sameTree);

    return updateUtil.createPatchSet(repo, rw, inserter, user, change, updated, patchSetMsg, notes);
  }

  private String getUpdatedMessage(String original, String reviewTargetFooter, String reviewFilesFooter) {
    String message = original;

    message = updateUtil.insertFooters(message, reviewTargetFooter, reviewTarget);
    message = updateUtil.insertFooters(message, reviewFilesFooter, reviewFiles);

    return message;
  }

  private String getPatchSetMessage(boolean sameTree) {
    if (sameTree) {
      return "Updated commit message.";
    }

    return "Updated files based on " + reviewTarget + ".";
  }

  private RevCommit getUpdatedCommit(CurrentUser user, String message) throws IOException {
    ZoneId tz = current.getCommitterIdent().getZoneId();
    PersonIdent committer = user.asIdentifiedUser().newCommitterIdent(Instant.now(), tz);

    CommitBuilder updated = new CommitBuilder();

    updated.setAuthor(current.getAuthorIdent());
    updated.setCommitter(committer);
    updated.setMessage(message);
    updated.setParentIds(current.getParents());
    updated.setTreeId(updatedTree);

    return rw.parseCommit(commit(updated));
  }

  private ObjectId commit(CommitBuilder builder)
      throws IOException {
    ObjectId id = inserter.insert(builder);
    inserter.flush();
    return id;
  }
}
