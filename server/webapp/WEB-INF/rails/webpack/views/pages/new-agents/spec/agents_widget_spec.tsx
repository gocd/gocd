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

import * as m from "mithril";

import {Agents, AgentsJSON} from "models/new-agent/agents";
import {AgentsWidget} from "views/pages/new-agents/agents_widget";
import {TestHelper} from "views/pages/spec/test_helper";

describe("New Agents Widget", () => {
  const helper = new TestHelper();
  const agents = Agents.fromJSON(agentsJSON());

  beforeEach(() => {
    helper.mount(() => <AgentsWidget agents={agents}/>);
  });

  afterEach(helper.unmount.bind(helper));

  it("should render table headers", () => {
    const headers = helper.findByDataTestId("table-header-row");

    expect(headers.children()).toHaveLength(8);
    expect(headers.children()[0]).toContainText("Agent Name");
    expect(headers.children()[1]).toContainText("Sandbox");
    expect(headers.children()[2]).toContainText("OS");
    expect(headers.children()[3]).toContainText("IP Address");
    expect(headers.children()[4]).toContainText("Status");
    expect(headers.children()[5]).toContainText("Free Space");
    expect(headers.children()[6]).toContainText("Resources");
    expect(headers.children()[7]).toContainText("Environments");
  });

  it("should render agents in the table", () => {
    const agents    = agentsJSON();
    const tableBody = helper.findByDataTestId("table-body");
    expect(tableBody.children()).toHaveLength(2);

    const agent1     = agents._embedded.agents[0];
    const agent2     = agents._embedded.agents[1];
    const agentUUID1 = agent1.uuid;
    const agentUUID2 = agent2.uuid;

    expect(helper.findByDataTestId(`agent-hostname-of-${agentUUID2}`)).toContainText(agent2.hostname);
    expect(helper.findByDataTestId(`agent-sandbox-of-${agentUUID2}`)).toContainText(agent2.sandbox);
    expect(helper.findByDataTestId(`agent-operating-system-of-${agentUUID2}`)).toContainText(agent2.operating_system);
    expect(helper.findByDataTestId(`agent-ip-address-of-${agentUUID2}`)).toContainText(agent2.ip_address);
    expect(helper.findByDataTestId(`agent-free-space-of-${agentUUID2}`)).toContainText(agent2.free_space.toString());
    expect(helper.findByDataTestId(`agent-resources-of-${agentUUID2}`)).toContainText(agent2.resources.join(", "));

    expect(helper.findByDataTestId(`agent-hostname-of-${agentUUID1}`)).toContainText(agent1.hostname);
    expect(helper.findByDataTestId(`agent-sandbox-of-${agentUUID1}`)).toContainText(agent1.sandbox);
    expect(helper.findByDataTestId(`agent-operating-system-of-${agentUUID1}`)).toContainText(agent1.operating_system);
    expect(helper.findByDataTestId(`agent-ip-address-of-${agentUUID1}`)).toContainText(agent1.ip_address);
    expect(helper.findByDataTestId(`agent-free-space-of-${agentUUID1}`)).toContainText(agent1.free_space.toString());
    expect(helper.findByDataTestId(`agent-resources-of-${agentUUID1}`)).toContainText(agent1.resources.join(", "));
  });

  function agentsJSON(): AgentsJSON {
    return {
      _embedded: {
        agents: [
          {
            uuid: "uuid1",
            hostname: "windows-10-pro",
            ip_address: "10.1.0.5",
            sandbox: "C:\\go",
            operating_system: "Windows 10",
            // @ts-ignore
            agent_config_state: "Enabled",
            // @ts-ignore
            agent_state: "Idle",
            environments: [{
              name: "gocd",
              origin: {
                type: "gocd",
                _links: {
                  self: {
                    href: "https://build.gocd.org/go/admin/config_xml"
                  },
                  doc: {
                    href: "https://api.gocd.org/19.8.0/#get-configuration"
                  }
                }
              }
            },
              {
                name: "internal",
                origin: {
                  type: "gocd",
                  _links: {
                    self: {
                      href: "https://build.gocd.org/go/admin/config_xml"
                    },
                    doc: {
                      href: "https://api.gocd.org/19.8.0/#get-configuration"
                    }
                  }
                }
              }],
            // @ts-ignore
            build_state: "Idle",
            free_space: 93259825152,
            resources: ["dev", "fat", "ie9", "windows"]
          },
          {
            uuid: "uuid2",
            hostname: "mac-mini",
            ip_address: "10.1.0.10",
            sandbox: "/var/run/",
            operating_system: "Mac OS X",
            // @ts-ignore
            agent_config_state: "Disabled",
            // @ts-ignore
            agent_state: "Idle",
            environments: [{
              name: "gocd",
              origin: {
                type: "gocd",
                _links: {
                  self: {
                    href: "https://build.gocd.org/go/admin/config_xml"
                  },
                  doc: {
                    href: "https://api.gocd.org/19.8.0/#get-configuration"
                  }
                }
              }
            }, {
              name: "internal",
              origin: {
                type: "gocd",
                _links: {
                  self: {
                    href: "https://build.gocd.org/go/admin/config_xml"
                  },
                  doc: {
                    href: "https://api.gocd.org/19.8.0/#get-configuration"
                  }
                }
              }
            }],
            // @ts-ignore
            build_state: "Building",
            // @ts-ignore
            free_space: "unknown",
            resources: ["firefox"]
          }]
      }
    };
  }
});
