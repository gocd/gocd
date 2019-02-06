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
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import * as uuid from "uuid/v4";
import * as headerIconStyles from "views/components/header_icon/index.scss";
import {Attrs, ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import * as styles from "views/pages/config_repos/index.scss";

describe("ConfigReposWidget", () => {
  let $root: any, root: any;
  let attrs: Attrs<ConfigRepo>;
  let onDelete: jasmine.Spy;
  let onEdit: jasmine.Spy;
  let onRefresh: jasmine.Spy;
  let configRepos: Stream<ConfigRepo[] | null>;
  let pluginInfos: Stream<Array<PluginInfo<any>>>;

  beforeEach(() => {
    // @ts-ignore
    [$root, root] = window.createDomElementForTest();

    onDelete    = jasmine.createSpy("onDelete");
    onEdit      = jasmine.createSpy("onEdit");
    onRefresh   = jasmine.createSpy("onRefresh");
    configRepos = stream();
    pluginInfos = stream();

    attrs = {
      objects: configRepos,
      pluginInfos,
      onDelete,
      onEdit,
      onRefresh
    };
  });

  beforeEach(mount);

  afterEach(unmount);
  // @ts-ignore
  afterEach(window.destroyDomElementForTest);

  function mount() {
    m.mount(root, {
      view() {
        return (<ConfigReposWidget {...attrs}/>);
      }
    });

    m.redraw();
  }

  function unmount() {
    m.mount(root, null);
    m.redraw();
  }

  function find(id: string) {
    return $root.find(`[data-test-id='${id}']`);
  }

  it("should render a message when there are no config repos", () => {
    configRepos([]);
    m.redraw();
    expect(find("flash-message-info"))
      .toContainText("There are no config repositories setup. Click the \"Add\" button to add one.");
  });

  describe("Should render expanded config repo panel", () => {

    it("should render material details section", () => {
      configRepos([createConfigRepo()]);
      m.redraw();
      const materialPanel = find("config-repo-material-panel");
      const keyValuePair = materialPanel.children().get(1).children;
      const title = materialPanel.children().get(0);

      expect(title).toHaveText("Material");
      expect(keyValuePair[0]).toContainText("Type");
      expect(keyValuePair[0]).toContainText("git");
      expect(keyValuePair[1]).toContainText("URL");
      expect(keyValuePair[1]).toContainText("https://example.com/git");
      expect(keyValuePair[2]).toContainText("Branch");
      expect(keyValuePair[2]).toContainText("master");
    });

    it("should render config repository configuration details section", () => {
      configRepos([createConfigRepo("testPlugin")]);
      m.redraw();
      const materialPanel = find("config-repo-plugin-panel");
      const title = materialPanel.children().get(0) ;
      const keyValuePair = materialPanel.children().get(1).children;

      expect(title).toHaveText("Config Repository Configurations");
      expect(keyValuePair[0]).toContainText("Id");
      expect(keyValuePair[0]).toContainText("testPlugin");
      expect(keyValuePair[1]).toContainText("Plugin Id");
      expect(keyValuePair[1]).toContainText("json.config.plugin");
    });

    it("should render good modification details section", () => {
      configRepos([createConfigRepo()]);
      m.redraw();
      const materialPanel = find("config-repo-good-modification-panel");
      const icon = materialPanel.children().get(0);
      const title = materialPanel.children().get(1);
      const keyValuePair = materialPanel.children().get(2).children;

      expect(title).toHaveText("Good Modification");
      expect(icon).toHaveClass(styles.goodModificationIcon);
      expect(keyValuePair[0]).toContainText("Username");
      expect(keyValuePair[0]).toContainText("GaneshSPatil <ganeshpl@gmail.com>");
      expect(keyValuePair[1]).toContainText("Email");
      expect(keyValuePair[1]).toContainText("ganeshpl@gmail.com");
      expect(keyValuePair[2]).toContainText("Revision");
      expect(keyValuePair[2]).toContainText("1234");
      expect(keyValuePair[3]).toContainText("Comment");
      expect(keyValuePair[3]).toContainText("Revert \"Delete this\"\n\nThis reverts commit 9b402012ea5c24ce032c8ef4582c0a9ce2d14ade.");
      expect(keyValuePair[4]).toContainText("Modified Time");
      expect(keyValuePair[4]).toContainText("2019-01-11T11:24:08Z");
    });

    it("should render latest modification details section", () => {
      configRepos([createConfigRepo()]);
      m.redraw();
      const materialPanel = find("config-repo-latest-modification-panel");
      const icon = materialPanel.children().get(0);
      const title = materialPanel.children().get(1);
      const keyValuePair = materialPanel.children().get(2).children;

      expect(title).toHaveText("Latest Modification");
      expect(icon).toHaveClass(styles.errorLastModificationIcon);
      expect(keyValuePair[0]).toContainText("Username");
      expect(keyValuePair[0]).toContainText("Mahesh <mahesh@gmail.com>");
      expect(keyValuePair[1]).toContainText("Email");
      expect(keyValuePair[1]).toContainText("mahesh@gmail.com");
      expect(keyValuePair[2]).toContainText("Revision");
      expect(keyValuePair[2]).toContainText("5432");
      expect(keyValuePair[3]).toContainText("Comment");
      expect(keyValuePair[3]).toContainText("Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.");
      expect(keyValuePair[4]).toContainText("Modified Time");
      expect(keyValuePair[4]).toContainText("2019-01-14T05:39:40Z");
      expect(keyValuePair[5]).toContainText("Error");
      expect(keyValuePair[5]).toContainText("blah!");
    });
  });

  it("should render a list of config repos", () => {
    const repo1 = createConfigRepo();
    const repo2 = createConfigRepo();
    configRepos([repo1, repo2]);
    pluginInfos([configRepoPluginInfo()]);
    m.redraw();

    const repoIds = find('collapse-header');
    const repoIcons = find('config-repo-json-plugin-icon');

    expect(repoIds).toHaveLength(2);
    expect(repoIcons).toHaveLength(2);

    expect(repoIds.get(0)).toContainText(repo1.id());
    expect(repoIds.get(1)).toContainText(repo2.id());
  });

  it("should render config repos with their plugin-id, material url, commit message, username and revision in header", () => {
    const repo1 = createConfigRepo("Repo1", "https://example.com/git/90d9f82c-bbfd-4f70-ab09-fd72dee42427");
    const repo2 = createConfigRepo("Repo2", "https://example.com/git/0b4243ff-7431-48e1-a60e-a79b7b80b654");
    configRepos([repo1, repo2]);
    m.redraw();

    const title = $root.find(`.${styles.headerTitleText}`);
    expect(title).toHaveLength(6);

    expect(title.get(0)).toContainText("Repo1");
    expect(title.get(1)).toContainText("https://example.com/git/90d9f82c-bbfd-4f70-ab09-fd72dee42427");
    expect(title.get(2)).toContainText("Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.");
    expect(title.get(2)).toContainText("Mahesh <mahesh@gmail.com> | 5432");

    expect(title.get(3)).toContainText("Repo2");
    expect(title.get(4)).toContainText("https://example.com/git/0b4243ff-7431-48e1-a60e-a79b7b80b654");
  });

  it("should render a warning message when plugin is missing", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    m.redraw();
    expect(find("flash-message-alert")).toHaveText("This plugin is missing.");
    expect($root.find(`.${headerIconStyles.unknownIcon}`)).toBeInDOM();
  });

  it("should render a warning message when parsing did not finish", () => {
    const repo = createConfigRepo();
    repo.lastParse(null);
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    m.redraw();
    expect(find("flash-message-warning")).toHaveText("This configuration repository was never parsed.");
    expect($root.find(`.${styles.neverParsed}`)).toBeInDOM();
  });

  it("should render a warning message when parsing failed and there is no latest modification", () => {
    const repo = createErrorConfigRepo();
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    m.redraw();
    expect(find("flash-message-warning")).toContainText("There was an error parsing this configuration repository:");
    expect(find("flash-message-warning")).toContainText("blah!");
    expect($root.find(`.${styles.lastParseErrorIcon}`)).toBeInDOM();
  });

  it("should callback the delete function when delete button is clicked", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    m.redraw();

    simulateEvent.simulate(find("config-repo-delete").get(0), "click");

    expect(onDelete).toHaveBeenCalledWith(repo, jasmine.any(MouseEvent));
  });

  it("should callback the edit function when edit button is clicked", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    m.redraw();

    simulateEvent.simulate(find("config-repo-edit").get(0), "click");

    expect(onEdit).toHaveBeenCalledWith(repo, jasmine.any(MouseEvent));
  });

  it("should callback the refresh function when refresh button is clicked", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    m.redraw();

    simulateEvent.simulate(find("config-repo-refresh").get(0), "click");

    expect(onRefresh).toHaveBeenCalledWith(repo, jasmine.any(MouseEvent));
  });

  function createConfigRepo(id?: string, repoId?: string) {
    id = id || uuid();
    return ConfigRepo.fromJSON({
                                 material: {
                                   type: "git",
                                   attributes: {
                                     url: "https://example.com/git/" + (repoId || uuid()),
                                     name: "foo",
                                     auto_update: true,
                                     branch : "master"
                                   }
                                 },
                                 configuration: [{
                                   key: "file_pattern",
                                   value: "*.json"
                                 }],
                                 parse_info: {
                                   latest_parsed_modification: {
                                     username: "Mahesh <mahesh@gmail.com>",
                                     email_address: "mahesh@gmail.com",
                                     revision: "5432",
                                     comment: "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.",
                                     modified_time: "2019-01-14T05:39:40Z"
                                   },
                                   good_modification: {
                                     username: "GaneshSPatil <ganeshpl@gmail.com>",
                                     email_address: "ganeshpl@gmail.com",
                                     revision: "1234",
                                     comment: "Revert \"Delete this\"\n\nThis reverts commit 9b402012ea5c24ce032c8ef4582c0a9ce2d14ade.",
                                     modified_time: "2019-01-11T11:24:08Z"
                                   },
                                   error: "blah!"
                                 },
                                 id,
                                 plugin_id: "json.config.plugin"
                               });
  }

  function createErrorConfigRepo(id?: string, repoId?: string) {
    id = id || uuid();
    return ConfigRepo.fromJSON({
                                 material: {
                                   type: "git",
                                   attributes: {
                                     url: "https://example.com/git/" + (repoId || uuid()),
                                     name: "foo",
                                     auto_update: true,
                                     branch : "master"
                                   }
                                 },
                                 configuration: [{
                                   key: "file_pattern",
                                   value: "*.json"
                                 }],
                                 parse_info: {
                                   error: "blah!"
                                 },
                                 id,
                                 plugin_id: "json.config.plugin"
                               });
  }

  function configRepoPluginInfo() {
    const pluginInfoWithConfigRepoExtension = {
      id: "json.config.plugin",
      status: {
        state: "active"
      },
      about: {
        name: "JSON Configuration Plugin",
        version: "0.2",
        target_go_version: "16.1.0",
        description: "Configuration plugin that supports Go configuration in JSON",
        target_operating_systems: [],
        vendor: {
          name: "Tomasz Setkowski",
          url: "https://github.com/tomzo/gocd-json-config-plugin"
        }
      },
      extensions: [
        {
          type: "configrepo",
          plugin_settings: {
            configurations: [
              {
                key: "pipeline_pattern",
                metadata: {
                  required: false,
                  secure: false
                }
              }
            ],
            view: {
              template: "config repo plugin view"
            }
          }
        }
      ]
    };

    return PluginInfo.fromJSON(pluginInfoWithConfigRepoExtension);
  }
});
