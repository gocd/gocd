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
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import * as uuid from "uuid/v4";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as headerIconStyles from "views/components/header_icon/index.scss";
import {TestHelper} from "views/pages/spec/test_helper";
import {Attrs, ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import * as styles from "views/pages/config_repos/index.scss";

describe("ConfigReposWidget", () => {
  let attrs: Attrs<ConfigRepo>;
  let onDelete: jasmine.Spy;
  let onEdit: jasmine.Spy;
  let onRefresh: jasmine.Spy;
  let configRepos: Stream<ConfigRepo[]>;
  let pluginInfos: Stream<Array<PluginInfo<any>>>;

  const helper = new TestHelper();

  beforeEach(() => {
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

  beforeEach(() => {
    helper.mount(() => <ConfigReposWidget {...attrs}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render a message when there are no config repos", () => {
    configRepos([]);
    helper.redraw();
    expect(helper.findByDataTestId("flash-message-info"))
      .toContainText("There are no config repositories setup. Click the \"Add\" button to add one.");
  });

  describe("Expanded config repo details", () => {

    it("should render material details section", () => {
      configRepos([createConfigRepo()]);
      helper.redraw();
      const materialPanel = helper.findByDataTestId("config-repo-material-panel");
      const title         = materialPanel.children().get(0);
      const keyValuePair  = materialPanel.children().get(1).children[0].children;

      expect(title).toHaveText("Material");
      expect(keyValuePair[0]).toContainText("Type");
      expect(keyValuePair[0]).toContainText("git");
      expect(keyValuePair[1]).toContainText("URL");
      expect(keyValuePair[1]).toContainText("https://example.com/git");
      expect(keyValuePair[2]).toContainText("Branch");
      expect(keyValuePair[2]).toContainText("master");
    });

    it("should render config repository configuration details section", () => {
      configRepos([createConfigRepo({id: "testPlugin"})]);
      helper.redraw();
      const materialPanel = helper.findByDataTestId("config-repo-plugin-panel");
      const title         = materialPanel.children().get(0);
      const keyValuePair  = materialPanel.children().get(1).children[0].children;

      expect(title).toHaveText("Config Repository Configurations");
      expect(keyValuePair[0]).toContainText("Id");
      expect(keyValuePair[0]).toContainText("testPlugin");
      expect(keyValuePair[1]).toContainText("Plugin Id");
      expect(keyValuePair[1]).toContainText("json.config.plugin");
    });

    it("should render good modification details section", () => {
      configRepos([createConfigRepo()]);
      helper.redraw();
      const materialPanel = helper.findByDataTestId("config-repo-good-modification-panel");
      const icon          = materialPanel.find(`.${styles.goodModificationIcon}`);
      const title         = materialPanel.find(`.${styles.sectionHeaderTitle}`);
      const keyValuePair  = materialPanel.find(`.${styles.configRepoProperties} li`);

      expect(title).toHaveText("Last known good commit currently being used");
      expect(icon).toHaveClass(styles.goodModificationIcon);
      expect(keyValuePair[0]).toContainText("Username");
      expect(keyValuePair[0]).toContainText("GaneshSPatil <ganeshpl@gmail.com>");
      expect(keyValuePair[1]).toContainText("Email");
      expect(keyValuePair[1]).toContainText("ganeshpl@gmail.com");
      expect(keyValuePair[2]).toContainText("Revision");
      expect(keyValuePair[2]).toContainText("1234");
      expect(keyValuePair[3]).toContainText("Comment");
      expect(keyValuePair[3])
        .toContainText("Revert \"Delete this\"\n\nThis reverts commit 9b402012ea5c24ce032c8ef4582c0a9ce2d14ade.");
      expect(keyValuePair[4]).toContainText("Modified Time");
      expect(keyValuePair[4]).toContainText("2019-01-11T11:24:08Z");
    });

    it("should render latest modification details section", () => {
      configRepos([createConfigRepo()]);
      helper.redraw();
      const materialPanel = helper.findByDataTestId("config-repo-latest-modification-panel");
      const icon          = materialPanel.find(`.${styles.errorLastModificationIcon}`);
      const title         = materialPanel.find(`.${styles.sectionHeaderTitle}`);
      const keyValuePair  = materialPanel.find(`.${styles.configRepoProperties} li`);

      expect(title).toHaveText("Latest commit in the repository");
      expect(icon).toHaveClass(styles.errorLastModificationIcon);
      expect(keyValuePair[0]).toContainText("Username");
      expect(keyValuePair[0]).toContainText("Mahesh <mahesh@gmail.com>");
      expect(keyValuePair[1]).toContainText("Email");
      expect(keyValuePair[1]).toContainText("mahesh@gmail.com");
      expect(keyValuePair[2]).toContainText("Revision");
      expect(keyValuePair[2]).toContainText("5432");
      expect(keyValuePair[3]).toContainText("Comment");
      expect(keyValuePair[3])
        .toContainText(
          "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.");
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
    helper.redraw();

    const repoIds = helper.findByDataTestId("collapse-header");
    expect(repoIds).toHaveLength(2);
    expect(helper.findByDataTestId("plugin-icon")).toHaveLength(2);
    expect(helper.findByDataTestId("plugin-icon").get(0))
      .toHaveAttr("src", "http://localhost:8153/go/api/plugin_images/json.config.plugin/f787");

    expect(repoIds.get(0)).toContainText(repo1.id());
    expect(repoIds.get(1)).toContainText(repo2.id());
  });

  it("should render config repo's plugin-id, material url, commit-message, username and revision in header", () => {
    const repo1 = createConfigRepo({
                                     id: "Repo1",
                                     repoId: "90d9f82c-bbfd-4f70-ab09-fd72dee42427"
                                   });
    const repo2 = createConfigRepo({
                                     id: "Repo2",
                                     repoId: "0b4243ff-7431-48e1-a60e-a79b7b80b654"
                                   });
    configRepos([repo1, repo2]);
    helper.redraw();

    expect(helper.find(`.${styles.headerTitleText}`)[0]).toContainText("Repo1");
    expect(helper.find(`.${styles.headerTitleUrl}`)[0])
      .toContainText("https://example.com/git/90d9f82c-bbfd-4f70-ab09-fd72dee42427");
    expect(helper.find(`.${styles.comment}`)[0])
      .toContainText("Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d...");
    expect(helper.find(`.${styles.committerInfo}`)[0]).toContainText("Mahesh <mahesh@gmail.com> | 5432");

    expect(helper.find(`.${styles.headerTitleText}`)[1]).toContainText("Repo2");
    expect(helper.find(`.${styles.headerTitleUrl}`)[1])
      .toContainText("https://example.com/git/0b4243ff-7431-48e1-a60e-a79b7b80b654");
  });

  it("should render config repo's trimmed commit-message, username and revision if they are too long to fit in header",
     () => {
       const repo1 = createConfigRepo({
                                        id: "Repo1",
                                        repoId: "https://example.com/",
                                        latestCommitMessage: "A very very long commit message which will be trimmed after 82 characters and this is being tested",
                                        latestCommitUsername: "A_Long_username_with_a_long_long_long_long_long_text",
                                        latestCommitRevision: "df31759540dc28f75a20f443a19b1148df31759540dc28f75a20f443a19b1148df"
                                      });

       configRepos([repo1]);
       helper.redraw();

       expect(helper.find(`.${styles.headerTitleText}`)).toContainText("Repo1");
       expect(helper.find(`.${styles.headerTitleUrl}`)).toContainText("https://example.com/");
       expect(helper.find(`.${styles.comment}`))
         .toContainText("A very very long commit message which will be trimmed after 82 characters and thi...");
       expect(helper.find(`.${styles.committerInfo}`))
         .toContainText("A_Long_username_with_a_long_long_long... | df31759540dc28f75a20f443a19b1148df317...");
     });

  it("should render a warning message when plugin is missing", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    helper.redraw();
    expect(helper.findByDataTestId("flash-message-alert")).toHaveText("This plugin is missing.");
    expect(helper.find(`.${headerIconStyles.unknownIcon}`)).toBeInDOM();
  });

  it("should render a warning message when parsing did not finish", () => {
    const repo = createConfigRepo();
    repo.lastParse(null);
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    helper.redraw();
    expect(helper.findByDataTestId("flash-message-alert")).toHaveText("This configuration repository was never parsed.");
  });

  it("should render a warning message when parsing failed and there is no latest modification", () => {
    const repo = createConfigRepoWithError();
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    helper.redraw();
    expect(helper.findByDataTestId("flash-message-alert")).toContainText("There was an error parsing this configuration repository:");
    expect(helper.findByDataTestId("flash-message-alert")).toContainText("blah!");
  });

  it("should render in-progress icon when material update is in progress", () => {
    const repo = createConfigRepo({material_update_in_progress: true});
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    helper.redraw();
    expect(helper.findByDataTestId("repo-update-in-progress-icon")).toBeInDOM();
    expect(helper.findByDataTestId("repo-update-in-progress-icon")).toHaveClass(styles.configRepoUpdateInProgress);
  });

  it("should render red top border and expand the widget to indicate error in config repo parsing", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    helper.redraw();
    const detailsPanel = helper.findByDataTestId("config-repo-details-panel");
    expect(detailsPanel).toBeInDOM();
    expect(detailsPanel).toHaveClass(collapsiblePanelStyles.error);
    expect(detailsPanel).toHaveAttr("data-test-element-state", "expanded");
  });

  it("should not render top border and keep the widget collapsed when there is no error", () => {
    const repo = createConfigRepo({});
    if (repo.lastParse()) {
      repo.lastParse()!.error(null);
    }
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    helper.redraw();
    const detailsPanel = helper.findByDataTestId("config-repo-details-panel");
    expect(detailsPanel).toBeInDOM();
    expect(detailsPanel).not.toHaveClass(collapsiblePanelStyles.error);
    expect(detailsPanel).toHaveAttr("data-test-element-state", "collapsed");
  });

  it("should callback the delete function when delete button is clicked", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    helper.redraw();

    simulateEvent.simulate(helper.findByDataTestId("config-repo-delete").get(0), "click");

    expect(onDelete).toHaveBeenCalledWith(repo, jasmine.any(MouseEvent));
  });

  it("should callback the edit function when edit button is clicked", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    helper.redraw();

    simulateEvent.simulate(helper.findByDataTestId("config-repo-edit").get(0), "click");

    expect(onEdit).toHaveBeenCalledWith(repo, jasmine.any(MouseEvent));
  });

  it("should callback the refresh function when refresh button is clicked", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    helper.redraw();

    simulateEvent.simulate(helper.findByDataTestId("config-repo-refresh").get(0), "click");

    expect(onRefresh).toHaveBeenCalledWith(repo, jasmine.any(MouseEvent));
  });

  function createConfigRepo(overrides?: any) {
    const parameters = {
      id: uuid(),
      repoId: uuid(),
      material_update_in_progress: false,
      latestCommitMessage: "Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d344fbbb9b1.",
      latestCommitUsername: "Mahesh <mahesh@gmail.com>",
      latestCommitRevision: "5432",
      ...overrides
    };

    return ConfigRepo.fromJSON({
                                 material: {
                                   type: "git",
                                   attributes: {
                                     url: "https://example.com/git/" + (parameters.repoId),
                                     name: "foo",
                                     auto_update: true,
                                     branch: "master"
                                   }
                                 },
                                 configuration: [{
                                   key: "file_pattern",
                                   value: "*.json"
                                 }],
                                 parse_info: {
                                   latest_parsed_modification: {
                                     username: parameters.latestCommitUsername,
                                     email_address: "mahesh@gmail.com",
                                     revision: parameters.latestCommitRevision,
                                     comment: parameters.latestCommitMessage,
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
                                 id: parameters.id,
                                 plugin_id: "json.config.plugin",
                                 material_update_in_progress: parameters.material_update_in_progress
                               });
  }

  function createConfigRepoWithError(id?: string, repoId?: string) {
    id = id || uuid();
    return ConfigRepo.fromJSON({
                                 material: {
                                   type: "git",
                                   attributes: {
                                     url: "https://example.com/git/" + (repoId || uuid()),
                                     name: "foo",
                                     auto_update: true,
                                     branch: "master"
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
                                 plugin_id: "json.config.plugin",
                                 material_update_in_progress: false
                               });
  }

  function configRepoPluginInfo() {
    const links                             = {
      self: {
        href: "http://localhost:8153/go/api/admin/plugin_info/json.config.plugin"
      },
      doc: {
        href: "https://api.gocd.org/#plugin-info"
      },
      find: {
        href: "http://localhost:8153/go/api/admin/plugin_info/:plugin_id"
      },
      image: {
        href: "http://localhost:8153/go/api/plugin_images/json.config.plugin/f787"
      }
    };
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

    return PluginInfo.fromJSON(pluginInfoWithConfigRepoExtension, links);
  }
});
