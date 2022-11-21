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

import com.google.gerrit.entities.Change;
import com.google.gerrit.extensions.restapi.AuthException;
import com.google.gerrit.extensions.restapi.PreconditionFailedException;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.change.ChangeResource;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.permissions.ChangePermission;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;

import static java.util.Objects.requireNonNull;

@Singleton
class FollowPreconditions {
  private final Configuration cfg;
  private final Provider<CurrentUser> userProvider;
  private final PermissionBackend permissionBackend;

  @Inject
  public FollowPreconditions(
      Configuration cfg,
      Provider<CurrentUser> userProvider,
      PermissionBackend permissionBackend) {
    this.cfg = requireNonNull(cfg);
    this.userProvider = requireNonNull(userProvider);
    this.permissionBackend = requireNonNull(permissionBackend);
  }

  void assertCanChangeReviewTarget(ChangeResource rsrc) throws PreconditionFailedException {
    Change change = rsrc.getChange();

    if (change.isMerged()) {
      throw new PreconditionFailedException("change is MERGED");
    }

    if (change.isAbandoned()) {
      throw new PreconditionFailedException("change is ABANDONED");
    }

    if (!onReviewBranch(change)) {
      throw new PreconditionFailedException("not on review branch");
    }
  }

  protected boolean onReviewBranch(Change change) {
    return change.getDest().branch().equals(cfg.getReviewBranch());
  }

  void assertAddPatchSetPermission(ChangeResource rsrc) throws AuthException {
    if (!canAddPatchSet(rsrc)) {
      throw new AuthException("not allowed to create new patch sets");
    }
  }

  protected boolean canAddPatchSet(ChangeResource rsrc) {
    PermissionBackend.WithUser userPermission = permissionBackend.user(userProvider.get());
    return userPermission.change(rsrc.getChangeData()).testOrFalse(ChangePermission.ADD_PATCH_SET);
  }
}