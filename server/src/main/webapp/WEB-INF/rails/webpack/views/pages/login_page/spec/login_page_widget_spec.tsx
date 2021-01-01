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

import m from "mithril";
import {AuthPluginInfo} from "models/authentication/auth_plugin_info";
import {LoginPageWidget} from "views/pages/login_page/login_page_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import styles from "../login_page_widget.scss";

describe("LoginPageWidget", () => {
  const helper = new TestHelper();

  afterEach(helper.unmount.bind(helper));

  it("should show login form with web login buttons when both are present", () => {
    mount({
            hasPasswordPlugins: true,
            hasWebBasedPlugins: true,
            webBasedPlugins: [{
              imageUrl: "https://example.com/image",
              pluginName: "My plugin",
              redirectUrl: "/some-redirect-url"
            }]
          });

    expect(helper.byTestId("form-field-input-username")).toBeInDOM();
    expect(helper.byTestId("form-field-input-password")).toBeInDOM();

    expect(helper.root!.textContent).toContain("Or login using:");
    expect(helper.byTestId("link-to-login-using-my-plugin")).toBeInDOM();
    expect(helper.byTestId("link-to-login-using-my-plugin")).toHaveAttr("href", "/some-redirect-url");
    expect(helper.byTestId("image-for-my-plugin")).toBeInDOM();
    expect(helper.byTestId("image-for-my-plugin")).toHaveAttr("src", "https://example.com/image");
  });

  it("should show login form only when no web based plugins are installed", () => {
    mount({
            hasPasswordPlugins: true,
            hasWebBasedPlugins: false,
            webBasedPlugins: []
          });

    expect(helper.byTestId("form-field-input-username")).toBeInDOM();
    expect(helper.byTestId("form-field-input-password")).toBeInDOM();

    expect(helper.root!.textContent).not.toContain("redirect");

    expect(helper.q("a")).toBeFalsy();
    expect(helper.q(`.${styles.loginOptions} img`)).toBeFalsy();
  });

  it("should show web login buttons and no login form when only web based plugins are installed", () => {
    mount({
            hasPasswordPlugins: false,
            hasWebBasedPlugins: true,
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

    expect(helper.q("input")).toBeFalsy();

    expect(helper.root!.textContent).not.toContain("redirect");

    expect(helper.q("a")).toBeInDOM();
    expect(helper.q(`.${styles.loginOptions} img`)).toBeInDOM();
  });

  it("should show redirect message if only 1 web based plugin is installed and no password plugins", () => {
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

    expect(helper.q("input")).toBeFalsy();

    expect(helper.root!.textContent).toContain("redirect");

    expect(helper.q("a")).toBeInDOM();
    expect(helper.q(`.${styles.redirect} img`)).toBeInDOM();
  });

  it("should show login error", () => {
    mount({
            loginError: "Login error: wrong username or password",
            hasPasswordPlugins: false,
            hasWebBasedPlugins: true,
            doNotAutoRedirect: true,
            webBasedPlugins: [{
              imageUrl: "https://example.com/image1",
              pluginName: "My plugin1",
              redirectUrl: "/some-redirect-url1"
            }]
          });

    expect(helper.textByTestId("flash-message-alert")).toContain("Login error: wrong username or password.");

  });

  function mount(authPluginInfo: AuthPluginInfo) {
    helper.mount(() => <LoginPageWidget {...authPluginInfo}/>);
  }

});
