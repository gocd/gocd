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

import m from "mithril";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {EnvironmentHeader} from "views/pages/new-environments/environment_header_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("Environments Header Widget", () => {
  const helper = new TestHelper();

  let environment: EnvironmentWithOrigin;

  beforeEach(() => {
    environment = EnvironmentWithOrigin.fromJSON(data.environment_json());
    helper.mount(() => <EnvironmentHeader environment={environment}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render name of environment", () => {
    expect(helper.findByDataTestId("environment-header-for-" + environment.name())).toBeInDOM();
    expect(helper.findByDataTestId("env-name")).toHaveText(environment.name());
  });

  it("should render pipeline count", () => {
    expect(helper.findByDataTestId("key-value-key-pipeline-count")).toHaveText("Pipeline Count");
    expect(helper.findByDataTestId("key-value-value-pipeline-count")).toHaveText("2");
  });

  it("should render agent count", () => {
    expect(helper.findByDataTestId("key-value-key-agent-count")).toHaveText("Agent Count");
    expect(helper.findByDataTestId("key-value-value-agent-count")).toHaveText("2");
  });
});
