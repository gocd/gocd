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

import * as $ from "jquery";
import * as m from "mithril";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {ConfigRepo, GitMaterialAttributes, LastParse} from "models/config_repos/types";
import {PluginInfo} from "models/shared/plugin_infos_new/plugin_info";
import * as simulateEvent from "simulate-event";
import * as uuid from "uuid/v4";
import {Attrs, ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import * as styles from "views/pages/config_repos/index.scss";

describe("ConfigReposWidget", () => {
  let $root: any, root: any;
  let attrs: Attrs<ConfigRepo[]>;
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
    expect(find("flash-message-info")).toContainText("There are no config repositories setup. Click the \"Add\" button to add one.");
  });

  it("should render a list of config repos", () => {
    const repo1 = createConfigRepo();
    const repo2 = createConfigRepo();
    configRepos([repo1, repo2]);
    m.redraw();
    expect(find("key-value-value-id")).toHaveLength(2);
    expect($(find("key-value-value-id").get(0))).toHaveText(repo1.id());
    expect($(find("key-value-value-id").get(1))).toHaveText(repo2.id());
  });

  it("should render a single config repo", () => {
    const repo = createConfigRepo();
    repo.lastParse(new LastParse("1234", true));
    configRepos([repo]);
    m.redraw();
    expect($root).toContainText("Last seen revision: 1234");
    expect($root.find(`.${styles.scmHeader}`)).toHaveText("SCM configuration for git material");
    expect(find("key-value-key-url")).toContainText(`URL`);
    expect(find("key-value-value-url")).toContainText((repo.material().attributes() as GitMaterialAttributes).url());
  });

  it("should render a warning message when plugin is missing", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    m.redraw();
    expect(find("flash-message-alert")).toHaveText("This plugin is missing.");
  });

  it("should render a warning message when parsing did not finish", () => {
    const repo = createConfigRepo();
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    m.redraw();
    expect(find("flash-message-warning")).toHaveText("This configuration repository was never parsed.");
  });

  it("should render a warning message when parsing failed", () => {
    const repo = createConfigRepo();
    repo.lastParse(new LastParse("foo", false, "blah!"));
    configRepos([repo]);
    pluginInfos([configRepoPluginInfo()]);
    m.redraw();
    expect(find("flash-message-warning")).toContainText("There was an error parsing this configuration repository:");
    expect(find("flash-message-warning")).toContainText("blah!");
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

  function createConfigRepo(id = uuid()) {
    return ConfigRepo.fromJSON({
      material: {
        type: "git",
        attributes: {
          url: "https://example.com/git/" + uuid(),
          name: "foo",
          auto_update: true
        }
      },
      configuration: [],
      last_parse: {},
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
