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

define(["jquery", "mithril", "lodash", 'models/agents/agents', "views/agents/button_row_widget", "views/agents/models/agents_widget_view_model"], function ($, m, _, Agents, ButtonRowWidget, AgentsVM) {
  describe("Button Row Widget", function () {

    var agents;

    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var selectedAgents     = function () {
    };
    var disableAgents      = function () {
    };
    var enableAgents       = function () {
    };
    var deleteAgents       = function () {
    };
    var updateResources    = function () {
    };
    var updateEnvironments = function () {
    };

    var agentsVM = new AgentsVM();

    beforeAll(function () {
      agents        = m.prop();
      var allAgents = Agents.fromJSON(json());
      agents(allAgents);
      var isAnyAgentSelected = m.prop(false);
      mount(isAnyAgentSelected);
    });

    afterAll(function () {
      unmount();
    });

    beforeEach(function () {
      m.redraw(true);
    });

    describe('Heading Row', function () {
      it('should contain the agents page heading text', function () {
        var headingText = $root.find('.page-header h1');
        expect(headingText).toHaveText('Agents');
      });
    });

    describe('Button Group', function () {
      it('should contain the row elements', function () {
        var rowElements = $root.find('.agent-button-group button');
        expect(rowElements).toHaveLength(8);
        expect(rowElements[0]).toHaveText("Delete");
        expect(rowElements[1]).toHaveText("Disable");
        expect(rowElements[2]).toHaveText("Enable");
        expect(rowElements[3]).toHaveText("Resources");
        expect(rowElements[4]).toHaveText("Add");
        expect(rowElements[5]).toHaveText("Apply");
        expect(rowElements[6]).toHaveText("Environments");
        expect(rowElements[7]).toHaveText("Apply");
      });

      it('should disable the buttons if agents are not selected', function () {
        var rowElements = $root.find('.agent-button-group button');
        expect(rowElements[0]).toBeDisabled();
        expect(rowElements[1]).toBeDisabled();
        expect(rowElements[2]).toBeDisabled();
        expect(rowElements[3]).toBeDisabled();
        expect(rowElements[6]).toBeDisabled();
      });

      it('should enable the buttons if at least one agent is selected', function () {
        var isAnyAgentSelected = m.prop(true);
        mount(isAnyAgentSelected);
        var rowElements = $root.find('.agent-button-group button');

        expect(rowElements[0]).not.toBeDisabled();
        expect(rowElements[1]).not.toBeDisabled();
        expect(rowElements[2]).not.toBeDisabled();
        expect(rowElements[3]).not.toBeDisabled();
        expect(rowElements[5]).not.toBeDisabled();
      });

    });

    var mount = function (isAnyAgentSelected) {
      m.mount(root,
        m.component(ButtonRowWidget,
          {
            isAnyAgentSelected:   isAnyAgentSelected,
            dropdown:             agentsVM.dropdown,
            selectedAgents:       selectedAgents,
            onDisable:            disableAgents,
            onEnable:             enableAgents,
            onDelete:             deleteAgents,
            onResourcesUpdate:    updateResources,
            onEnvironmentsUpdate: updateEnvironments
          })
      );
      m.redraw(true);
    };

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };
    
    var json = function () {
      return [
        {
          "_links":             {
            "self": {
              "href": "https://ci.example.com/go/api/agents/uuid-1"
            },
            "doc":  {
              "href": "http://api.go.cd/#agents"
            },
            "find": {
              "href": "https://ci.example.com/go/api/agents/:uuid"
            }
          },
          "uuid":               "uuid-1",
          "hostname":           "in-john.local",
          "ip_address":         "10.12.2.200",
          "sandbox":            "usr/local/foo",
          "operating_system":   "Linux",
          "free_space":         "unknown",
          "agent_config_state": "Enabled",
          "agent_state":        "Missing",
          "build_state":        "Unknown",
          "resources":          [
            "Firefox"
          ],
          "environments":       []
        }
      ];
    };
  });
});
