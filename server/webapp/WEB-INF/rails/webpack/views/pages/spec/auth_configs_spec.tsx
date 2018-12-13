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
import {AuthConfigsPage} from "views/pages/auth_configs";

describe("AuthorizationConfigurationPage", () => {
  let $root: any, root: any;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });
  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render page with page header", () => {
    expect(find("title")).toContainText("Authorization Configurations");
    expect(find("add-auth-config-button")).toContainText("Add");
  });

  it("should disable add button if no authorization plugins installed", () => {
    expect(find("add-auth-config-button")).toBeDisabled();
  });

  function mount() {
    m.mount(root, new AuthConfigsPage());
    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
});
