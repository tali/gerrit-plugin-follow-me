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

import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static java.util.Objects.requireNonNull;

@Singleton
class Configuration {
  private final String followBranch;
  private final String reviewBranch;
  private final String reviewFilesFooter;
  private final String reviewTargetFooter;
  private final String versionPrefix;
  private final String versionDropPrefix;

  static final String DEFAULT_FOLLOW_BRANCH = "refs/heads/master";
  static final String DEFAULT_REVIEW_BRANCH = "refs/heads/review";
  static final String DEFAULT_REVIEW_FILES_FOOTER = "Review-Files";
  static final String DEFAULT_REVIEW_TARGET_FOOTER = "Review-Target";
  static final String DEFAULT_VERSION_PREFIX = "refs/tags/";
  static final String DEFAULT_VERSION_DROP_PREFIX = "refs/tags/";

  @Inject
  public Configuration(
      PluginConfigFactory pluginConfigFactory,
      @PluginName String pluginName) {
    PluginConfig cfg = requireNonNull(pluginConfigFactory.getFromGerritConfig(pluginName));

    this.followBranch = cfg.getString("followBranch", DEFAULT_FOLLOW_BRANCH);
    this.reviewBranch = cfg.getString("reviewBranch", DEFAULT_REVIEW_BRANCH);
    this.reviewFilesFooter = cfg.getString("reviewFilesFooter", DEFAULT_REVIEW_FILES_FOOTER);
    this.reviewTargetFooter = cfg.getString("reviewTargetFooter", DEFAULT_REVIEW_TARGET_FOOTER);
    this.versionPrefix = cfg.getString("versionPrefix", DEFAULT_VERSION_PREFIX);
    this.versionDropPrefix = cfg.getString("versionDropPrefix", DEFAULT_VERSION_DROP_PREFIX);
  }

  public String getFollowBranch() {
    return followBranch;
  }

  public String getReviewBranch() {
    return reviewBranch;
  }

  public String getReviewFilesFooter() {
    return reviewFilesFooter;
  }

  public String getReviewTargetFooter() {
    return reviewTargetFooter;
  }

  public String getVersionPrefix() {
    return versionPrefix;
  }

  public String getVersionDropPrefix() {
    return versionDropPrefix;
  }
}
