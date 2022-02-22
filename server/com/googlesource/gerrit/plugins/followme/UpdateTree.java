// Copyright (C) 2013 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.followme;

import com.google.common.flogger.FluentLogger;

import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.change.ChangeKindCache;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.change.NotifyResolver;
import com.google.gerrit.server.change.PatchSetInserter;
import com.google.gerrit.server.notedb.ChangeNotes;
import com.google.gerrit.server.update.BatchUpdate;
import com.google.gerrit.server.update.UpdateException;
import com.google.gerrit.server.util.time.TimeUtil;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Collection;
import java.util.TimeZone;
import java.time.Instant;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.PersonIdent;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;


class UpdateTree implements AutoCloseable {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final Change change;
  private final Repository repo;
  private final RevWalk rw;
  private final ObjectReader reader;
  private final ObjectInserter inserter;
  private final UpdateUtil updateUtil;
  private RevCommit current;
  private RevCommit followme;
  private ObjectId updatedTree;
  private Collection<String> changedPaths;

  UpdateTree(Repository repo, Change change, UpdateUtil updateUtil) {
    this.change = change;
    this.updateUtil = updateUtil;
    this.repo = repo;
    this.inserter = repo.newObjectInserter();
    this.reader = inserter.newReader();
    this.rw = new RevWalk(reader);
  }

  public void close() {
    rw.close();
    reader.close();
    inserter.close();
    repo.close();
  }

  void follow(String reference, Collection<String> changedPaths) throws IOException {
    current = updateUtil.getCurrentCommit(repo, rw, change);
    assert(current.getParentCount() == 1);
    RevCommit parent = rw.parseCommit(current.getParent(0));
    followme = updateUtil.getReferenceCommit(repo, rw, reference);

    RevTree currentTree = rw.parseTree(current.getTree());
    RevTree parentTree = rw.parseTree(parent.getTree());
    RevTree followTree = rw.parseTree(followme.getTree());
    logger.atSevere().log("updatedTree  currentTree=%s parentTree=%s followTree=%s", currentTree, parentTree, followTree);

    this.changedPaths = changedPaths;
    this.updatedTree = getUpdatedTree3("", currentTree, parentTree, followTree);
  }

  public String getVersion(String prefix, String notFound) throws IOException {
    for (Ref ref : repo.getRefDatabase().getTipsWithSha1(followme)) {
      var name = ref.getName();
      if (name.startsWith(prefix)) {
        return name;
      }
    }

    return notFound;
  }

  public final boolean isCurrent() {
    return updatedTree.equals(current.getTree());
  }

  public PatchSet.Id createPatchset(
        CurrentUser user, String footer, String version, ChangeNotes notes
  ) throws IOException, BadRequestException, ConfigInvalidException, UpdateException, RestApiException {
    PatchSet.Id psId = ChangeUtil.nextPatchSetId(repo, change.currentPatchSetId());
    RevCommit updated = getUpdatedCommit(user, footer, version);

    return updateUtil.createPatchset(repo, rw, inserter, user, change, updated, version, notes);
  }

  private final RevCommit getUpdatedCommit(CurrentUser user, String footer, String version) throws IOException {
    TimeZone tz = current.getCommitterIdent().getTimeZone();
    PersonIdent committer = user.asIdentifiedUser().newCommitterIdent(Instant.now(), tz);

    CommitBuilder updated = new CommitBuilder();

    updated.setAuthor(current.getAuthorIdent());
    updated.setCommitter(committer);
    updated.setMessage(updateUtil.insertFooter(current.getFullMessage(), footer, version));
    updated.setParentIds(current.getParents());
    updated.setTreeId(updatedTree);

    return rw.parseCommit(commit(updated));
  }

