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

import {ChangeInfo, RevisionInfo} from '@gerritcodereview/typescript-api/rest-api';
import {RestPluginApi} from '@gerritcodereview/typescript-api/rest';

export declare interface FollowInfo {
  on_review_branch: boolean;
  can_update: boolean;
  new_patchset_id: number;
  version: string;
  changed_paths: string[];
}

export async function testChangeUpdate(restApi: RestPluginApi, change: ChangeInfo, revision: RevisionInfo): Promise<FollowInfo> {
  console.warn("testChangeUpdate change", change, revision);
  const endpoint = `/changes/${change.id}/follow`;
  const content = {
    do_update: false,
    return_version: true,
    return_changes: true,
  };
  const resp = await restApi.post<FollowInfo>(endpoint, content)
  console.warn("success POST", endpoint, content, resp);
  return resp;
}

export async function doChangeUpdate(restApi: RestPluginApi, change: ChangeInfo, revision: RevisionInfo): Promise<FollowInfo> {
  console.warn("doChangeUpdate change", change, revision);
  const endpoint = `/changes/${change.id}/follow`;
  const content = {
    do_update: true,
    return_version: true,
    return_changes: true,
  };
  const resp = await restApi.post<FollowInfo>(endpoint, content)
  console.warn("success POST", endpoint, content, resp);
  return resp;
}
