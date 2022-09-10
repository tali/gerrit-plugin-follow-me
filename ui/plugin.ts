/**
 * @license
 * Copyright (C) 2022 Siemens Mobility GmbH
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

import {changeFollowGet} from './api';
import {ChangeUpdateDialog} from './followme_dialog';


window.Gerrit.install(plugin => {
  const restApi = plugin.restApi();
  var followAction: string | null;
//  const reporting = plugin.reporting();

  plugin.on(EventType.SHOW_CHANGE, async (change: ChangeInfo, _revision: RevisionInfo, _mergeable: boolean) => {
    const followme = await changeFollowGet(restApi, change);
    if (!followme.on_review_branch) {
      console.debug("followme not onReviewBranch");
      return;
    }
    var actions = plugin.changeActions();
    if (followAction != null) {
      actions.remove(followAction);
    }
    if (followme.on_review_branch) {
      followAction = actions.add(ActionType.REVISION, 'update-review');
      actions.setEnabled(followAction, true);
      actions.setLabel(followAction, "Select");
      actions.setTitle(followAction, `Change Review-Target or Review-Files`);
      actions.setIcon(followAction, "rule");
      actions.addTapListener(followAction, async () => {
        const popupApi = await plugin.popup();
        const openDialog = await popupApi.open();
        var dialog = new ChangeUpdateDialog();
        dialog.initialize(followme);
        dialog.plugin = plugin;
        dialog.change = change;
        dialog.popupApi = popupApi;
        openDialog.appendContent(dialog);
      });
    }
  });

  console.debug("follow-me plugin loaded");
});
