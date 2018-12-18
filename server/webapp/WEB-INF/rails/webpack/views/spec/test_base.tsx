/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as m from "mithril";

export class TestBase {
  private $root: any;
  private root: any;

  constructor() {
    // @ts-ignore
    [this.$root, this.root] = window.createDomElementForTest();
  }

  mount = (component: m.Children) => {
    m.mount(this.root, {
      view() {
        return component;
      }
    });
    m.redraw();
  };

  unmount = () => {
    m.mount(this.root, null);
    // @ts-ignore
    window.destroyDomElementForTest();
    m.redraw();
  };

  findByDataTestId(id: string) {
    return this.$root.find(`[data-test-id='${id}']`);
  }

  getJQueryRoot() {
    return this.$root;
  }
}