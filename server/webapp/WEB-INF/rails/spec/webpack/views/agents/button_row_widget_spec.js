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

describe("Button Row Widget", () => {
  const m      = require('mithril');
  const Stream = require('mithril/stream');

  require('jasmine-jquery');

  const Agents          = require('models/agents/agents');
  const ButtonRowWidget = require("views/agents/button_row_widget");
  const AgentsVM        = require("views/agents/models/agents_widget_view_model");

  let agents;

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const selectedAgents     = () => {
  };
  const disableAgents      = () => {
  };
  const enableAgents       = () => {
  };
  const deleteAgents       = () => {
  };
  const updateResources    = () => {
  };
  const updateEnvironments = () => {
  };

  const agentsVM = new AgentsVM();

  beforeEach(() => {
    agents          = Stream();
    const allAgents = Agents.fromJSON(json());
    agents(allAgents);
    const areOperationsAllowed = Stream(false);
    mount(areOperationsAllowed);
    m.redraw();
  });

  afterEach(() => {
    unmount();
  });

  describe('Heading Row', () => {
    it('should contain the agents page heading text', () => {
      const headingText = $root.find('.page-header h1');
      expect(headingText).toHaveText('Agents');
    });
  });

  describe('Button Group', () => {
    it('should contain the row elements', () => {
      const rowElementButtons = $root.find('.header-panel-button-group button');
      expect(rowElementButtons).toHaveLength(5);
      expect(rowElementButtons[0]).toHaveText("Delete");
      expect(rowElementButtons[1]).toHaveText("Disable");
      expect(rowElementButtons[2]).toHaveText("Enable");
      expect(rowElementButtons[3]).toHaveText("Resources");
      expect(rowElementButtons[4]).toHaveText("Environments");
    });

    it('should disable the buttons if agents are not selected', () => {
      const rowElements = $root.find('.header-panel-button-group button');
      expect(rowElements[0]).toBeDisabled();
      expect(rowElements[1]).toBeDisabled();
      expect(rowElements[2]).toBeDisabled();
      expect(rowElements[3]).toBeDisabled();
      expect(rowElements[4]).toBeDisabled();
    });

    it('should enable the buttons if at least one agent is selected', () => {
      const areOperationsAllowed = Stream(true);
      mount(areOperationsAllowed);
      const rowElements = $root.find('.header-panel-button-group button');

      expect(rowElements[0]).not.toBeDisabled();
      expect(rowElements[1]).not.toBeDisabled();
      expect(rowElements[2]).not.toBeDisabled();
      expect(rowElements[3]).not.toBeDisabled();
      expect(rowElements[5]).not.toBeDisabled();
    });

  });

  const mount = (areOperationsAllowed) => {
    m.mount(root, {
      view() {
        return m(ButtonRowWidget, {
          areOperationsAllowed,
          dropdown:             agentsVM.dropdown,
          selectedAgents,
          onDisable:            disableAgents,
          onEnable:             enableAgents,
          onDelete:             deleteAgents,
          onResourcesUpdate:    updateResources,
          onEnvironmentsUpdate: updateEnvironments
        });
      }
    });
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
          "href": "https://ci.example.com/go/api/agents/uuid-1"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
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
});
