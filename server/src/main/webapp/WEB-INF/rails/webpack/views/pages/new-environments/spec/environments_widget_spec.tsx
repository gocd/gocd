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
import {EnvironmentsWidget} from "views/pages/new-environments/environments_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environments Widget", () => {
  const helper = new TestHelper();

  let xmlEnv: EnvironmentJSON, configRepoEnv: EnvironmentJSON, env: EnvironmentJSON;
  let environments: Environments;

  beforeEach(() => {
    xmlEnv        = data.xml_environment_json();
    configRepoEnv = data.config_repo_environment_json();
    env           = data.environment_json();

    environments = Environments.fromJSON({_embedded: {environments: [xmlEnv, configRepoEnv, env]}});
    helper.mount(() => <EnvironmentsWidget environments={Stream(environments)}
                                           deleteEnvironment={jasmine.createSpy()}
                                           onSuccessfulSave={_.noop}/>);
  });

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
});
