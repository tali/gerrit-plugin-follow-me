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
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestModifyView;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.update.UpdateException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.errors.ConfigInvalidException;

import com.googlesource.gerrit.plugins.reviewtarget.PostFollow.Input;

import static java.util.Objects.requireNonNull;

@Singleton
class PostFollow implements RestModifyView<ChangeResource, Input> {
  static class Input {
    boolean doUpdate;
    String newReviewTarget;
    String newReviewFiles;
  }

  static class FollowInfo {
    boolean onReviewBranch;
    boolean validReviewTarget;
    int newPatchsetId;
    String version;
    List<String> addedPaths;
    List<String> updatedPaths;
    List<String> removedPaths;
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager gitManager;
  private final Configuration cfg;
  private final FollowPreconditions preconditions;
  private final UpdateUtil updateUtil;

  @Inject
  PostFollow(
      GitRepositoryManager gitManager,
      Configuration cfg,
      FollowPreconditions preconditions,
      UpdateUtil updateUtil) {
    this.gitManager = requireNonNull(gitManager);
    this.cfg = requireNonNull(cfg);
    this.preconditions = requireNonNull(preconditions);
    this.updateUtil = requireNonNull(updateUtil);
  }

  @Override
  public Response<FollowInfo> apply(ChangeResource rsrc, Input input) throws IOException, RestApiException, ConfigInvalidException, UpdateException {
    preconditions.assertAddPatchSetPermission(rsrc);

    Change change = rsrc.getChange();
    logger.atFine().log("FollowMe POST id=%s doUpdate=%s newReviewTarget=%s", change.getId(), input.doUpdate, input.newReviewTarget);

    FollowInfo resp = new FollowInfo();
    if (!change.isNew()) {
      // ignore MERGED or ABANDONED changes
      return Response.ok(resp);
    }
    resp.onReviewBranch = (change.getDest().branch().equals(cfg.getReviewBranch()));
    if (!resp.onReviewBranch) {
      return Response.ok(resp);
    }
    CurrentUser user = rsrc.getUser();

    try (
        Repository repo = gitManager.openRepository(change.getProject());
        UpdateTree update = new UpdateTree(repo, change, updateUtil);
    ) {
      if (input.newReviewTarget != null) {
        update.newReviewTarget(input.newReviewTarget);
      } else {
        update.useReviewTargetFooter(cfg.getReviewTargetFooter());
      }

      if (input.newReviewFiles != null) {
        update.newReviewFiles(input.newReviewFiles);
      } else {
        update.useReviewFilesFooter(cfg.getReviewFilesFooter());
      }

      resp.validReviewTarget = update.isValidReviewTarget();
      if (!resp.validReviewTarget) {
        return Response.ok(resp);
      }

      update.rewritePaths();

      resp.addedPaths = new ArrayList<>();
      resp.updatedPaths = new ArrayList<>();
      resp.removedPaths = new ArrayList<>();
      update.getChangedPaths(resp.addedPaths, resp.updatedPaths, resp.removedPaths);

      resp.version = update.getTargetVersion(cfg.getVersionPrefix(), cfg.getVersionDropPrefix());

      if (input.doUpdate) {
        resp.newPatchsetId = update.createPatchSet(user, cfg.getReviewTargetFooter(), cfg.getReviewFilesFooter(), rsrc.getNotes());
      }
    }
    return Response.ok(resp);
  }

}
