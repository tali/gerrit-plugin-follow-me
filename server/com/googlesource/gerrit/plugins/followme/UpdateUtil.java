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

package com.googlesource.gerrit.plugins.followme;

import com.google.common.flogger.FluentLogger;

import com.google.gerrit.entities.Change;
import com.google.gerrit.entities.PatchSet;
import com.google.gerrit.extensions.api.changes.NotifyHandling;
import com.google.gerrit.extensions.restapi.BadRequestException;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.ChangeUtil;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
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

import java.lang.IllegalArgumentException;
import java.io.IOException;
import java.util.List;
import org.eclipse.jgit.lib.CommitBuilder;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.MutableObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.TreeFormatter;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;


@Singleton
class UpdateUtil {

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final PatchSetInserter.Factory patchSetInserterFactory;
  private final ChangeKindCache changeKindCache;
  private final NotifyResolver notifyResolver;
  private final BatchUpdate.Factory updateFactory;

  @Inject
  UpdateUtil(
      PatchSetInserter.Factory patchSetInserterFactory,
      BatchUpdate.Factory updateFactory,
      NotifyResolver notifyResolver,
      ChangeKindCache changeKindCache) {
    this.patchSetInserterFactory = patchSetInserterFactory;
    this.changeKindCache = changeKindCache;
    this.updateFactory = updateFactory;
    this.notifyResolver = notifyResolver;
  }

  RevCommit getReferenceCommit(Repository repo, RevWalk rw, String refName) throws IOException {
    Ref refRef = repo.findRef(refName);
    if (refRef == null)
      return null;

    ObjectId refId = repo.findRef(refName).getObjectId();
    if (refId == null)
      return null;

    return rw.parseCommit(refId);
  }

  private boolean hasChangeId(String message, int start) {
    return message.indexOf("\nChange-Id:", start) >= 0;
  }

  public String insertFooters(String oldMessage, String key, String values) {
    final StringBuilder message = new StringBuilder();

    // search for start and end of any existing footer within the last paragraph
    int start = oldMessage.lastIndexOf("\n\n");
    boolean existingFooter = false;
    if (start >= 0) {
      existingFooter = hasChangeId(oldMessage, start + 1);
      start = oldMessage.indexOf("\n" + key + ":", start + 1);
    }
    if (start >= 0) {
        // include message before old footer
        message.append(oldMessage.substring(0, start + 1));
    } else {
      // we already have footers, but not for our key
      message.append(oldMessage);
    }
    if (!existingFooter) {
      // start new footer paragraph
      message.append("\n");
    }
    // append our new footer entries
    for (String value : values.split("\n")) {
      value = value.strip();
      if (value.isEmpty()) continue;

      message.append(key);
      message.append(": ");
      message.append(value);
      message.append("\n");
    }
    // append the rest of the original footers
    while (start >= 0) {
      // skip this line
      start = oldMessage.indexOf("\n", start + 1);
      // copy everything up to the next footer with our key
      int next = oldMessage.indexOf("\n" + key + ":", start);
      if (next > 0) {
        message.append(oldMessage.substring(start + 1, next + 1));
        start = next;
      } else {
        message.append(oldMessage.substring(start + 1));
        start = -1;
      }
    }

    return message.toString();
  }

  public int createPatchset(
        Repository repo, RevWalk rw, ObjectInserter inserter,
        CurrentUser user,
        Change change, RevCommit updated, String patchsetMsg,
        ChangeNotes notes
  ) throws IOException, BadRequestException, ConfigInvalidException, UpdateException, RestApiException {
    PatchSet.Id psId = ChangeUtil.nextPatchSetId(repo, change.currentPatchSetId());

    StringBuilder builder = new StringBuilder("Created patch set ").append(psId.get()).append(": ");
    builder.append(patchsetMsg);
    String message = builder.toString();

    PatchSetInserter patchset =
        patchSetInserterFactory
            .create(notes, psId, updated)
            .setSendEmail(!change.isWorkInProgress())
            .setMessage(patchsetMsg);

    try (BatchUpdate bu = updateFactory.create(change.getProject(), user, TimeUtil.now())) {
      bu.setRepository(repo, rw, inserter);
      bu.setNotify(notifyResolver.resolve(NotifyHandling.ALL, null));
      bu.addOp(change.getId(), patchset);
      bu.execute();
    }
    logger.atInfo().log(patchsetMsg);

    return psId.get();
  }

  public static RevCommit getCurrentCommit(Repository repo, RevWalk rw, Change change) throws IOException {
    return rw.parseCommit(repo.exactRef(change.currentPatchSetId().toRefName()).getObjectId());
  }

};