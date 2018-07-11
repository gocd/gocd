/*
 * Copyright 2017 ThoughtWorks, Inc.
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

describe("Agent State Count Widget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');

  require("jasmine-jquery");

  const Agents                = require('models/agents/agents');
  const AgentStateCountWidget = require("views/agents/agent_state_count_widget");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);
  beforeEach(() => {
    const agents    = Stream();
    const allAgents = Agents.fromJSON(json());
    agents(allAgents);
    mount(agents);
  });

  afterEach(() => {
    unmount();
  });

  it('should contain the agents state count information', () => {
    const children = $root.find('.search-summary').children();
    expect(children).toHaveLength(4);
    expect(children[0]).toContainText('Total');
    expect(children[0]).toContainText('1');
  });

  it('should contain the agents Pending count information', () => {
    const children = $root.find('.search-summary').children();
    expect(children[1]).toContainText('Pending');
    expect(children[1]).toContainText('0');
  });

  it('should contain the agents Enabled count information', () => {
    const children = $root.find('.search-summary').children();
    expect(children[2]).toContainText('Enabled');
    expect(children[2]).toContainText('1');
  });

  it('should contain the agents Disabled count information', () => {
    const children = $root.find('.search-summary').children();
    expect(children[3]).toContainText('Disabled');
    expect(children[3]).toContainText('0');
  });

  const mount = (agents) => {
    m.mount(root,
      {
        view() {
          return m(AgentStateCountWidget, {agents});
        }
      }
    );
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const json = () => [
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               "dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e",
      "hostname":           "in-john.local",
      "ip_address":         "10.12.2.200",
      "sandbox":            "",
      "operating_system":   "",
      "free_space":         "unknown",
      "agent_config_state": "Enabled",
      "agent_state":        "Missing",
      "build_state":        "Unknown",
      "resources":          [
        "firefox"
      ],
      "environments":       [
        "Dev"
      ]
    }
  ];

});
