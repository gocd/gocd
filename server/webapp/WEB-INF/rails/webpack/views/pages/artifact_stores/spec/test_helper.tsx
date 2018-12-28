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

export class TestHelper {
  private root: any;
  private $root: any;
  private component: any;

  mount(component: any) {
    this.component = component;
    // @ts-ignore
    const all      = window.createDomElementForTest();
    this.$root     = all[0];
    this.root      = all[1];
    m.mount(this.root, {
      view() {
        return <div>{component}</div>;
      }
    });
    m.redraw();
  }

  unmount(done?: any) {
    m.mount(this.root, null);
    m.redraw();
    //@ts-ignore
    window.destroyDomElementForTest();
    this.root  = null;
    this.$root = null;
    if (done && typeof done === "function") {
      done();
    }
  }

  findByDataTestId(id: string) {
    return this.$root.find(`[data-test-id='${id}']`);
  }

  find(selector: string) {
    return this.$root.find(selector);
  }

  findIn(elem: any, id: string) {
    return $(elem).find(`[data-test-id='${id}']`);
  }

  remount() {
    this.unmount();
    this.mount(this.component);
  }
}
