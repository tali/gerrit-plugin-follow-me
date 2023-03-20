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
import com.google.gerrit.extensions.restapi.ResourceConflictException;
import com.google.gerrit.extensions.restapi.Response;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.change.RebaseUtil;
import com.google.gerrit.server.git.GitRepositoryManager;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.update.UpdateException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.IOException;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.errors.ConfigInvalidException;

import static java.util.Objects.requireNonNull;

@Singleton
class GetFollow implements RestReadView<ChangeResource> {

  static class FollowInfo {
    boolean onReviewBranch;
    boolean validReviewTarget;
    boolean rebaseRequired;
    String version;
    String followVersion;
    String followBranch;
    String reviewTarget;
    String reviewFiles;
  }

  private static final FluentLogger logger = FluentLogger.forEnclosingClass();

  private final GitRepositoryManager gitManager;
  private final Configuration cfg;
  private final FollowPreconditions preconditions;
  private final UpdateUtil updateUtil;
  private final RebaseUtil rebaseUtil;

  @Inject
  GetFollow(
      GitRepositoryManager gitManager,
      Configuration cfg,
      FollowPreconditions preconditions,
      UpdateUtil updateUtil,
      RebaseUtil rebaseUtil) {
    this.gitManager = requireNonNull(gitManager);
    this.cfg = requireNonNull(cfg);
    this.preconditions = requireNonNull(preconditions);
    this.updateUtil = requireNonNull(updateUtil);
    this.rebaseUtil = requireNonNull(rebaseUtil);
  }

  @Override
  public Response<FollowInfo> apply(ChangeResource rsrc) throws IOException, RestApiException, ConfigInvalidException, UpdateException {
    preconditions.assertAddPatchSetPermission(rsrc);
    preconditions.assertCanChangeReviewTarget(rsrc);

    Change change = rsrc.getChange();
    FollowInfo resp = new FollowInfo();
    resp.onReviewBranch = true;

    logger.atFine().log("FollowMe GET id=%s key=%s", change.getId(), change.getKey());

    try (
        Repository repo = gitManager.openRepository(change.getProject());
        UpdateTree update = new UpdateTree(repo, updateUtil, rebaseUtil);
    ) {
      update.useChange(change);
      update.useReviewTargetFooter(cfg.getReviewTargetFooter());
      resp.validReviewTarget = update.isValidReviewTarget();
      update.useReviewFilesFooter(cfg.getReviewFilesFooter());

      resp.followBranch = cfg.getFollowBranch();
      update.useFollowBranch(cfg.getFollowBranch());
      resp.followVersion = update.getFollowVersion(cfg.getVersionPrefix(), cfg.getVersionDropPrefix());

      resp.validReviewTarget = update.isValidReviewTarget();
      if (!resp.validReviewTarget) {
        return Response.ok(resp);
      }

      resp.reviewTarget = update.getReviewTarget();
      resp.reviewFiles = update.getReviewFiles();
      resp.version = update.getTargetVersion(cfg.getVersionPrefix(), cfg.getVersionDropPrefix());
      resp.rebaseRequired = update.rebaseWhenNecessary(rsrc.getNotes().getCurrentPatchSet());
    }
    return Response.ok(resp);
  }

}
