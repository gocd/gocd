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

import m from "mithril";
import Stream from "mithril/stream";
import {Agents} from "models/agents/agents";
import {EnvironmentWithOrigin} from "models/new-environments/environments";
import data from "models/new-environments/spec/test_data";
import {EnvironmentHeader} from "views/pages/new-environments/environment_header_widget";
import {TestHelper} from "views/pages/spec/test_helper";
import {AgentWithOrigin} from "../../../../models/new-environments/environment_agents";
import {Origin, OriginType} from "../../../../models/origin";

describe("Environments Header Widget", () => {
  const helper = new TestHelper();

  let environment: EnvironmentWithOrigin;

  beforeEach(() => {
    const environmentJSON = data.environment_json();
    environment           = EnvironmentWithOrigin.fromJSON(environmentJSON);
    const agents          = new Agents(data.convert_to_agent(environment.agents()[0]), data.convert_to_agent(environment.agents()[1]));
    helper.mount(() => <EnvironmentHeader environment={environment} agents={Stream(agents)}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render name of environment", () => {
    expect(helper.byTestId("environment-header-for-" + environment.name())).toBeInDOM();
    expect(helper.textByTestId("env-name")).toBe(environment.name());
  });

  it("should render pipeline count", () => {
    expect(helper.textByTestId("key-value-key-pipeline-count")).toBe("Pipeline Count");
    expect(helper.textByTestId("key-value-value-pipeline-count")).toBe("2");
  });

  it("should render agent count", () => {
    // Non existent agent
    environment.agents().push(new AgentWithOrigin("some-agent", "", new Origin(OriginType.ConfigRepo)));

    expect(helper.textByTestId("key-value-key-agent-count")).toBe("Agent Count");
    expect(helper.textByTestId("key-value-value-agent-count")).toBe("2");
  });
});
