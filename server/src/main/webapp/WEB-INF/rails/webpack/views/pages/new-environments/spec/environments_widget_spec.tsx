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

import _ from "lodash";
import m from "mithril";
import Stream from "mithril/stream";
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
                                           deleteEnvironment={jasmine.createSpy()}
                                           onSuccessfulSave={_.noop}/>);
  }

  beforeEach(() => mountModal());

  afterEach(helper.unmount.bind(helper));

  it("should render collapsible panel for each environment", () => {
    expect(helper.byTestId("collapsible-panel-for-env-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("collapsible-panel-for-env-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("collapsible-panel-for-env-" + env.name)).toBeInDOM();
  });

  it("should render environment header for all the environments", () => {
    expect(helper.byTestId("environment-header-for-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-header-for-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-header-for-" + env.name)).toBeInDOM();
  });

  it("should render environment body for all the environments", () => {
    expect(helper.byTestId("environment-body-for-" + xmlEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-body-for-" + configRepoEnv.name)).toBeInDOM();
    expect(helper.byTestId("environment-body-for-" + env.name)).toBeInDOM();
  });

  it("should render warning line if no pipelines and agents assigned to the environment", () => {
    helper.unmount();
    const envJson     = data.environment_json();
    envJson.agents    = [];
    envJson.pipelines = [];
    mountModal([envJson]);
    helper.redraw();
    expect(helper.byTestId("collapsible-panel-for-env-" + envJson.name)).toHaveClass(styles.warning);
  });
});
