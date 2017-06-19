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
describe("Agent Row Widget", () => {
  const $      = require("jquery");
  const m      = require('mithril');
  const Stream = require('mithril/stream');
  require('jasmine-jquery');

  const Agents          = require('models/agents/agents');
  const AgentsRowWidget = require("views/agents/agent_row_widget");
  const AgentsVM        = require("views/agents/models/agents_widget_view_model");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const agents   = Stream();
  let allAgents;
  const agentsVM = new AgentsVM();

  beforeEach(() => {
    allAgents = Agents.fromJSON(json());
  });

  afterEach(() => {
    unmount();
  });

  it('should contain the agent information', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().firstAgent(), model, true);

    const row         = $root.find('tr:first');
    const checkbox    = $(row).find('input');
    const information = $(row).find('td');
    expect(information[1]).toExist();
    expect($(information[2]).find('.content')).toHaveText('in-john.local');
    expect($(information[2]).find('.content')).toContainElement('a');
    expect($(information[3]).find('.content')).toHaveText('/var/lib/go-agent');
    expect($(information[4]).find('.content')).toHaveText('Linux');
    expect($(information[5]).find('.content')).toHaveText('10.12.2.200');
    expect($(information[6]).find('.content')).toHaveText('Missing');
    expect($(information[7]).find('.content')).toHaveText('Unknown');
    expect($(information[8]).find('.content')).toHaveText('firefox');
    expect($(information[9]).find('.content')).toHaveText('Dev');
    expect(checkbox).toBeChecked();
  });

  it('should not contain link to job run history for non-admin user', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().firstAgent(), model, false);

    const row         = $root.find('tr')[0];
    const information = $(row).find('td');
    expect($(information[2]).find('.content')).toHaveText('in-john.local');
    expect($(information[2]).find('.content')).not.toContainElement('a');
  });

  it('should check the value based on the checkbox model', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().firstAgent(), model, true);

    const checkbox = $root.find('input')[0];
    expect(checkbox.checked).toBe(model());
  });

  it('should not display checkbox for non-admin user', () => {
    agents(allAgents);
    mount(agents().firstAgent(), model, false);
    expect('tr input').not.toBeInDOM();
  });

  it('should show none specified if agent has no resource', () => {
    agents(allAgents.toJSON()[1]);
    mount(agents(), model, true);
    const row         = $root.find('tr')[0];
    const information = $(row).find('td');
    expect($(information[8]).find('.content')).toHaveText('none specified');
  });

  it('should show none specified if agent has no environment', () => {
    agents(allAgents.toJSON()[1]);
    mount(agents(), model, true);
    const row         = $root.find('tr')[0];
    const information = $(row).find('td');
    expect($(information[9]).find('.content')).toHaveText('none specified');
  });

  it('should set the class based on the status of the agent', () => {
    agents(allAgents);
    mount(agents().firstAgent(), model, true);
    const row = $root.find('tr')[0];
    expect(row.classList).toContain(agents().firstAgent().status().toLowerCase());
  });

  it('should change the checkbox model when checkbox is clicked', () => {
    agents(allAgents);
    const model = Stream(false);
    mount(agents().firstAgent(), model, true);
    const row      = $root.find('tr')[0];
    const checkbox = $(row).find('input');
    expect(model()).toBe(false);
    $(checkbox).click();
    m.redraw();
    expect(model()).toBe(true);
  });

  it('should have links to pipeline, stage and job as a part of build details dropdown', () => {
    agents(allAgents.toJSON()[2]);
    mount(agents(), model, true);
    const buildDetailsLinks = $root.find('.build-details a').map((_i, el) => $(el).attr('href'));
    const buildDetails      = agents().buildDetails();
    expect(buildDetailsLinks).toEqual([buildDetails.pipelineUrl(), buildDetails.stageUrl(), buildDetails.jobUrl()]);
  });

  const mount = (agent, model, isUserAdmin) => {
    m.mount(root, {
      view() {
        return m(AgentsRowWidget, {
          agent,
          'checkBoxModel': model,
          'dropdown':      agentsVM.dropdown,
          isUserAdmin
        });
      }
    });
    m.redraw();
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

  const model = Stream();

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
      "uuid":               'uuid-1',
      "hostname":           "in-john.local",
      "ip_address":         "10.12.2.200",
      "sandbox":            "/var/lib/go-agent",
      "operating_system":   "Linux",
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
    },
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
      "uuid":               'uuid-2',
      "hostname":           "in-john.local",
      "ip_address":         "10.12.2.200",
      "sandbox":            "/var/lib/go-agent",
      "operating_system":   "Linux",
      "free_space":         "unknown",
      "agent_config_state": "Enabled",
      "agent_state":        "Missing",
      "build_state":        "Unknown",
      "resources":          [],
      "environments":       []
    },
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
      "uuid":               'uuid-3',
      "hostname":           "host-3",
      "ip_address":         "10.12.2.201",
      "sandbox":            "/var/lib/go-agent-3",
      "operating_system":   "Linux",
      "free_space":         111902543872,
      "agent_config_state": "Enabled",
      "agent_state":        "Building",
      "build_state":        "Unknown",
      "resources":          [
        "linux", "java"
      ],
      "environments":       [
        "staging", "perf"
      ],
      "build_details":      {
        "_links":        {
          "job":      {
            "href": "http://localhost:8153/go/tab/build/detail/up42/2/up42_stage/1/up42_job"
          },
          "stage":    {
            "href": "http://localhost:8153/go/pipelines/up42/2/up42_stage/1"
          },
          "pipeline": {
            "href": "http://localhost:8153/go/tab/pipeline/history/up42"
          }
        },
        "pipeline_name": "up42",
        "stage_name":    "up42_stage",
        "job_name":      "up42_job"
      }
    }
  ];
});
