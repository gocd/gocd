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

import * as m from "mithril";
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import {TestHelper} from "views/pages/spec/test_helper";
import {LogoutPageWidget} from "views/pages/logout_page/logout_page_widget";

describe("LogoutPageWidget", () => {

  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should show logout message with auto redirect message", () => {
    mount({
            hasPasswordPlugins: true,
            hasWebBasedPlugins: true,
            doNotAutoRedirect: true,
            webBasedPlugins: [{
              imageUrl: "https://example.com/image",
              pluginName: "My plugin",
              redirectUrl: "/some-redirect-url"
            }]
          });

    expect($(helper.root!)).toContainText("You have been logged out.");
    expect($(helper.root!)).toContainText("automatically redirected");
    expect(helper.find("a")).toHaveAttr("href", "/go/auth/login");
  });

  it("should show logout message with auto redirect message when no web based plugins are installed", () => {
    mount({
            hasPasswordPlugins: true,
            hasWebBasedPlugins: false,
            doNotAutoRedirect: true,
            webBasedPlugins: []
          });

    expect($(helper.root!)).toContainText("You have been logged out.");
    expect($(helper.root!)).toContainText("automatically redirected");
    expect(helper.find("a")).toHaveAttr("href", "/go/auth/login");
  });

  it("should show logout message with auto redirect message when when only web based plugins are installed", () => {
    mount({
            hasPasswordPlugins: false,
            hasWebBasedPlugins: true,
            doNotAutoRedirect: true,
            webBasedPlugins: [{
              imageUrl: "https://example.com/image1",
              pluginName: "My plugin1",
              redirectUrl: "/some-redirect-url1"
            }, {
              imageUrl: "https://example.com/image2",
              pluginName: "My plugin2",
              redirectUrl: "/some-redirect-url2"
            }]
          });

    expect($(helper.root!)).toContainText("You have been logged out.");
    expect($(helper.root!)).toContainText("automatically redirected");
    expect(helper.find("a")).toHaveAttr("href", "/go/auth/login");
  });

  it("should not show redirect message if only 1 web based plugin is installed and no password plugins", () => {
    mount({
            hasPasswordPlugins: false,
            hasWebBasedPlugins: true,
            doNotAutoRedirect: true,
            webBasedPlugins: [{
              imageUrl: "https://example.com/image1",
              pluginName: "My plugin1",
              redirectUrl: "/some-redirect-url1"
            }]
          });

    expect($(helper.root!)).toContainText("You have been logged out.");
    expect($(helper.root!)).not.toContainText("automatic");
    expect($(helper.root!)).toContainText("Click here to login again.");
    expect(helper.find("a")).toHaveAttr("href", "/go/auth/login");
  });

  function mount(authPluginInfo: AuthPluginInfo) {
    helper.mount(() => <LogoutPageWidget {...authPluginInfo} />);
  }

});
