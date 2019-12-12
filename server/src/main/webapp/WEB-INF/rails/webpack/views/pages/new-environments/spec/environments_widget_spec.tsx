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
import {ScrollManager} from "views/components/anchor/anchor";
import styles from "views/components/collapsible_panel/index.scss";
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {stubAllMethods, TestHelper} from "views/pages/spec/test_helper";

describe("Environments Widget", () => {
  const helper = new TestHelper();

  let sm: ScrollManager;

  function mountModal(envs: EnvironmentJSON[]) {
    sm = stubAllMethods(["shouldScroll", "getTarget", "setTarget", "scrollToEl", "hasTarget"]);
    helper.mount(() => <EnvironmentsWidget
      environments={Stream(Environments.fromJSON({_embedded: {environments: envs}}))}
      agents={Stream(new Agents())}
      onDelete={jasmine.createSpy()}
      onSuccessfulSave={_.noop}
      sm={sm}/>);
  }

  afterEach(helper.unmount.bind(helper));

  it("should render collapsible panel for each environment", () => {
    const xmlEnv        = data.xml_environment_json();
    const configRepoEnv = data.config_repo_environment_json();
    const env           = data.environment_json();
    mountModal([xmlEnv, configRepoEnv, env]);

    expect(helper.byTestId("collapsible-panel-for-env-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("collapsible-panel-for-env-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("collapsible-panel-for-env-" + env.name)).toBeInDOM();
  });

  it("should render environment header for all the environments", () => {
    const xmlEnv        = data.xml_environment_json();
    const configRepoEnv = data.config_repo_environment_json();
    const env           = data.environment_json();
    mountModal([xmlEnv, configRepoEnv, env]);

    expect(helper.byTestId("environment-header-for-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-header-for-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-header-for-" + env.name)).toBeInDOM();
  });

  it("should render environment body for all the environments", () => {
    const xmlEnv        = data.xml_environment_json();
    const configRepoEnv = data.config_repo_environment_json();
    const env           = data.environment_json();
    mountModal([xmlEnv, configRepoEnv, env]);

    expect(helper.byTestId("environment-body-for-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-body-for-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-body-for-" + env.name)).toBeInDOM();
  });

  it("should render delete icon for all the environments", () => {
    mountModal([data.xml_environment_json(), data.config_repo_environment_json(), data.environment_json()]);
    const deleteIcons = helper.allByTestId("Delete-icon");

    expect(deleteIcons).toHaveLength(3);
  });

  it("should render disabled delete icon for the environments that user can not administer", () => {
    const xmlEnv          = data.xml_environment_json();
    xmlEnv.can_administer = false;
    mountModal([xmlEnv, data.xml_environment_json()]);

    m.redraw.sync();

    const deleteIcons = helper.allByTestId("Delete-icon");

    expect(deleteIcons).toHaveLength(2);
    expect(deleteIcons[0].title).toBe(`You are not authorized to delete the '${xmlEnv.name}' environment.`);
    expect(deleteIcons[0]).toBeDisabled();
    expect(deleteIcons[1].title).toBeFalsy();
    expect(deleteIcons[1]).not.toBeDisabled();
  });

  it("should render disabled delete icon for the environments that are partially defined in config repository", () => {
    const env = data.environment_json();
    mountModal([data.xml_environment_json(), env]);
    const deleteIcons = helper.allByTestId("Delete-icon");

    expect(deleteIcons).toHaveLength(2);
    expect(deleteIcons[0].title).toBeFalsy();
    expect(deleteIcons[0]).not.toBeDisabled();
    expect(deleteIcons[1].title).toBe(`Cannot delete '${env.name}' environment as it is partially defined in config repository.`);
    expect(deleteIcons[1]).toBeDisabled();
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

    const noEnvPresentText = "Either no environments have been set up or you are not authorized to view the environments.";

    expect(helper.byTestId("no-environment-present-msg")).toContainText(noEnvPresentText);
    expect(helper.byTestId("doc-link")).toBeInDOM();
    expect(helper.q("a", helper.byTestId("doc-link"))).toHaveAttr("href", docsUrl("configuration/managing_environments.html"));
  });

  it('should have warning tooltip if environment is not associated with any pipeline and agent', () => {
    mountModal([data.environment_without_pipeline_and_agent_json()]);

    const tooltipIcon = helper.byTestId("warning-icon");

    expect(tooltipIcon).toBeInDOM();
    expect(helper.textByTestId("warning-tooltip-content", helper.byTestId("collapse-header")))
      .toBe("Neither pipelines nor agents are associated with this environment.");
  });

  it('should render error info if the element specified in the anchor does not exist', () => {
    let scrollManager: ScrollManager;
    scrollManager = {
      hasTarget:    jasmine.createSpy().and.callFake(() => true),
      getTarget:    jasmine.createSpy().and.callFake(() => "env"),
      shouldScroll: jasmine.createSpy(),
      setTarget:    jasmine.createSpy(),
      scrollToEl:   jasmine.createSpy()
    };

    helper.mount(() => <EnvironmentsWidget
      environments={Stream(Environments.fromJSON({_embedded: {environments: []}}))}
      agents={Stream(new Agents())}
      onDelete={jasmine.createSpy()}
      onSuccessfulSave={_.noop}
      sm={scrollManager}/>);

    expect(helper.byTestId("anchor-env-not-present")).toBeInDOM();

    const anchorElementNotPresent = "Either 'env' environment has not been set up or you are not authorized to view the same.";

    expect(helper.textByTestId("anchor-env-not-present")).toBe(anchorElementNotPresent);
  });
});
