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

import {docsUrl} from "gen/gocd_version";
import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
import {Agents} from "models/agents/agents";
import {EnvironmentJSON, Environments} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import styles from "views/components/collapsible_panel/index.scss";
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environments Widget", () => {
  const helper = new TestHelper();

  let environments: Environments;
  const xmlEnv        = data.xml_environment_json();
  const configRepoEnv = data.config_repo_environment_json();
  const env           = data.environment_json();

  function mountModal(envs: EnvironmentJSON[] = [xmlEnv, configRepoEnv, env]) {
    environments = Environments.fromJSON({_embedded: {environments: envs}});
    helper.mount(() => <EnvironmentsWidget environments={Stream(environments)}
                                           agents={Stream(new Agents())}
                                           onDelete={jasmine.createSpy()}
                                           onSuccessfulSave={_.noop}/>);
  }

  afterEach(helper.unmount.bind(helper));

  it("should render collapsible panel for each environment", () => {
    mountModal([xmlEnv, configRepoEnv, env]);
    expect(helper.byTestId("collapsible-panel-for-env-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("collapsible-panel-for-env-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("collapsible-panel-for-env-" + env.name)).toBeInDOM();
  });

  it("should render environment header for all the environments", () => {
    mountModal([xmlEnv, configRepoEnv, env]);
    expect(helper.byTestId("environment-header-for-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-header-for-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-header-for-" + env.name)).toBeInDOM();
  });

  it("should render environment body for all the environments", () => {
    mountModal([xmlEnv, configRepoEnv, env]);
    expect(helper.byTestId("environment-body-for-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-body-for-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-body-for-" + env.name)).toBeInDOM();
  });

  it("should render delete icon for all the environments", () => {
    mountModal([xmlEnv, configRepoEnv, env]);
    const deleteIcons = helper.allByTestId("Delete-icon");
    expect(deleteIcons).toHaveLength(3);
  });

  it("should render disabled delete icon for the environments that user can not administer", () => {
    xmlEnv.can_administer = false;

    mountModal([xmlEnv, configRepoEnv, env]);
    const deleteIcons = helper.allByTestId("Delete-icon");
    expect(deleteIcons).toHaveLength(3);

    expect(deleteIcons[0].title).toBe(`You are not authorized to delete '${xmlEnv.name}' environment.`);
    expect(deleteIcons[0]).toBeDisabled();
    expect(deleteIcons[1].title).toBeFalsy();
    expect(deleteIcons[1]).not.toBeDisabled();
    expect(deleteIcons[2].title).toBeFalsy();
    expect(deleteIcons[2]).not.toBeDisabled();
  });

  it("should render warning line if no pipelines and agents assigned to the environment", () => {
    const envJson     = data.environment_json();
    envJson.agents    = [];
    envJson.pipelines = [];
    mountModal([envJson]);
    expect(helper.byTestId("collapsible-panel-for-env-" + envJson.name)).toHaveClass(styles.warning);
  });

  it('should render info message if there are no environments are available', () => {
    mountModal([]);
    expect(helper.byTestId("no-environment-present-msg")).toBeInDOM();
    const noEnvPresentText = "No environments are displayed because either no environments have been set up or you are not authorized to view the pipelines within any of the environments.";
    expect(helper.byTestId("no-environment-present-msg")).toContainText(noEnvPresentText);
    expect(helper.byTestId("doc-link")).toBeInDOM();
    expect(helper.q("a", helper.byTestId("doc-link"))).toHaveAttr("href", docsUrl("configuration/managing_environments.html"));
  });
});
