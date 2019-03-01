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
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";

describe("Agent Row Widget", () => {
  const $      = require("jquery");
  const m      = require('mithril');
  const Stream = require('mithril/stream');
  const Modal  = require('views/shared/new_modal');
  require('jasmine-jquery');

  const PluginInfos = require('models/shared/plugin_infos');

  const Agents          = require('models/agents/agents');
  const AgentsRowWidget = require("views/agents/agent_row_widget");
  const AgentsVM        = require("views/agents/models/agents_widget_view_model");
  const agents          = Stream();
  const agentsVM        = new AgentsVM();

  let shouldShowAnalyticsIcon = true;
  let elasticAgentPluginInfo;
  let allAgents;
  const helper                = new TestHelper();

  beforeEach(() => {
    shouldShowAnalyticsIcon = true;
    elasticAgentPluginInfo  = getElasticAgentPluginInfo();
  });

  beforeEach(() => {
    jasmine.Ajax.install();
    allAgents = Agents.fromJSON(json());
  });

  afterEach(() => {
    jasmine.Ajax.uninstall();
    Modal.destroyAll();
    helper.unmount();
  });

  it('should contain the agent information', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().firstAgent(), model, true);

    const row         = helper.find('tr:first');
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

    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    expect($(information[2]).find('.content')).toHaveText('in-john.local');
    expect($(information[2]).find('.content')).not.toContainElement('a');
  });

  it('should contain link to job run history for normal agents', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().firstAgent(), model, true);

    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    const hostname    = $(information[2]).find('.content');

    expect(hostname).toHaveText('in-john.local');
    expect(hostname.find('a')[0].href).toContain(`/go/agents/${allAgents.firstAgent().uuid()}/job_run_history`);
  });

  it('should contain link to agent status report page for elastic agents', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().lastAgent(), model, true, true);

    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    const hostname    = $(information[2]).find('.content');

    expect(hostname).toHaveText('elastic-agent-hostname');
    expect(hostname.find('a')[0].href).toContain(`/go/admin/status_reports/${allAgents.lastAgent().elasticPluginId()}/${allAgents.lastAgent().elasticAgentId()}`);
  });

  it('should contain link to job run history page for elastic agents when elastic agent plugin is missing', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().lastAgent(), model, true, null);

    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    const hostname    = $(information[2]).find('.content');

    expect(hostname).toHaveText('elastic-agent-hostname');
    expect(hostname.find('a')[0].href).toContain(`/go/agents/${allAgents.lastAgent().uuid()}/job_run_history`);
  });


  it('should not contain link to agent status report page when plugin doesnt support status reports', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().lastAgent(), model, true);

    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    const hostname    = $(information[2]).find('.content');

    expect(hostname).toHaveText('elastic-agent-hostname');
    expect(hostname.find('a')[0].href).toContain(`/go/agents/${allAgents.lastAgent().uuid()}/job_run_history`);
  });

  it('should check the value based on the checkbox model', () => {
    agents(allAgents);
    const model = Stream(true);
    mount(agents().firstAgent(), model, true);

    const checkbox = helper.find('input')[0];
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
    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    expect($(information[8]).find('.content')).toHaveText('none specified');
  });

  it('should show none specified if agent has no environment', () => {
    agents(allAgents.toJSON()[1]);
    mount(agents(), model, true);
    const row         = helper.find('tr')[0];
    const information = $(row).find('td');
    expect($(information[9]).find('.content')).toHaveText('none specified');
  });

  it('should set the class based on the status of the agent', () => {
    agents(allAgents);
    mount(agents().firstAgent(), model, true);
    const row = helper.find('tr')[0];
    expect(row.classList).toContain(agents().firstAgent().status().toLowerCase());
  });

  it('should change the checkbox model when checkbox is clicked', () => {
    agents(allAgents);
    const model = Stream(false);
    mount(agents().firstAgent(), model, true);
    const row      = helper.find('tr')[0];
    const checkbox = $(row).find('input');
    expect(model()).toBe(false);
    $(checkbox).click();
    m.redraw();
    expect(model()).toBe(true);
  });

  it('should have links to pipeline, stage and job as a part of build details dropdown', () => {
    agents(allAgents.toJSON()[2]);
    mount(agents(), model, true);
    const buildDetailsLinks = helper.find('.build-details a').map((_i, el) => $(el).attr('href'));
    const buildDetails      = agents().buildDetails();
    expect(buildDetailsLinks).toEqual([buildDetails.pipelineUrl(), buildDetails.stageUrl(), buildDetails.jobUrl()]);
  });

  it('should not render analytics plugin icon if no analytics plugin supports agent metric', () => {
    agents(allAgents);
    mount(agents().firstAgent(), model, true, false);
    expect(helper.find('.agent-analytics')).not.toBeInDOM();
  });

  it('should render analytics plugin icon if any analytics plugin supports agent metric', () => {
    agents(allAgents);
    elasticAgentPluginInfo.extensions.push(getAnalyticsExtension());
    mount(agents().firstAgent(), model, true, true);
    expect(helper.find('.agent-analytics')).toBeInDOM();
  });

  it('should render analytics for given agent on clicking analytics icon', () => {
    agents(allAgents.toJSON()[2]);

    elasticAgentPluginInfo.extensions.push(getAnalyticsExtension());

    mount(agents(), model, true, false);
    expect(helper.find('.agent-analytics')).toBeInDOM();

    $('.agent-analytics').click();
    m.redraw();

    expect($('.new-modal-container')).toBeInDOM();
    expect($('.modal-title')).toContainText(`Analytics for agent: host-3`);
  });


  it('should not render analytics plugin icon if analytics icon should not be shown', () => {
    agents(allAgents);
    elasticAgentPluginInfo.extensions.push(getAnalyticsExtension());

    shouldShowAnalyticsIcon = false;

    mount(agents().firstAgent(), model, true);
    expect(helper.find('.agent-analytics')).not.toBeInDOM();
  });

  const mount = (agent, model, isUserAdmin, supportsAgentStatusReportPage = false) => {
    elasticAgentPluginInfo.extensions[0]['capabilities']['supports_agent_status_report'] = supportsAgentStatusReportPage;

    const pluginInfos = PluginInfos.fromJSON([elasticAgentPluginInfo]);
    helper.mount(() => m(AgentsRowWidget, {
      agent,
      'checkBoxModel': model,
      'dropdown':      agentsVM.dropdown,
      isUserAdmin,
      shouldShowAnalyticsIcon,
      'pluginInfos':   () => pluginInfos
    }));
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
            "href": "https://ci.example.com/go/tab/build/detail/up42/2/up42_stage/1/up42_job"
          },
          "stage":    {
            "href": "https://ci.example.com/go/pipelines/up42/2/up42_stage/1"
          },
          "pipeline": {
            "href": "https://ci.example.com/go/tab/pipeline/history/up42"
          }
        },
        "pipeline_name": "up42",
        "stage_name":    "up42_stage",
        "job_name":      "up42_job"
      }
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/96934a52-035e-4e79-acbf-da2238091edd"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               "agent-4",
      "hostname":           "elastic-agent-hostname",
      "ip_address":         "172.17.0.2",
      "elastic_agent_id":   "elastic-agent-id",
      "elastic_plugin_id":  "cd.go.contrib.elasticagent.kubernetes",
      "sandbox":            "/go",
      "operating_system":   "Alpine Linux v3.5",
      "free_space":         14496362496,
      "agent_config_state": "Enabled",
      "agent_state":        "Building",
      "environments":       [],
      "build_state":        "Building",
      "build_details":      {
        "_links":        {
          "job":      {
            "href": "https://ci.example.com/go/tab/build/detail/up42/5/up42_stage/1/up42_job"
          },
          "stage":    {
            "href": "https://ci.example.com/go/pipelines/up42/5/up42_stage/1"
          },
          "pipeline": {
            "href": "https://ci.example.com/go/tab/pipeline/history/up42"
          }
        },
        "pipeline_name": "up42",
        "stage_name":    "up42_stage",
        "job_name":      "up42_job"
      }
    }
  ];

  const getElasticAgentPluginInfo = () => ({
    "id":         "cd.go.contrib.elasticagent.kubernetes",
    "status":     {
      "state": "active"
    },
    "about":      {
      "name":                     "Docker Elastic Agent Plugin",
      "version":                  "0.6.1",
      "target_go_version":        "16.12.0",
      "description":              "Docker Based Elastic Agent Plugins for GoCD",
      "target_operating_systems": [],
      "vendor":                   {
        "name": "GoCD Contributors",
        "url":  "https://github.com/gocd-contrib/docker-elastic-agents"
      }
    },
    "extensions": [
      {
        "type":             "elastic-agent",
        "plugin_settings":  {
          "configurations": [
            {
              "key":      "instance_type",
              "metadata": {
                "secure":   false,
                "required": true
              }
            }
          ],
          "view":           {
            "template": "elastic agent plugin settings view"
          }
        },
        "profile_settings": {
          "configurations": [],
          "view":           {
            "template": 'some cool template!'
          }
        },
        "capabilities":     {
          "supports_status_report":       true,
          "supports_agent_status_report": true
        }
      }
    ]
  });

  const getAnalyticsExtension = () => ({
    "type":             "analytics",
    "plugin_settings":  {
      "configurations": [
        {
          "key":      "instance_type",
          "metadata": {
            "secure":   false,
            "required": true
          }
        }
      ],
      "view":           {
        "template": "analytics plugin settings view"
      }
    },
    "profile_settings": {
      "configurations": [],
      "view":           {
        "template": 'some cool template!'
      }
    },
    "capabilities":     {
      "supported_analytics": [
        {
          "type":  "agent",
          "id":    "agent_utilization",
          "title": "Agent Utilization"
        }
      ]
    }

  });
});
