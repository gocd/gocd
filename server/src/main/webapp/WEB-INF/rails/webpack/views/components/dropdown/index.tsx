/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import {MithrilComponent} from "jsx/mithril-component";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import styles from "./index.scss";

function hideOnBlur(elem: any, state: any, event: any) {
  if (!elem.contains(event.target)) {
    state.dropdownOpen(false);
  }
}

function isOpen(state: any) {
  return state.dropdownOpen() ? styles.open : null;
}

export interface State {
  dropdownOpen: Stream<boolean>;
}

export interface Attrs {
  property: Stream<string>;
  possibleValues: DropdownElement[];
  label: string;
}

export interface DropdownElement {
  id: string;
  value: string;
}

export class Dropdown extends MithrilComponent<Attrs, State> {
  oncreate(vnode: m.Vnode<Attrs, State>) {
    //@ts-ignore
    window.addEventListener("click", hideOnBlur.bind(this, vnode.dom, vnode.state));
  }

  onremove(vnode: m.Vnode<Attrs, State>) {
    //@ts-ignore
    window.removeEventListener("click", hideOnBlur.bind(this, vnode.dom, vnode.state));
  }

  oninit(vnode: m.Vnode<Attrs, State>) {
    vnode.state.dropdownOpen = Stream();
  }

  view(vnode: m.Vnode<Attrs, State>) {
    const label   = vnode.attrs.label;
    const options = vnode.attrs.possibleValues;

    const dropdownHtml = _.map(options, (option) => {
      return <a tabindex={0} class={styles.cDropdownItem} onclick={(event: MouseEvent) => {
        vnode.attrs.property((event.target! as any).getAttribute("data-id"));
        vnode.state.dropdownOpen(!vnode.state.dropdownOpen());
        event.preventDefault();
      }} aria-label={`Group by ${option.value}`} data-id={option.id}>{option.value}</a>;
    });

    const selectedText = options.find((item) => {
      return item.id === vnode.attrs.property();
    })!.value;

    return <div class={styles.groupby}>
      <label class={styles.groupingLabel}>{label}</label>
      <div class={styles.cDropdown + ` ${isOpen(vnode.state)}`}>
        <a aria-label={`Group by ${selectedText}`} tabindex={0} class={styles.cDropdownHead}
           onclick={() => vnode.state.dropdownOpen(!vnode.state.dropdownOpen())}>{selectedText}</a>
        <i role="presentation" class={styles.cDownArrow}
           onclick={() => vnode.state.dropdownOpen(!vnode.state.dropdownOpen())}/>
        <div class={styles.cDropdownBody}>
          {dropdownHtml}
        </div>
      </div>
    </div>;
  }
}