  private final ObjectId getUpdatedTree3(String prefix, ObjectId current, ObjectId parent, ObjectId followme)
      throws IOException {
//    logger.atSevere().log("updatedTree current=%s parent=%s followme=%s", current, parent, followme);
    // try to reuse a complete tree
    if (current.equals(parent))
      return current;
    if (current.equals(followme))
      return current;
    if (parent.equals(followme))
      return followme;

    // we have to take a deeper look at the tree contents

    TreeFormatter updated = new TreeFormatter();
    MutableObjectId oid = new MutableObjectId();

    TreeWalk walk = new TreeWalk(repo, reader);
    int idCur = walk.addTree(current);
    int idPar = walk.addTree(parent);
    int idFol = walk.addTree(followme);
    walk.setRecursive(false);

    while (walk.next()) {
      FileMode modeCur = walk.getFileMode(idCur);
      FileMode modePar = walk.getFileMode(idPar);
      FileMode modeFol = walk.getFileMode(idFol);
//      logger.atSevere().log("- updatedTree %s", walk.getPathString());

      if (modeCur == FileMode.MISSING && modePar == FileMode.MISSING) {
        // entry is not part of this change, leave it alone
        continue;
      }
      if (walk.idEqual(idCur, idPar) && (modeCur == modePar)) {
        // entry is not part of this change, leave it alone
        walk.getObjectId(oid, idCur);
        updated.append(walk.getRawPath(), modeCur, oid);
        continue;
      }
      String path = prefix + walk.getPathString(); // TBD slow
      if (modeCur == FileMode.TREE && modePar == FileMode.TREE && modeFol == FileMode.TREE) {
        // recurse into subtree
        ObjectId subtree = getUpdatedTree3(path + "/", walk.getObjectId(idCur), walk.getObjectId(idPar), walk.getObjectId(idFol));
        updated.append(walk.getRawPath(), FileMode.TREE, subtree);
        continue;
      }
      if (modeCur == FileMode.TREE && modeFol == FileMode.TREE) {
        // recurse into subtree
        ObjectId subtree = getUpdatedTree2(path + "/",  walk.getObjectId(idCur), walk.getObjectId(idFol));
        updated.append(walk.getRawPath(), FileMode.TREE, subtree);
        continue;
      }
      // update this entry
      if (changedPaths != null && (!walk.idEqual(idCur, idFol) || modeCur != modeFol)) {
        changedPaths.add(path);
      }
      if (modeFol == FileMode.MISSING) {
        logger.atSevere().log("- removedPath3 '%s'", path);
      } else {
        walk.getObjectId(oid, idFol);
        updated.append(walk.getRawPath(), modeFol, oid);
        logger.atSevere().log("- changedPath3 '%s'", path);
      }
    }
    return tree(updated);
  }

  private final ObjectId getUpdatedTree2(String prefix, ObjectId current, ObjectId followme)
      throws IOException {
//    logger.atSevere().log("updatedTree current=%s parent=%s followme=%s", current, parent, followme);
    // try to reuse a complete tree
    if (current.equals(followme))
      return current;

    // we have to take a deeper look at the tree contents

    TreeFormatter updated = new TreeFormatter();
    MutableObjectId oid = new MutableObjectId();

    TreeWalk walk = new TreeWalk(repo, reader);
    int idCur = walk.addTree(current);
    int idFol = walk.addTree(followme);
    walk.setRecursive(false);

    while (walk.next()) {
      FileMode modeCur = walk.getFileMode(idCur);
      FileMode modeFol = walk.getFileMode(idFol);
//      logger.atSevere().log("- updatedTree %s", walk.getPathString());

      if (modeCur == FileMode.MISSING) {
        // entry is not part of this change, leave it alone
        continue;
      }
      String path = prefix + walk.getPathString(); // TBD slow
      if (modeCur == FileMode.TREE && modeFol == FileMode.TREE) {
        // recurse into subtree
        ObjectId subtree = getUpdatedTree2(path + "/", walk.getObjectId(idCur), walk.getObjectId(idFol));
        updated.append(walk.getRawPath(), FileMode.TREE, subtree);
        continue;
      }
      // update this entry
      if (changedPaths != null) {
        changedPaths.add(path);
      }
      if (modeFol == FileMode.MISSING) {
        logger.atSevere().log("- removedPath2 '%s'", path);
      } else {
        walk.getObjectId(oid, idFol);
        updated.append(walk.getRawPath(), modeFol, oid);
        logger.atSevere().log("- changedPath2 '%s'", path);
      }
    }
    return tree(updated);
  }

  private final ObjectId commit(CommitBuilder builder)
      throws IOException {
    ObjectId id = inserter.insert(builder);
    inserter.flush();
    return id;
  }

  private final ObjectId tree(TreeFormatter formatter)
      throws IOException {
    ObjectId id = inserter.insert(formatter);
    inserter.flush();
    return id;
  }
};
