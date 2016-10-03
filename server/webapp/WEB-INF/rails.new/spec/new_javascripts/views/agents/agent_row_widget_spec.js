/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(["jquery", "mithril", 'models/agents/agents', "views/agents/agent_row_widget", "views/agents/models/agents_widget_view_model"], function ($, m, Agents, AgentsRowWidget, AgentsVM) {
  describe("Agent Row Widget", function () {
    var $root    = $('#mithril-mount-point'), root = $root.get(0);
    var agents   = m.prop();
    var allAgents;
    var agentsVM = new AgentsVM();

    beforeAll(function () {
      allAgents = Agents.fromJSON(json());
    });

    afterEach(function () {
      unmount();
    });

    it('should contain the agent information', function () {
      agents(allAgents);
      var model = m.prop(true);
      mount(agents().firstAgent(), model);

      var row         = $root.find('tr')[0];
      var checkbox    = $(row).find('input');
      var information = row.children;
      expect(information[1]).toHaveText('in-john.local');
      expect(information[2]).toHaveText('/var/lib/go-agent');
      expect(information[3]).toHaveText('Linux');
      expect(information[4]).toHaveText('10.12.2.200');
      expect(information[5]).toHaveText('Missing');
      expect(information[6]).toHaveText('Unknown');
      expect(information[7]).toHaveText('firefox');
      expect(information[8]).toHaveText('Dev');
      expect(checkbox).toBeChecked();
    });

    it('should check the value based on the checkbox model', function () {
      agents(allAgents);
      var model = m.prop(true);
      mount(agents().firstAgent(), model);

      var checkbox = $root.find('input')[0];
      expect(checkbox.checked).toBe(model());
    });

    it('should show none specified if agent has no resource', function () {
      agents(allAgents.toJSON()[1]);
      mount(agents(), model);
      var row         = $root.find('tr')[0];
      var information = row.children;
      expect(information[7]).toHaveText('none specified');
    });

    it('should show none specified if agent has no environment', function () {
      agents(allAgents.toJSON()[1]);
      mount(agents(), model);
      var row         = $root.find('tr')[0];
      var information = row.children;
      expect(information[8]).toHaveText('none specified');
    });

    it('should set the class based on the status of the agent', function () {
      agents(allAgents);
      mount(agents().firstAgent(), model);
      var row = $root.find('tr')[0];
      expect(row.classList).toContain(agents().firstAgent().status().toLowerCase());
    });

    it('should change the checkbox model when checkbox is clicked', function () {
      agents(allAgents);
      var model = m.prop(false);
      mount(agents().firstAgent(), model);
      var row      = $root.find('tr')[0];
      var checkbox = $(row).find('input');
      expect(model()).toBe(false);
      $(checkbox).click();
      m.redraw(true);
      expect(model()).toBe(true);
    });

    it('should have links to pipeline, stage and job as a part of build details dropdown', function () {
      agents(allAgents.toJSON()[2]);
      mount(agents(), model);
      var buildDetailsLinks = $root.find('.build-details a').map(function (i, el) {
        return $(el).attr('href');
      });
      var buildDetails      = agents().buildDetails();
      expect(buildDetailsLinks).toEqual([buildDetails.pipelineUrl(), buildDetails.stageUrl(), buildDetails.jobUrl()]);
    });

    var mount = function (agent, model) {
      m.mount(root,
        m.component(AgentsRowWidget,
          {
            'agent':         agent,
            'checkBoxModel': model,
            'dropdown':      agentsVM.dropdown
          })
      );
      m.redraw(true);
    };

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };

    var model = m.prop();

    var json = function () {
      return [
        {
          "_links":             {
            "self": {
              "href": "https://ci.example.com/go/api/agents/dfdbe0b1-4521-4a52-ac2f-ca0cf6bdaa3e"
            },
            "doc":  {
              "href": "http://api.go.cd/#agents"
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
              "href": "http://api.go.cd/#agents"
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
              "href": "http://api.go.cd/#agents"
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
    };
  });
});
