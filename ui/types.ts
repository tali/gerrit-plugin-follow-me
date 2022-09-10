/**
 * @license
 * Copyright 2020 Google LLC
 * SPDX-License-Identifier: Apache-2.0
 */

// copied from polygerrit-ui/app/types/events.ts

export interface BindValueChangeEventDetail {
  value: string | undefined;
};

export type BindValueChangeEvent = CustomEvent<BindValueChangeEventDetail>;
