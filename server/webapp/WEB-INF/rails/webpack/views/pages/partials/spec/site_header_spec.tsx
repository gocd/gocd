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
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";
import {Attrs, SiteHeader} from "views/pages/partials/site_header";
import * as styles from "../site_header.scss";

describe("Site Header", () => {

  const helper = new TestHelper();

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest("/go/api/data_sharing/settings/notification_auth", undefined, "GET").andReturn({});
    jasmine.Ajax.stubRequest("https://datasharing.gocd.org/v1", undefined, "POST").andReturn({});
    jasmine.Ajax.stubRequest("/go/api/data_sharing/settings").andReturn({});
    jasmine.Ajax.stubRequest("/go/api/server_health_messages", undefined, "GET").andReturn({});

  });

  afterEach(unmount);

  it("should display the user menu when a user is logged in", () => {
    mount({
      isAnonymous: false,
      userDisplayName: "Jon Doe",
      canViewTemplates: false,
      isGroupAdmin: false,
      isUserAdmin: false,
      canViewAdminPage: false,
      showAnalyticsDashboard: false
    });
    expect(helper.find(`.${styles.userLink}`)).toHaveText("Jon Doe");
    expect(findMenuItem("/go/preferences/notifications")).toHaveText("Preferences");
    expect(findMenuItem("/go/access_tokens")).toHaveText("Personal Access Tokens");
    expect(findMenuItem("/go/auth/logout")).toHaveText("Sign out");
    expect(findMenuItem("https://gocd.org/help")).toHaveText("Need Help?");
  });

  it("should not display the user menu when logged in as anonymous", () => {
    mount({
      isAnonymous: true,
      userDisplayName: "",
      canViewTemplates: false,
      isGroupAdmin: false,
      isUserAdmin: false,
      canViewAdminPage: false,
      showAnalyticsDashboard: false
    });
    expect(helper.find(`.${styles.userLink}`)).not.toBeInDOM();
    expect(findMenuItem("/go/preferences/notifications")).not.toBeInDOM();
    expect(findMenuItem("/go/access_tokens")).not.toBeInDOM();
    expect(findMenuItem("/go/auth/logout")).not.toBeInDOM();
    expect(findMenuItem("https://gocd.org/help")).toHaveText("Need Help?");
  });

  function mount(attrs: Attrs) {
    helper.mount(() => <SiteHeader {...attrs}/>);
  }

  function unmount() {
    helper.unmount();
    jasmine.Ajax.uninstall();
  }

  function findMenuItem(href: string) {
    return helper.find(`a[href='${href}']`);
  }
});
