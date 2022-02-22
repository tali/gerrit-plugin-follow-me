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
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.change.NotifyResolver;
import com.google.gerrit.server.update.BatchUpdate;
import com.google.gerrit.server.update.UpdateException;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;
import org.eclipse.jgit.lib.ObjectInserter;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevTree;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.errors.ConfigInvalidException;
import org.eclipse.jgit.errors.RepositoryNotFoundException;

import com.googlesource.gerrit.plugins.followme.ChangeFollow.Input;
import com.googlesource.gerrit.plugins.followme.ChangeFollow.FollowInfo;

@Singleton
class ChangeFollow implements RestModifyView<ChangeResource, Input> {
  static class Input {
    String reference;
    boolean doUpdate;
    boolean returnVersion;
    boolean returnChanges;
  }
  static class FollowInfo {
    boolean onReviewBranch;
    boolean canUpdate;
    int newPatchsetId;
    String version;
    Collection<String> changedPaths;
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager gitManager;
  private final Configuration cfg;
  private final UpdateUtil updateUtil;

  @Inject
  ChangeFollow(
      GitRepositoryManager gitManager,
      Configuration cfg,
      UpdateUtil updateUtil) {
    this.gitManager = gitManager;
    this.cfg = cfg;
    this.updateUtil = updateUtil;
  }

  @Override
  public Response<FollowInfo> apply(ChangeResource rsrc, Input input) throws IOException, RestApiException, ConfigInvalidException, UpdateException {
//    preConditions.assertUpdatePermission(rsrc);
//    preConditions.assertCanBeUpdated(rsrc, input);

    logger.atSevere().log("FollowMe input %s", input);
    FollowInfo resp = new FollowInfo();
    Change change = rsrc.getChange();
    resp.onReviewBranch = (change.getDest().branch().equals(cfg.getReviewBranch()));
    if (!resp.onReviewBranch) {
      logger.atSevere().log("FollowMe wrong branch %s != %s", change.getDest().branch(), cfg.getReviewBranch());
      resp.canUpdate = false;
      return Response.ok(resp);
    }
    CurrentUser user = rsrc.getUser();

    String reference = input.reference;
    if (reference == null) {
      reference = cfg.getFollowBranch();
    }
    logger.atSevere().log("FollowMe ChangeUpdate.apply id=%s key=%s reference=%s", change.getId(), change.getKey(), reference);

    try (
        Repository repo = gitManager.openRepository(change.getProject());
        UpdateTree update = new UpdateTree(repo, change, updateUtil);
    ) {
      if (input.returnChanges) {
        resp.changedPaths = new Vector();
      }
      update.follow(reference, resp.changedPaths);

      if (input.returnVersion || input.doUpdate) {
        resp.version = update.getVersion(cfg.getVersionPrefix(), reference);
        logger.atSevere().log("FollowMe refernce %s", resp.version);
      }
      if (update.isCurrent()) {
        resp.canUpdate = false;
      } else {
        resp.canUpdate = true;
        if (input.doUpdate) {
          resp.newPatchsetId = update.createPatchset(user, cfg.getVersionFooter(), resp.version, rsrc.getNotes()).get();
        }
      }
    }
    logger.atSevere().log("FollowMe onReviewBranch=%s, canUpdate=%s newPatchsetId=%s version=%s changedPaths=%s", resp.onReviewBranch, resp.canUpdate, resp.newPatchsetId, resp.version, resp.changedPaths);
    return Response.ok(resp);
  }

}
