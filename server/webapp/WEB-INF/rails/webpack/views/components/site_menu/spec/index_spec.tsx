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
import SiteMenu, {Attrs} from "views/components/site_menu/index";
import * as styles from "views/components/site_menu/index.scss";

describe("Site Menu", () => {

  let $root: any, root: any;
  beforeEach(() => {
    //@ts-ignore
    [$root, root] = window.createDomElementForTest();
  });

  afterEach(unmount);
  //@ts-ignore
  afterEach(window.destroyDomElementForTest);

  it("should display the menus for an admin", () => {
    mount({
      canViewTemplates: true,
      isGroupAdmin: true,
      isUserAdmin: true,
      canViewAdminPage: true,
      showAnalytics: true
    } as Attrs);
    const dashboard = $root.find("a").get(0);
    const agents    = $root.find("a").get(1);
    const analytics = $root.find("a").get(2);
    const admin     = $root.find("a").get(3);
    expect(dashboard).toHaveText("Dashboard");
    expect(dashboard).toHaveAttr("href", "/go/pipelines");
    expect(agents).toHaveText("Agents");
    expect(agents).toHaveAttr("href", "/go/agents");
    expect(analytics).toHaveText("Analytics");
    expect(analytics).toHaveAttr("href", "/go/analytics");
    expect(admin).toHaveText("Admin");
    expect(findMenuItem("/go/admin/pipelines")).toHaveText("Pipelines");
    expect(findMenuItem("/go/admin/config_repos")).toHaveText("Config Repositories");
    expect(findMenuItem("/go/admin/environments")).toHaveText("Environments");
    expect(findMenuItem("/go/admin/templates")).toHaveText("Templates");
    expect(findMenuItem("/go/admin/elastic_profiles")).toHaveText("Elastic Agent Profiles");
    expect(findMenuItem("/go/admin/config_xml")).toHaveText("Config XML");
    expect(findMenuItem("/go/admin/artifact_stores")).toHaveText("Artifact Stores");
    expect(findMenuItem("/go/admin/data_sharing/settings")).toHaveText("Data Sharing");
    expect(findMenuItem("/go/admin/maintenance_mode")).toHaveText("Server Maintenance Mode");
    expect(findMenuItem("/go/admin/config/server")).toHaveText("Server Configuration");
    expect(findMenuItem("/go/admin/users")).toHaveText("Users Management");
    expect(findMenuItem("/go/admin/backup")).toHaveText("Backup");
    expect(findMenuItem("/go/admin/plugins")).toHaveText("Plugins");
    expect(findMenuItem("/go/admin/package_repositories/new")).toHaveText("Package Repositories");
    expect(findMenuItem("/go/admin/security/auth_configs")).toHaveText("Authorization Configuration");
    expect(findMenuItem("/go/admin/security/roles")).toHaveText("Role configuration");
    expect($root.find(`a.${styles.siteNavLink}`)).toHaveLength(4);
    expect($root.find(`a.${styles.siteSubNavLink}`)).toHaveLength(16);
  });

  it("should display the menus for users who can view templates", () => {
    mount({
      canViewTemplates: true,
      isGroupAdmin: false,
      isUserAdmin: false,
      canViewAdminPage: true,
      showAnalytics: true
    } as Attrs);
    const dashboard = $root.find("a").get(0);
    const agents    = $root.find("a").get(1);
    const analytics = $root.find("a").get(2);
    const admin     = $root.find("a").get(3);
    expect(dashboard).toHaveText("Dashboard");
    expect(dashboard).toHaveAttr("href", "/go/pipelines");
    expect(agents).toHaveText("Agents");
    expect(agents).toHaveAttr("href", "/go/agents");
    expect(analytics).toHaveText("Analytics");
    expect(analytics).toHaveAttr("href", "/go/analytics");
    expect(admin).toHaveText("Admin");
    expect(findMenuItem("/go/admin/pipelines")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/config_repos")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/templates")).toHaveText("Templates");
    expect(findMenuItem("/go/admin/elastic_profiles")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/config_xml")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/config/server")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/users")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/backup")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/plugins")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/package_repositories/new")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/security/auth_configs")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/security/roles")).not.toBeInDOM();
    expect($root.find(`a.${styles.siteNavLink}`)).toHaveLength(4);
    expect($root.find(`a.${styles.siteSubNavLink}`)).toHaveLength(1);
  });

  it("should display the menus for Group Admins", () => {
    mount({
      canViewTemplates: true,
      isGroupAdmin: true,
      isUserAdmin: false,
      canViewAdminPage: true,
      showAnalytics: true
    } as Attrs);
    const dashboard = $root.find("a").get(0);
    const agents    = $root.find("a").get(1);
    const analytics = $root.find("a").get(2);
    const admin     = $root.find("a").get(3);
    expect(dashboard).toHaveText("Dashboard");
    expect(dashboard).toHaveAttr("href", "/go/pipelines");
    expect(agents).toHaveText("Agents");
    expect(agents).toHaveAttr("href", "/go/agents");
    expect(analytics).toHaveText("Analytics");
    expect(analytics).toHaveAttr("href", "/go/analytics");
    expect(admin).toHaveText("Admin");
    expect(findMenuItem("/go/admin/pipelines")).toHaveText("Pipelines");
    expect(findMenuItem("/go/admin/config_repos")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/templates")).toHaveText("Templates");
    expect(findMenuItem("/go/admin/elastic_profiles")).toHaveText("Elastic Agent Profiles");
    expect(findMenuItem("/go/admin/pipelines/snippet")).toHaveText("Config XML");
    expect(findMenuItem("/go/admin/config/server")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/users")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/backup")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/plugins")).toHaveText("Plugins");
    expect(findMenuItem("/go/admin/package_repositories/new")).toHaveText("Package Repositories");
    expect(findMenuItem("/go/admin/security/auth_configs")).not.toBeInDOM();
    expect(findMenuItem("/go/admin/security/roles")).not.toBeInDOM();
    expect($root.find(`a.${styles.siteNavLink}`)).toHaveLength(4);
    expect($root.find(`a.${styles.siteSubNavLink}`)).toHaveLength(6);
  });

  it("should not show analytics when the attribute is passed as false", () => {
    mount({
      canViewTemplates: true,
      isGroupAdmin: true,
      isUserAdmin: false,
      canViewAdminPage: true,
      showAnalytics: false
    } as Attrs);
    const dashboard = $root.find("a").get(0);
    const agents    = $root.find("a").get(1);
    const admin     = $root.find("a").get(2);
    expect(dashboard).toHaveText("Dashboard");
    expect(dashboard).toHaveAttr("href", "/go/pipelines");
    expect(agents).toHaveText("Agents");
    expect(agents).toHaveAttr("href", "/go/agents");
    expect(admin).toHaveText("Admin");
    expect(findMenuItem("/go/analytics")).not.toBeInDOM();
  });

  it("should not show Admin link for non-admins", () => {
    mount({
            canViewTemplates: true,
            isGroupAdmin: false,
            isUserAdmin: false,
            canViewAdminPage: false,
            showAnalytics: false
          } as Attrs);
    const dashboard = $root.find("a").get(0);
    const agents    = $root.find("a").get(1);
    const admin     = $root.find("a").get(2);
    expect(dashboard).toHaveText("Dashboard");
    expect(dashboard).toHaveAttr("href", "/go/pipelines");
    expect(agents).toHaveText("Agents");
    expect(agents).toHaveAttr("href", "/go/agents");
    expect(admin).not.toHaveText("Admin");
  });

  function mount(menuAttrs: Attrs) {
    m.mount(root, {
      view() {
        return <SiteMenu {...menuAttrs}/>;
      }
    });
    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function findMenuItem(href: string) {
    return $root.find(`a[href='${href}']`);
  }

});
