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
import {TestHelper} from "views/pages/spec/test_helper";
import {VM as AgentsVM} from "views/agents/models/agents_widget_view_model";
import {ButtonRowWidget} from "views/agents/button_row_widget";
import {Agents} from "models/agents/agents";
import Stream from "mithril/stream";
import m from "mithril";

describe("Button Row Widget", () => {


  let agents;

  const helper = new TestHelper();

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
    m.redraw.sync();
  });

  afterEach(helper.unmount.bind(helper));

  describe('Heading Row', () => {
    it('should contain the agents page heading text', () => {
      const headingText = helper.text('.page-header h1');
      expect(headingText).toBe('Agents');
    });
  });

  describe('Button Group', () => {
    it('should contain the row elements', () => {
      const rowButtonText = helper.textAll('.header-panel-button-group button');
      expect(rowButtonText).toHaveLength(5);
      expect(rowButtonText[0]).toBe("Delete");
      expect(rowButtonText[1]).toBe("Disable");
      expect(rowButtonText[2]).toBe("Enable");
      expect(rowButtonText[3]).toBe("Resources");
      expect(rowButtonText[4]).toBe("Environments");
    });

    it('should disable the buttons if agents are not selected', () => {
      const rowElements = helper.qa('.header-panel-button-group button');
      expect(rowElements[0]).toBeDisabled();
      expect(rowElements[1]).toBeDisabled();
      expect(rowElements[2]).toBeDisabled();
      expect(rowElements[3]).toBeDisabled();
      expect(rowElements[4]).toBeDisabled();
    });

    it('should enable the buttons if at least one agent is selected', () => {
      const areOperationsAllowed = Stream(true);
      helper.unmount();
      mount(areOperationsAllowed);
      const rowElements = helper.qa('.header-panel-button-group button');

      expect(rowElements[0]).not.toBeDisabled();
      expect(rowElements[1]).not.toBeDisabled();
      expect(rowElements[2]).not.toBeDisabled();
      expect(rowElements[3]).not.toBeDisabled();
      expect(rowElements[5]).not.toBeDisabled();
    });

  });

  const mount = (areOperationsAllowed) => {
    helper.mount(() => m(ButtonRowWidget, {
      areOperationsAllowed,
      dropdown:             agentsVM.dropdown,
      selectedAgents,
      onDisable:            disableAgents,
      onEnable:             enableAgents,
      onDelete:             deleteAgents,
      onResourcesUpdate:    updateResources,
      onEnvironmentsUpdate: updateEnvironments
    }));
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
