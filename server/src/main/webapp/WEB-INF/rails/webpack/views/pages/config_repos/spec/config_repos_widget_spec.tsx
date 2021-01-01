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

import {docsUrl} from "gen/gocd_version";
import {asSelector} from "helpers/css_proxies";
import _ from 'lodash';
import m from "mithril";
import Stream from "mithril/stream";
import {ConfigRepo} from "models/config_repos/types";
import {Rule} from "models/rules/rules";
import {PluginInfos} from "models/shared/plugin_infos_new/plugin_info";
import {ScrollManager} from "views/components/anchor/anchor";
import * as collapsiblePanelStyles from "views/components/collapsible_panel/index.scss";
import * as headerIconStyles from "views/components/header_icon/index.scss";
import {ConfigReposWidget} from "views/pages/config_repos/config_repos_widget";
import {ConfigRepoVM} from "views/pages/config_repos/config_repo_view_model";
import styles from "views/pages/config_repos/index.scss";
import {
  configRepoPluginInfo,
  createConfigRepoParsed,
  createConfigRepoParsedWithError,
  createConfigRepoWithError
} from "views/pages/config_repos/spec/test_data";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";

const sel = asSelector(styles);

describe("ConfigReposWidget", () => {
  let showDeleteModal: jasmine.Spy;
  let showEditModal: jasmine.Spy;
  const reparseRepo: jasmine.Spy = jasmine.createSpy("reparseRepo");
  let models: Stream<ConfigRepoVM[]>;
  let pluginInfos: Stream<PluginInfos>;
  let sm: ScrollManager;

  const helper = new TestHelper();

  function vm(repo: ConfigRepo): ConfigRepoVM {
    const reparseRepoPromise: () => Promise<void> = () => new Promise((resolve) => {
      reparseRepo();
      resolve();
    });

    return {
      repo,
      results: {
        // tslint:disable-next-line no-empty
        prime(f) {}, invalidate() {}, ready() { return false; },
        failureReason() { return void 0; }, failed() { return false; },
        contents() { return { children: [], name() { return ""; } }; }
      },
      showDeleteModal,
      showEditModal,
      reparseRepo: reparseRepoPromise,
      // tslint:disable-next-line no-empty
      on(t, fn) {}, off(t) {}, reset() {}, notify(t) {}
    };
  }

  beforeEach(() => {
    showDeleteModal = jasmine.createSpy("showDeleteModal");
    showEditModal   = jasmine.createSpy("showEditModal");
    sm              = stubAllMethods(["shouldScroll", "getTarget", "setTarget", "scrollToEl", "hasTarget"]);
    models          = Stream([] as ConfigRepoVM[]);
    pluginInfos     = Stream(new PluginInfos());

    helper.mount(() => <ConfigReposWidget {...{
      flushEtag: _.noop,
      models,
      pluginInfos,
      sm,
      urlGenerator: {
        webhookUrlFor: (type: string, id: string) => `http://gocd/${type}/${id}`,
        siteUrlsConfigured() { return true; }
      }
    }}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("renders webhook suggestions when autoUpdate is false", () => {
    models([vm(createConfigRepoParsed({auto_update: false}))]);
    helper.redraw();

    expect(helper.byTestId("config-repo-plugin-panel")).toBeInDOM();
    expect(helper.q(sel.webhookSuggestions)).toBeInDOM();
  });

  it("does not render webhook suggestions when autoUpdate is true", () => {
    models([vm(createConfigRepoParsed())]);
    helper.redraw();

    expect(helper.byTestId("config-repo-plugin-panel")).toBeInDOM();
    expect(helper.q(sel.webhookSuggestions)).not.toBeInDOM();
  });

  it("should render a message when there are no config repos", () => {
    models([]);
    helper.redraw();
    expect(helper.textByTestId("flash-message-info")).toBe("Either no config repositories have been set up or you are not authorized to view the same. Learn More");
    expect(helper.byTestId("doc-link")).toBeInDOM();
    expect(helper.q("a", helper.byTestId("doc-link"))).toHaveAttr("href", docsUrl("advanced_usage/pipelines_as_code.html"));
  });

  describe("Expanded config repo details", () => {

    it("should render material details section", () => {
      models([vm(createConfigRepoParsedWithError())]);
      helper.redraw();
      const materialPanel = helper.byTestId("config-repo-material-panel");
      const title         = materialPanel.children.item(0);
      const keyValuePair  = materialPanel.children.item(1)!.children.item(0)!;

      expect(title).toHaveText("Material");
      expect(helper.byTestId("key-value-key-type", keyValuePair)).toContainText("Type");
      expect(helper.byTestId("key-value-value-type", keyValuePair)).toContainText("git");
      expect(helper.byTestId("key-value-key-username", keyValuePair)).toContainText("Username");
      expect(helper.byTestId("key-value-value-username", keyValuePair)).toContainText("bob");
      expect(helper.byTestId("key-value-key-password", keyValuePair)).toContainText("Password");
      expect(helper.byTestId("key-value-value-password", keyValuePair)).toContainText("*******");
      expect(helper.byTestId("key-value-key-url", keyValuePair)).toContainText("URL");
      expect(helper.byTestId("key-value-value-url", keyValuePair)).toContainText("https://example.com/git");
      expect(helper.byTestId("key-value-key-branch", keyValuePair)).toContainText("Branch");
      expect(helper.byTestId("key-value-value-branch", keyValuePair)).toContainText("master");
    });

    it("should render config repository configuration details section", () => {
      models([vm(createConfigRepoParsedWithError({id: "testPlugin"}))]);
      helper.redraw();
      const materialPanel = helper.byTestId("config-repo-plugin-panel");
      const title         = materialPanel.children.item(0);
      const keyValuePair  = materialPanel.children.item(1)!.children.item(0)!.children;

      expect(title).toHaveText("Config Repository Configurations");
      expect(keyValuePair[0]).toContainText("Name");
      expect(keyValuePair[0]).toContainText("testPlugin");
      expect(keyValuePair[1]).toContainText("Plugin Id");
      expect(keyValuePair[1]).toContainText("json.config.plugin");
    });

    it("renders user-defined properties section", () => {
      const repo = ConfigRepo.fromJSON({
        material: {
          type: "git",
          attributes: {
            url: "https://example.com/git/my-repo",
            name: "",
            auto_update: true,
            branch: "master",
            destination: ""
          }
        },
        can_administer: false,
        configuration: [
          { key: "userdef.I yam what I yam", value: "And that's all that I yam" },
          { key: "chipmunks", value: "Alvin, Simon, Theodore" },
          { key: "userdef.a cow says", encrypted_value: "moo" },
        ],
        parse_info: {},
        id: "my-repo",
        plugin_id: "json.config.plugin",
        material_update_in_progress: false,
        rules: []
      });

      models([vm(repo)]);
      helper.redraw();

      const panel = helper.byTestId("config-repo-user-properties-panel");

      expect(panel).toBeInDOM();

      expect(helper.text(panel)).not.toMatch("chipmunks");
      expect(helper.text(panel)).not.toMatch("Alvin");

      expect(helper.byTestId("key-value-key-i-yam-what-i-yam", panel)).toBeInDOM();
      expect(helper.textByTestId("key-value-key-i-yam-what-i-yam", panel)).toBe("I yam what I yam");
      expect(helper.textByTestId("key-value-value-i-yam-what-i-yam", panel)).toBe("And that's all that I yam");

      expect(helper.byTestId("key-value-key-a-cow-says", panel)).toBeInDOM();
      expect(helper.textByTestId("key-value-key-a-cow-says", panel)).toBe("a cow says");
      expect(helper.textByTestId("key-value-value-a-cow-says", panel)).toBe("********************************");
      expect(helper.byTestId("Lock-icon", helper.byTestId("key-value-value-a-cow-says", panel))).toBeInDOM();

      expect(helper.byTestId("key-value-key-i-yam-what-i-yam", panel)).toBeInDOM();
      expect(helper.textByTestId("key-value-key-i-yam-what-i-yam", panel)).toBe("I yam what I yam");
    });

    it("should ONLY render latest modification wheb good === latest", () => {
      models([vm(createConfigRepoParsed())]);
      helper.redraw();
      const materialPanel = helper.byTestId("config-repo-latest-modification-panel");
      const icon          = materialPanel.querySelector(`.${styles.goodModificationIcon}`);
      const title         = materialPanel.querySelector(`.${styles.sectionHeaderTitle}`);

      expect(materialPanel).toBeInDOM();
      expect(helper.byTestId("config-repo-good-modification-panel")).toBeNull();
      expect(title).toHaveText("Latest commit in the repository");
      expect(icon).toBeInDOM();
    });

    it("should render good modification details section", () => {
      models([vm(createConfigRepoParsedWithError())]);
      helper.redraw();
      const materialPanel = helper.byTestId("config-repo-good-modification-panel");
      const icon          = materialPanel.querySelector(`.${styles.goodModificationIcon}`);
      const title         = materialPanel.querySelector(`.${styles.sectionHeaderTitle}`);
      const keyValuePair  = Array.from(materialPanel.querySelectorAll(`.${styles.configRepoProperties} li`));

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
      models([vm(createConfigRepoParsedWithError())]);
      helper.redraw();
      const materialPanel = helper.byTestId("config-repo-latest-modification-panel");
      const icon          = materialPanel.querySelector(`.${styles.errorLastModificationIcon}`);
      const title         = materialPanel.querySelector(`.${styles.sectionHeaderTitle}`);
      const keyValuePair  = materialPanel.querySelectorAll(`.${styles.configRepoProperties} li`);

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

    it('should render rules info if present', () => {
      const configRepo = createConfigRepoParsed();
      const rule       = new Rule("allow", "refer", "environment", "test-env");
      configRepo.rules().push(Stream(rule));
      models([vm(configRepo)]);
      helper.redraw();

      expect(helper.byTestId('rules-info')).toBeInDOM();
      const values = helper.qa('td', helper.byTestId('rule-table'));
      expect(values.length).toBe(3);
      expect(values[0].textContent).toBe('Allow');
      expect(values[1].textContent).toBe('Environment');
      expect(values[2].textContent).toBe(rule.resource());
    });
  });

  it("should render a list of config repos", () => {
    const repo1 = createConfigRepoParsedWithError();
    const repo2 = createConfigRepoParsedWithError();
    models([vm(repo1), vm(repo2)]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();

    const repoIds = Array.from(helper.allByTestId("collapse-header"));
    expect(repoIds).toHaveLength(2);
    expect(helper.allByTestId("plugin-icon")).toHaveLength(2);
    expect(helper.byTestId("plugin-icon"))
      .toHaveAttr("src", "http://localhost:8153/go/api/plugin_images/json.config.plugin/f787");

    expect(repoIds[0]).toContainText(repo1.id());
    expect(repoIds[1]).toContainText(repo2.id());
  });

  it("should render config repo's plugin-id, material url, commit-message, username and revision in header", () => {
    const repo1 = createConfigRepoParsedWithError({
      id: "Repo1",
      repoId: "90d9f82c-bbfd-4f70-ab09-fd72dee42427"
    });
    const repo2 = createConfigRepoParsedWithError({
      id: "Repo2",
      repoId: "0b4243ff-7431-48e1-a60e-a79b7b80b654"
    });
    models([vm(repo1), vm(repo2)]);
    helper.redraw();

    expect(helper.q(`.${styles.headerTitleText}`)).toContainText("Repo1");
    expect(helper.q(`.${styles.headerTitleUrl}`))
      .toContainText("https://example.com/git/90d9f82c-bbfd-4f70-ab09-fd72dee42427");
    expect(helper.q(`.${styles.comment}`))
      .toContainText("Revert \"Revert \"Delete this\"\"\n\nThis reverts commit 2daccbb7389e87c9eb789f6188065d...");
    expect(helper.q(`.${styles.committerInfo}`)).toContainText("Mahesh <mahesh@gmail.com> | 5432");

    expect(helper.qa(`.${styles.headerTitleText}`).item(1)).toContainText("Repo2");
    expect(helper.qa(`.${styles.headerTitleUrl}`).item(1))
      .toContainText("https://example.com/git/0b4243ff-7431-48e1-a60e-a79b7b80b654");
  });

  it("should render config repo's trimmed commit-message, username and revision if they are too long to fit in header",
    () => {
      const repo = createConfigRepoParsedWithError({
        id: "Repo1",
        repoId: "https://example.com/",
        latestCommitMessage: "A very very long commit message which will be trimmed after 82 characters and this is being tested",
        latestCommitUsername: "A_Long_username_with_a_long_long_long_long_long_text",
        latestCommitRevision: "df31759540dc28f75a20f443a19b1148df31759540dc28f75a20f443a19b1148df"
      });

      models([vm(repo)]);
      helper.redraw();

      expect(helper.q(`.${styles.headerTitleText}`)).toContainText("Repo1");
      expect(helper.q(`.${styles.headerTitleUrl}`)).toContainText("https://example.com/");
      expect(helper.q(`.${styles.comment}`))
        .toContainText("A very very long commit message which will be trimmed after 82 characters and thi...");
      expect(helper.q(`.${styles.committerInfo}`))
        .toContainText("A_Long_username_with_a_long_long_long... | df31759540dc28f75a20f443a19b1148df317...");
    });

  it("should render a warning message when plugin is missing", () => {
    const repo = createConfigRepoParsedWithError();
    models([vm(repo)]);
    helper.redraw();
    expect(helper.byTestId("flash-message-alert")).toHaveText("This plugin is missing.");
    expect(helper.q(`.${headerIconStyles.unknownIcon}`)).toBeInDOM();
  });

  it("should render a warning message when parsing did not finish", () => {
    const repo = createConfigRepoParsedWithError();
    repo.lastParse(void 0);
    models([vm(repo)]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();
    expect(helper.byTestId("flash-message-info")).toHaveText("This configuration repository has not been parsed yet.");
  });

  it("should render a warning message when parsing failed and there is no latest modification", () => {
    const repo = createConfigRepoWithError();
    models([vm(repo)]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();
    expect(helper.byTestId("flash-message-alert")).toContainText("There was an error parsing this configuration repository:");
    expect(helper.byTestId("flash-message-alert")).toContainText("blah!");
  });

  it("should render in-progress icon when material update is in progress", () => {
    const repo = createConfigRepoParsedWithError({material_update_in_progress: true});
    models([vm(repo)]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();
    expect(helper.byTestId("repo-update-in-progress-icon")).toBeInDOM();
    expect(helper.byTestId("repo-update-in-progress-icon")).toHaveClass(styles.configRepoUpdateInProgress);
  });

  it("should render happy state checkmark when there are no errors", () => {
    models([vm(createConfigRepoParsed())]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();
    expect(helper.byTestId("repo-success-state")).toBeInDOM();
    expect(helper.byTestId("repo-success-state")).toHaveClass(styles.configRepoSuccessState);
  });

  it("should render red top border and expand the widget to indicate error in config repo parsing", () => {
    const repo = createConfigRepoParsedWithError();
    models([vm(repo)]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();
    const detailsPanel = helper.byTestId("config-repo-details-panel");
    expect(detailsPanel).toBeInDOM();
    expect(detailsPanel).toHaveClass(collapsiblePanelStyles.error);
    expect(detailsPanel).toHaveAttr("data-test-element-state", "expanded");
  });

  it("should not render top border and keep the widget collapsed when there is no error", () => {
    const repo = createConfigRepoParsedWithError({});
    repo.lastParse()?.error(void 0);

    models([vm(repo)]);
    pluginInfos(new PluginInfos(configRepoPluginInfo()));
    helper.redraw();
    const detailsPanel = helper.byTestId("config-repo-details-panel");
    expect(detailsPanel).toBeInDOM();
    expect(detailsPanel).not.toHaveClass(collapsiblePanelStyles.error);
    expect(detailsPanel).toHaveAttr("data-test-element-state", "collapsed");
  });

  it("should callback the delete function when delete button is clicked", () => {
    const repo = createConfigRepoParsedWithError();
    models([vm(repo)]);
    helper.redraw();

    helper.clickByTestId("config-repo-delete");

    expect(showDeleteModal).toHaveBeenCalledWith(jasmine.any(MouseEvent));
  });

  it("should callback the edit function when edit button is clicked", () => {
    const repo = createConfigRepoParsedWithError();
    models([vm(repo)]);
    helper.redraw();

    helper.clickByTestId("config-repo-edit");

    expect(showEditModal).toHaveBeenCalledWith(jasmine.any(MouseEvent));
  });

  it("should callback the refresh function when refresh button is clicked", () => {
    const repo = createConfigRepoParsedWithError();
    models([vm(repo)]);
    helper.redraw();

    helper.clickByTestId("config-repo-refresh");

    expect(reparseRepo).toHaveBeenCalled();
  });

  it("should disable the action buttons", () => {
    const configRepo = createConfigRepoParsed();
    models([vm(configRepo)]);
    helper.redraw();

    const title = "You are not authorised to perform this action!";
    expect(helper.byTestId("config-repo-refresh")).toBeInDOM();
    expect(helper.byTestId("config-repo-refresh")).toBeDisabled();
    expect(helper.byTestId("config-repo-refresh").title).toBe(title);
    expect(helper.byTestId("config-repo-edit")).toBeInDOM();
    expect(helper.byTestId("config-repo-edit")).toBeDisabled();
    expect(helper.byTestId("config-repo-edit").title).toBe(title);
    expect(helper.byTestId("config-repo-delete")).toBeInDOM();
    expect(helper.byTestId("config-repo-delete")).toBeDisabled();
    expect(helper.byTestId("config-repo-delete").title).toBe(title);
  });

  it("should not disable the action buttons and add titles when user has admin permissions", () => {
    const configRepo = createConfigRepoParsed();
    configRepo.canAdminister(true);
    models([vm(configRepo)]);
    helper.redraw();

    expect(helper.byTestId("config-repo-refresh")).toBeInDOM();
    expect(helper.byTestId("config-repo-refresh")).not.toBeDisabled();
    expect(helper.byTestId("config-repo-refresh").title).toBeFalsy();
    expect(helper.byTestId("config-repo-edit")).toBeInDOM();
    expect(helper.byTestId("config-repo-edit")).not.toBeDisabled();
    expect(helper.byTestId("config-repo-edit").title).toBeFalsy();
    expect(helper.byTestId("config-repo-delete")).toBeInDOM();
    expect(helper.byTestId("config-repo-delete")).not.toBeDisabled();
    expect(helper.byTestId("config-repo-delete").title).toBeFalsy();
  });

  it('should render error msg when the anchor element is not present', () => {
    let scrollManager: ScrollManager;
    scrollManager = {
      hasTarget: jasmine.createSpy().and.callFake(() => true),
      getTarget: jasmine.createSpy().and.callFake(() => "cr-test"),
      shouldScroll: jasmine.createSpy(),
      setTarget: jasmine.createSpy(),
      scrollToEl: jasmine.createSpy()
    };
    helper.unmount();
    helper.mount(() => <ConfigReposWidget {...{
      flushEtag: _.noop,
      models,
      pluginInfos,
      sm: scrollManager,
      urlGenerator: {
        webhookUrlFor: (type: string, id: string) => `http://gocd/${type}/${id}`,
        siteUrlsConfigured() { return true; }
      }
    }}/>);
    helper.redraw();

    const errorMsgElement = helper.byTestId("anchor-config-repo-not-present");
    expect(errorMsgElement).toBeInDOM();
    expect(errorMsgElement.innerText).toBe("Either 'cr-test' config repository has not been set up or you are not authorized to view the same. Learn More");

    expect(helper.q("a", errorMsgElement)).toBeInDOM();
    expect(helper.q("a", errorMsgElement).getAttribute("href")).toBe(docsUrl("advanced_usage/pipelines_as_code.html"));
  });
});
