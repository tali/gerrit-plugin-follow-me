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

import static com.google.common.truth.Truth.assertThat;

import com.google.gerrit.server.change.NotifyResolver;
import com.google.gerrit.server.change.PatchSetInserter;
import com.google.gerrit.server.update.BatchUpdate;

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

  private UpdateUtil updateUtil;

  @Before
  public void setUp() {
    updateUtil = new UpdateUtil(
        patchSetInserterFactory,
        updateFactory,
        notifyResolver);
  }

  @Test
  public void insertFooters_new1() {
    assertThat(updateUtil.insertFooters("A\n\nB\nC\n", "C", "value"))
    .isEqualTo("A\n\nB\nC\n\nC: value\n");
  }

  @Test
  public void insertFooters_new2() {
    assertThat(updateUtil.insertFooters("A\n\nB\nC\n", "C", "value1 \n value2"))
    .isEqualTo("A\n\nB\nC\n\nC: value1\nC: value2\n");
  }

  @Test
  public void insertFooters_existing1() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\n", "B", "value"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nB: value\n");
  }

  @Test
  public void insertFooters_existing2() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\n", "B", "value1 \n value2"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nB: value1\nB: value2\n");
  }

  @Test
  public void insertFooters_replace1to1() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\nD: between\nE: last\n", "D", "value"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nD: value\nE: last\n");
  }

  @Test
  public void insertFooters_replace1to2() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\nD: between\nE: last\n", "D", "value1 \n value2"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nD: value1\nD: value2\nE: last\n");
  }

  @Test
  public void insertFooters_replace2to1() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\nD: between\nE: other\nD: another\n", "D", "value"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nD: value\nE: other\n");
  }

  @Test
  public void insertFooters_replace2to2() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\nD: between\nE: other\nD: another\n", "D", "value1 \n value2"))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\nD: value1\nD: value2\nE: other\n");
  }

  @Test
  public void insertFooters_remove1() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nC: remove\nChange-Id: footer\n", "C", ""))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\n");
  }

  @Test
  public void insertFooters_remove2() {
    assertThat(updateUtil.insertFooters("A\n\nB: no-footer\n\nChange-Id: footer\nC: remove\n", "C", ""))
    .isEqualTo("A\n\nB: no-footer\n\nChange-Id: footer\n");
  }

  @Test
  public void insertFooters_removeWhitespace() {
    assertThat(updateUtil.insertFooters("A\n", "B", "\n with whitespace \n "))
    .isEqualTo("A\n\nB: with whitespace\n");
  }
}
