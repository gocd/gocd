/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {AccessTokens} from "models/access_tokens/types";
import {AccessTokensPage} from "views/pages/access_tokens";
import {PageState} from "views/pages/page";
import {TestHelper} from "views/pages/spec/test_helper";

describe("AccessTokenPage", () => {
  const helper = new TestHelper();
  beforeEach(() => {
    jasmine.Ajax.install();
  });

  afterEach(() => {
    helper.unmount();
    jasmine.Ajax.uninstall();
  });

  it("should disable generate token button when in meta supportsAccessToken set to false", () => {
    document.body.setAttribute("data-meta", JSON.stringify({pluginId: "cd.go.ldap", supportsAccessToken: false}));
    mount();

    expect(helper.byTestId("generate-token-button")).toBeDisabled();
    expect(helper.byTestId("flash-message-info")).toBeInDOM();
    expect(helper.textByTestId("flash-message-info")).toBe("Creation of access token is not supported by the plugin cd.go.ldap.");
  });

  it("should not disable generate token button when in meta supportsAccessToken set to true", () => {
    document.body.setAttribute("data-meta", JSON.stringify({pluginId: "cd.go.ldap", supportsAccessToken: true}));
    mount();

    expect(helper.byTestId("flash-message-info")).toBeFalsy();
    expect(helper.byTestId("generate-token-button")).not.toBeDisabled();
  });

  function mount() {
    helper.mountPage(() => new StubbedPage());
  }

});

class StubbedPage extends AccessTokensPage {
  fetchData(vnode: m.Vnode<any, any>): Promise<any> {
    this.pageState = PageState.OK;
    vnode.state.accessTokens(new AccessTokens());
    return Promise.resolve();
  }
}
