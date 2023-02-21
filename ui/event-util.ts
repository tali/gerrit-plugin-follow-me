/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

// copied from app/utils/event-util.ts

export function fire<T>(target: EventTarget, type: string, detail: T) {
  target.dispatchEvent(
    new CustomEvent<T>(type, {
      detail,
      composed: true,
      bubbles: true,
    })
  );
}

export function fireReload(target: EventTarget, clearPatchset?: boolean) {
  fire(target, 'reload', {clearPatchset: !!clearPatchset});
}

