// Copyright (C) 2018 The Android Open Source Project
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

package com.googlesource.gerrit.plugins.followme;

import static com.google.common.truth.Truth.assertThat;
import static com.google.gerrit.testing.GerritJUnit.assertThrows;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.gerrit.server.change.ChangeKindCache;
import com.google.gerrit.server.change.NotifyResolver;
import com.google.gerrit.server.change.PatchSetInserter;
import com.google.gerrit.server.update.BatchUpdate;

import com.google.inject.Provider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class UpdateUtilTest {

  @Mock private PatchSetInserter.Factory patchSetInserterFactory;
  @Mock private BatchUpdate.Factory updateFactory;
  @Mock private NotifyResolver notifyResolver;
  @Mock private ChangeKindCache changeKindCache;

  private UpdateUtil updateUtil;

  @Before
  public void setUp() {
    updateUtil = new UpdateUtil(
        patchSetInserterFactory,
        updateFactory,
        notifyResolver,
        changeKindCache);
  }

  @Test
  public void insertNewFooter() {
    assertThat(updateUtil.insertFooter("A\n\nB\nC\n", "C", "value"))
    .isEqualTo("A\n\nB\nC\n\nC: value\n");
  }

  @Test
  public void appendToFooter() {
    assertThat(updateUtil.insertFooter("A\n\nB: no-footer\n\nChange-Id: footer\n", "B", "value"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nB: value\n");
  }

  @Test
  public void replaceFooter() {
    assertThat(updateUtil.insertFooter("A\n\nB: no-footer\n\nChange-Id: footer\nD: inbetween\nE: last\n", "D", "value"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nD: value\nE: last\n");
  }
}
