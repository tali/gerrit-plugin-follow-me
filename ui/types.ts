/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

// copied from polygerrit-ui/app/types/events.ts

export enum EventType {
  BIND_VALUE_CHANGED = 'bind-value-changed',
  CHANGE = 'change',
  CHANGED = 'changed',
  CHANGE_MESSAGE_DELETED = 'change-message-deleted',
  COMMIT = 'commit',
  DIALOG_CHANGE = 'dialog-change',
  DROP = 'drop',
  EDITABLE_CONTENT_SAVE = 'editable-content-save',
  GR_RPC_LOG = 'gr-rpc-log',
  IRON_ANNOUNCE = 'iron-announce',
  KEYDOWN = 'keydown',
  KEYPRESS = 'keypress',
  LOCATION_CHANGE = 'location-change',
  MOVED_LINK_CLICKED = 'moved-link-clicked',
  NETWORK_ERROR = 'network-error',
  OPEN_FIX_PREVIEW = 'open-fix-preview',
  CLOSE_FIX_PREVIEW = 'close-fix-preview',
  PAGE_ERROR = 'page-error',
  RECREATE_CHANGE_VIEW = 'recreate-change-view',
  RECREATE_DIFF_VIEW = 'recreate-diff-view',
  RELOAD = 'reload',
  REPLY = 'reply',
  SERVER_ERROR = 'server-error',
  SHORTCUT_TRIGGERERD = 'shortcut-triggered',
  SHOW_ALERT = 'show-alert',
  SHOW_ERROR = 'show-error',
  SHOW_TAB = 'show-tab',
  SHOW_SECONDARY_TAB = 'show-secondary-tab',
  TAP_ITEM = 'tap-item',
  TITLE_CHANGE = 'title-change',
}


export interface BindValueChangeEventDetail {
  value: string | undefined;
};

export type BindValueChangeEvent = CustomEvent<BindValueChangeEventDetail>;
