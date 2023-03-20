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
import {SelectReviewTargetDialog} from './dialog';


window.Gerrit.install(plugin => {
  const restApi = plugin.restApi();
  var selectAction: string | null;
//  const reporting = plugin.reporting();

  plugin.on(EventType.SHOW_CHANGE, async (change: ChangeInfo, _revision: RevisionInfo, _mergeable: boolean) => {
    if (change.id === undefined) return;
    const info = await changeFollowGet(restApi, change);
    if (!info.on_review_branch) {
      // this change is not managed by our plugin
      return;
    }
    var actions = plugin.changeActions();
    if (selectAction != null) {
      actions.remove(selectAction);
    }
    selectAction = actions.add(ActionType.REVISION, 'select-reviewtarget');
    actions.setEnabled(selectAction, true);
    actions.setLabel(selectAction, "Select");
    actions.setTitle(selectAction, `Change Review-Target or Review-Files`);
    actions.setIcon(selectAction, "rule");
    actions.addTapListener(selectAction, async () => {
      const popupApi = await plugin.popup();
      const openDialog = await popupApi.open();
      var dialog = new SelectReviewTargetDialog();
      dialog.initialize(info);
      dialog.plugin = plugin;
      dialog.change = change;
      dialog.popupApi = popupApi;
      openDialog.appendContent(dialog);
    });
    // hide actions which would mess with our managed changes
    actions.setActionHidden(ActionType.CHANGE, 'edit', true);
    actions.setActionHidden(ActionType.CHANGE, 'rebase', true);
    actions.setActionHidden(ActionType.REVISION, 'cherrypick', true);
  });
});
