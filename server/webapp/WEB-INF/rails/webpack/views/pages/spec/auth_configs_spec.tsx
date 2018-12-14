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
import {TestData} from "models/auth_configs/spec/test_data";
import {PluginInfoTestData} from "models/shared/plugin_infos_new/spec/test_data";
import {AuthConfigsPage} from "views/pages/auth_configs";

function pluginInfoResponse(...object: any) {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v4+json; charset=utf-8"
    },
    responseText: JSON.stringify(PluginInfoTestData.list(object))
  };
}

function authConfigResponse() {
  return {
    status: 200,
    responseHeaders: {
      "Content-Type": "application/vnd.go.cd.v1+json; charset=utf-8"
    },
    responseText: JSON.stringify(TestData.authConfigList(TestData.ldapAuthConfig()))
  };
}

describe("AuthorizationConfigurationPage", () => {
  let $root: any, root: any, authConfigsPage: AuthConfigsPage;
  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  beforeEach(() => {
    authConfigsPage = new AuthConfigsPage();
    jasmine.Ajax.install();
    mount();
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    unmount();
  });
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should render page with page header", (done) => {
    jasmine.Ajax.stubRequest("/go/api/admin/plugin_info?type=authorization")
           .andReturn(pluginInfoResponse(PluginInfoTestData.authorization()));

    jasmine.Ajax.stubRequest("/go/api/admin/security/auth_configs").andReturn(authConfigResponse());

    const originalFunction = authConfigsPage.fetchData;
    done();
    spyOn(authConfigsPage, "fetchData").and.returnValues(() => {
      return Promise.resolve(originalFunction).finally(() => {
        expect(find("title")).toContainText("Authorization Configurations");
        expect(find("add-auth-config-button")).toContainText("Add");

        expect(find("add-auth-config-button")).not.toBeDisabled();
      });
    });
  });

  it("should disable add button if no authorization plugins installed", (done) => {
    jasmine.Ajax.stubRequest("/go/api/admin/plugin_info?type=authorization")
           .andReturn(pluginInfoResponse());

    jasmine.Ajax.stubRequest("/go/api/admin/security/auth_configs").andReturn(authConfigResponse());

    const originalFunction = authConfigsPage.fetchData;
    done();
    spyOn(authConfigsPage, "fetchData").and.returnValues(() => {
      return Promise.resolve(originalFunction).finally(() => {
        expect(find("title")).toContainText("Authorization Configurations");
        expect(find("add-auth-config-button")).toContainText("Add");

        expect(find("add-auth-config-button")).toBeDisabled();
      });
    });
  });

  function mount() {
    m.mount(root, authConfigsPage);
    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }
})
;
