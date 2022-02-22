/**
 * @license
 * Copyright (C) 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import '@gerritcodereview/typescript-api/gerrit';
import {EventType} from '@gerritcodereview/typescript-api/plugin';
import {ChangeInfo, RevisionInfo} from '@gerritcodereview/typescript-api/rest-api';
import {ActionType} from '@gerritcodereview/typescript-api/change-actions';

import './file_list_column';
import './change_view_tab';
import {testChangeUpdate, doChangeUpdate} from './change_update';


window.Gerrit.install(plugin => {
  const restApi = plugin.restApi();
  var updateAction: string | null;
//  const reporting = plugin.reporting();

/*
  // Register our custom file list column
  plugin.registerDynamicCustomComponent(
      'change-view-file-list-header-prepend', 'gr-fme-file-list-header'
  );
  plugin.registerDynamicCustomComponent(
      'change-view-file-list-content-prepend', 'gr-fme-file-list-content'
  );

  plugin.registerDynamicCustomComponent(
      'change-view-tab-header', 'gr-fme-change-view-tab-header'
  );
  plugin.registerDynamicCustomComponent(
      'change-view-tab-content', 'gr-fme-change-view-tab-content'
  );
*/
  plugin.on(EventType.SHOW_CHANGE, async (change: ChangeInfo, revision: RevisionInfo, _mergeable: boolean) => {
    const followme = await testChangeUpdate(restApi, change, revision);
    if (!followme.on_review_branch) {
      console.debug("followme not onReviewBranch");
      return;
    }
    var actions = plugin.changeActions();
    if (updateAction != null) {
      actions.remove(updateAction);
    }
    updateAction = actions.add(ActionType.REVISION, 'update-review');
    if (followme.can_update) {
      actions.setEnabled(updateAction, true);
      actions.setLabel(updateAction, "Follow");
      actions.setTitle(updateAction, `Follow ${followme.version} and include updates for ${followme.changed_paths.length} files`);
      actions.setIcon(updateAction, "arrow-forward");
      actions.addTapListener(updateAction, async () => {
        if (updateAction != null) {
          actions.setEnabled(updateAction, true);
          actions.setLabel(updateAction, "Updating...");
        }
        await doChangeUpdate(restApi, change, revision);
        window.location.reload(); // TBD
      });
    } else {
      actions.setEnabled(updateAction, false);
      actions.setLabel(updateAction, "Follow");
      actions.setTitle(updateAction, `Already up-to-date with ${followme.version}`);
      actions.setIcon(updateAction, "arrow-forward");
    }
  });

  console.debug("follow-branch loaded");
});
