// Copyright (C) 2023 Siemens Mobility GmbH
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

import org.eclipse.jgit.ignore.FastIgnoreRule;

import java.util.ArrayList;
import java.util.List;

public class ReviewFilter {
  enum Selected { NO_MATCH, POSITIVE, NEGATIVE }

  private final List<FastIgnoreRule> rules;

  ReviewFilter(List<String> lines) {
    this.rules = getRules(lines);
  }

  ReviewFilter(String lines) {
    this.rules = getRules(List.of(lines.split("\n")));
  }

  private static List<FastIgnoreRule> getRules(List<String> lines) {
    List<FastIgnoreRule> rules = new ArrayList<>();
    for (String line : lines) {
      line = line.strip();
      if (line.isEmpty()) continue;
      // Add rules in the reverse order because later rules have higher priority.
      // This way we can stop at the first matching rule later.
      rules.add(0, new FastIgnoreRule(line));
    }
    return rules;
  }

  public boolean matchAll() {
    return rules.size() == 0;
  }

  /**
   * check if this path matches our given filter
   */
  Selected isPathToBeReviewed(String path, boolean isDirectory) {
    for (FastIgnoreRule rule : rules) {
      if (rule.isMatch(path, isDirectory, true)) {
        return rule.getResult() ? Selected.POSITIVE : Selected.NEGATIVE;
      }
    }
    // no rule matches
    return Selected.NO_MATCH;
  }
}
