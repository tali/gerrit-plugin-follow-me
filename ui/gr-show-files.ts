/**
 * @license
 * Copyright (C) 2023 Siemens Mobility GmbH
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

import {css, html, LitElement} from 'lit';
import {customElement, property, state} from 'lit/decorators';

declare global {
  interface HTMLElementTagNameMap {
    'gr-show-files': GrShowFiles,
  }
}

@customElement('gr-show-files')
export class GrShowFiles extends LitElement {

  @property({type: Array})
  files: string[] = [];

  @state() expanded = false;

  static override styles = [
    css`
      .summary { color: var(--deemphasized-text-color); }
      .fileName { color: var(--text-color); }
    `
  ];

  summary() {
    let count = this.files.length;
    if (count == 1) {
      return html`1 file`;
    } else {
      return html`${count} files`;
    }
  }

  override render() {
    if (this.expanded) {
      return html`
        <div @click=${this._clickHandler}>
          <span class=summary>${this.summary()}</span>
          <gr-icon icon="expand_less"></gr-icon>
          <br/>
          ${this.files.map(file => html`
            <span class="fileName">${file}</span>
            <br/>
          `)}
        </div>
      `;
    } else {
      return html`
        <div @click=${this._clickHandler}>
          ${this.summary()}
          <gr-icon icon="expand_more"></gr-icon>
        </div>
      `;
    }
  }

  _clickHandler() {
    this.expanded = !this.expanded;
  }
}
