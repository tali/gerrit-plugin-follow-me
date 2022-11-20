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
import com.google.gerrit.extensions.annotations.PluginData;
import com.google.gerrit.extensions.annotations.PluginName;
import com.google.gerrit.server.config.PluginConfig;
import com.google.gerrit.server.config.PluginConfigFactory;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.io.File;

import static java.util.Objects.requireNonNull;

@Singleton
class Configuration {
  private static final FluentLogger log = FluentLogger.forEnclosingClass();

  private final String followBranch;
  private final String reviewBranch;
  private final String reviewFilesFooter;
  private final String reviewTargetFooter;
  private final String versionPrefix;
  private final String versionDropPrefix;

  private final PluginConfig cfg;
  private File pluginData;

  @Inject
  public Configuration(
      PluginConfigFactory pluginConfigFactory,
      @PluginName String pluginName,
      @PluginData File pluginData) {
    this.cfg = requireNonNull(pluginConfigFactory.getFromGerritConfig(pluginName));
    this.pluginData = requireNonNull(pluginData);

    this.followBranch = cfg.getString("followBranch", "refs/heads/master");
    this.reviewBranch = cfg.getString("reviewBranch", "refs/heads/review");
    this.reviewFilesFooter = cfg.getString("reviewFilesFooter", "Review-Files");
    this.reviewTargetFooter = cfg.getString("reviewTargetFooter", "Review-Target");
    this.versionPrefix = cfg.getString("versionPrefix", "refs/tags/");
    this.versionDropPrefix = cfg.getString("versionDropPrefix", "refs/tags/");
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
