/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import m from "mithril";
import Stream from "mithril/stream";
import {Dropdown, DropdownAttrs, Primary} from "../index";
import Style from "./index.scss";

interface DummyDropdownButtonAttrs {
  name: string;
}

export class DummyDropdownButton extends Dropdown<DummyDropdownButtonAttrs> {
  private readonly showContent: Stream<boolean> = Stream();

  toggleDropdown(vnode: m.Vnode<DropdownAttrs & DummyDropdownButtonAttrs>, e: MouseEvent) {
    this.showContent(!this.showContent());
    vnode.attrs.show(this.showContent());
  }

  protected doRenderButton(vnode: m.Vnode<DropdownAttrs & DummyDropdownButtonAttrs>) {
    return <Primary dropdown={true} onclick={(e: MouseEvent) => this.toggleDropdown(vnode, e)}>{vnode.attrs.name}</Primary>;
  }

  protected doRenderDropdownContent(vnode: m.Vnode<DropdownAttrs & DummyDropdownButtonAttrs>) {
    if (!this.showContent()) {
      return;
    }

    return (<div class={Style.dropdownButtonContent}>
      {vnode.children}
    </div>);
  }
}
