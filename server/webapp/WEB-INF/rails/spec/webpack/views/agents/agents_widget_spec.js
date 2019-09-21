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
import {SparkRoutes} from "helpers/spark_routes";
import {TestHelper} from "views/pages/spec/test_helper";
import {VM as AgentsVM} from "views/agents/models/agents_widget_view_model";
import {AgentsWidget} from "views/agents/agents_widget";
import {Agents} from "models/agents/agents";
import {SortOrder} from "views/agents/models/route_handler";
import _ from "lodash";
import Stream from "mithril/stream";
import m from "mithril";
import "jasmine-ajax";
import "jasmine-jquery";

describe("Agents Widget", () => {
  const helper       = new TestHelper();

  const showSpinner      = Stream();
  const permanentMessage = Stream({});

  let agentsVM;
  let agents;
  let routeHandler;

  let shouldShowAnalyticsIcon = false;

  beforeEach(() => {
    routeHandler           = Stream(new SortOrder());
    routeHandler().perform = _.noop;
  });

  const route = (isUserAdmin) => {
    helper.route('/', () => {
      return {
        '/':                  {
          view() {
            return m(AgentsWidget, {
              vm:                   agentsVM,
              allAgents:            agents,
              isUserAdmin,
              showSpinner,
              permanentMessage,
              shouldShowAnalyticsIcon,
              sortOrder:            routeHandler,
              doCancelPolling:      _.noop,
              doRefreshImmediately: _.noop
            });
          }
        },
        '/:sortBy/:orderBy': {
          view() {
            return m(AgentsWidget, {
              vm:                   agentsVM,
              allAgents:            agents,
              isUserAdmin,
              showSpinner,
              permanentMessage,
              shouldShowAnalyticsIcon,
              sortOrder:            routeHandler,
              doCancelPolling:      _.noop,
              doRefreshImmediately: _.noop
            });
          }
        }
      };
    });
    m.route.set('/');
    helper.redraw();
  };

  const unmount = () => {
    helper.unmount();
  };

  beforeEach(() => {
    agentsVM = new AgentsVM(Stream(''));
    agents   = Stream(Agents.fromJSON(allAgentsJSON));
    agentsVM.initializeWith(agents());

    route(true);
  });

  afterEach(() => {
    unmount();
  });

  it('should contain the agent rows equal to the number of agents', () => {
    const agentRows = helper.qa('table tbody tr');
    expect(agentRows).toHaveLength(2);
  });

  it('should contain the agent row information', () => {
    const agentInfo      = helper.q('table tbody tr');
    const firstAgentInfo = helper.qa("td", agentInfo);
    expect(firstAgentInfo).toHaveLength(10);
    expect(helper.q('input[type="checkbox"]', firstAgentInfo[0])).toExist();
    expect(helper.text('.content', firstAgentInfo[2])).toBe('host-1');
    expect(helper.text('.content', firstAgentInfo[3])).toBe('usr/local/foo');
    expect(helper.text('.content', firstAgentInfo[4])).toBe('Linux');
    expect(helper.text('.content', firstAgentInfo[5])).toBe('10.12.2.200');
    expect(helper.text('.content', firstAgentInfo[6])).toContain('Disabled (Building)');
    expect(helper.text('.content', firstAgentInfo[7])).toBe('Unknown');
    expect(helper.text('.content', firstAgentInfo[8])).toBe('Firefox');
    expect(helper.text('.content', firstAgentInfo[9])).toBe('Dev, Test');
  });

  it('should contain the analytics icon row when analytics icon should be shown', () => {
    shouldShowAnalyticsIcon = true;
    helper.redraw();

    expect(helper.qa('th')).toHaveLength(11);
  });

  it('should select all the agents when selectAll checkbox is checked', () => {
    const allBoxes          = helper.qa('tbody input[type="checkbox"]');
    const selectAllCheckbox = helper.q('thead input[type="checkbox"]');

    expect(selectAllCheckbox).not.toBeChecked();
    expect(allBoxes[0]).not.toBeChecked();
    expect(allBoxes[1]).not.toBeChecked();

    helper.click(selectAllCheckbox);

    expect(allBoxes[0]).toBeChecked();
    expect(allBoxes[1]).toBeChecked();
  });

  it('should check select all checkbox on selecting all the checkboxes', () => {
    const allBoxes          = helper.qa('tbody input[type="checkbox"]');
    const selectAllCheckbox = helper.q('thead input[type="checkbox"]');

    expect(selectAllCheckbox).not.toBeChecked();
    expect(allBoxes[0]).not.toBeChecked();

    allBoxes[0].click();
    allBoxes[1].click();
    helper.redraw();

    expect(selectAllCheckbox).toBeChecked();
  });

  it('should hide all dropdown on click of the body', () => {
    clickAllAgents();

    jasmine.Ajax.withMock(() => {
      stubResourcesList();

      const resourceButton = buttonLabeled("Resources");
      helper.click(resourceButton);

      expect(resourceButton.parentElement).toHaveClass("is-open");

      helper.click('.agents-search-panel');

      expect(resourceButton.parentElement).not.toHaveClass("is-open");
    });
  });

  it('should not hide dropdown on click of dropdown list', () => {
    clickAllAgents();

    jasmine.Ajax.withMock(() => {
      stubResourcesList();

      const resourceButton = buttonLabeled("Resources");
      helper.click(resourceButton);

      expect(resourceButton.parentElement).toHaveClass("is-open");
      helper.click('.resource-dropdown');
      expect(resourceButton.parentElement).toHaveClass("is-open");
    });
  });

  it('should not allow operations when user is non-admin', () => {
    unmount();
    route(false);
    clickAllAgents();
    const sampleButton = buttonLabeled("Resources");
    expect(sampleButton).toBeDisabled();
  });

  it('should show message after disabling the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();

      allowBulkUpdate('PATCH');
      expect(helper.q('.callout')).not.toExist();

      helper.click(buttonLabeled("Disable"));

      expect(helper.text('.callout')).toBe('Disabled 2 agents');
    });
  });


  it('should show message after enabling the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('PATCH');

      expect(helper.q('.callout')).not.toExist();

      helper.click(buttonLabeled("Enable"));

      expect(helper.text('.callout')).toBe('Enabled 2 agents');
    });
  });

  it('should show message after deleting the agents', () => {
    unmount();
    route(true);

    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('DELETE');

      expect(helper.q('.callout')).not.toExist();

      helper.click(buttonLabeled("Delete"));

      expect(helper.text('.callout')).toBe('Deleted 2 agents');
    });
  });

  it('should show message after updating resource of the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('PATCH');
      stubResourcesList();

      expect(helper.q('.callout')).not.toExist();

      helper.click(buttonLabeled("Resources"));
      helper.click(buttonLabeled("Apply"));

      expect(helper.text('.callout')).toBe('Resources modified on 2 agents');
    });
  });

  it('should show message after updating environment of the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('PATCH');
      stubEnvironmentsList();

      expect(helper.q('.callout')).not.toExist();

      helper.click(buttonLabeled("Environments"));
      helper.click(buttonLabeled("Apply"));

      expect(helper.text('.callout')).toBe('Environments modified on 2 agents');
    });
  });

  it('should show only filtered agents after inserting filter text', () => {
    const searchField     = helper.q('#filter-agent');
    let agentsCountOnPage = helper.qa('.agents-table-body .agents-table tbody tr');
    expect(agentsCountOnPage).toHaveLength(2);

    helper.oninput(searchField, 'host-2');

    agentsCountOnPage = helper.qa('.agents-table-body .agents-table tbody tr');
    expect(agentsCountOnPage).toHaveLength(1);

    helper.oninput(searchField, 'host-');

    agentsCountOnPage = helper.qa('.agents-table-body .agents-table tbody tr');
    expect(agentsCountOnPage).toHaveLength(2);
  });

  it('should preserve the selection of agents during filter', () => {
    const searchField = helper.q('#filter-agent');

    helper.oninput(searchField, 'host-1');

    let allBoxes = helper.qa('.agents-table-body .agents-table tbody tr input[type="checkbox"]');
    allBoxes.forEach((cb) => helper.click(cb));

    helper.oninput(searchField, '');

    allBoxes = helper.qa('.agents-table tbody tr input[type="checkbox"]');
    expect(allBoxes).toHaveLength(2);
    expect(allBoxes[0]).toBeChecked();
    expect(allBoxes[1]).not.toBeChecked();
  });

  it('should allow sorting', () => {
    const getHostnamesInTable = () => {
      return helper.textAll(".agents-table tbody td:nth-child(3) .content");
    };

    const agentNameHeader = labelCalled("Agent Name");
    helper.click(agentNameHeader);

    expect(getHostnamesInTable()).toEqual(['host-1', 'host-2']);
    helper.click(agentNameHeader);

    expect(getHostnamesInTable()).toEqual(['host-2', 'host-1']);
    helper.click(agentNameHeader);

    expect(getHostnamesInTable()).toEqual(['host-1', 'host-2']);
  });

  it('should toggle the resources list on click of the resources button', () => {
    unmount();
    route(true);

    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubResourcesList();
      const resourceButton = buttonLabeled("Resources");
      const resourcesList    = helper.q('.has-dropdown');
      expect(resourcesList).not.toHaveClass('is-open');

      helper.click(resourceButton);

      expect(resourcesList).toHaveClass('is-open');

      helper.click(resourceButton);

      expect(resourcesList).not.toHaveClass('is-open');
    });
  });

  it('should toggle the environments list on click of the environments button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubEnvironmentsList();

      const environmentButton = buttonLabeled("Environments");
      const environmentsList  = helper.qa('.has-dropdown')[1];
      expect(environmentsList).not.toHaveClass('is-open');

      helper.click(environmentButton);

      expect(environmentsList).toHaveClass('is-open');

      helper.click(environmentButton);

      expect(environmentsList).not.toHaveClass('is-open');
    });
  });

  it('should hide the resources list on click of the environments button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubResourcesList();
      stubEnvironmentsList();
      const environmentButton = buttonLabeled("Environments");
      const resourcesButton   = buttonLabeled("Resources");
      const resourcesDropdown = resourcesButton.parentElement;

      helper.click(resourcesButton);

      expect(resourcesDropdown).toHaveClass('is-open');

      helper.click(environmentButton);

      expect(resourcesDropdown).not.toHaveClass('is-open');

      hideAllDropDowns();
    });
  });

  it('should hide the environment list on click of the resource button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubResourcesList();
      stubEnvironmentsList();

      const environmentButton    = buttonLabeled("Environments");
      const resourcesButton      = buttonLabeled("Resources");
      const environmentsDropdown = environmentButton.parentElement;

      helper.click(environmentButton);

      expect(environmentsDropdown).toHaveClass('is-open');

      helper.click(resourcesButton);

      expect(environmentsDropdown).not.toHaveClass('is-open');

      hideAllDropDowns();
    });
  });

  it('should show build details dropdown for building agent', () => {
    const buildingAgentStatus = helper.q(".agents-table tbody td:nth-child(7)");
    expect(buildingAgentStatus).not.toHaveClass('is-open');

    helper.click('.has-build-details-drop-down', buildingAgentStatus);

    expect(buildingAgentStatus).toHaveClass('is-open');
  });

  const clickAllAgents = () => {
    const uuids = agents().collectAgentProperty('uuid');
    _.forEach(uuids, (uuid) => {
      agentsVM.agents.checkboxFor(uuid)(true);
    });
    helper.redraw();
  };

  const hideAllDropDowns = () => {
    agentsVM.dropdown.hideAllDropDowns();
    helper.redraw();
  };

  const allowBulkUpdate = (method) => {
    jasmine.Ajax.stubRequest(SparkRoutes.agentsPath(), null, method).andReturn({
      responseText: JSON.stringify({}),
      headers:      {
        'Content-Type': 'application/json'
      },
      status:       200
    });
  };

  const stubResourcesList = () => {
    jasmine.Ajax.stubRequest('/go/api/admin/internal/resources', null, 'GET').andReturn({
      responseText: JSON.stringify(['Linux', 'Gauge', 'Java', 'Windows']),
      headers:      {
        'Content-Type': 'application/json'
      },
      status:       200
    });
  };

  const stubEnvironmentsList = () => {
    jasmine.Ajax.stubRequest('/go/api/admin/internal/environments', null, 'GET').andReturn({
      responseText: JSON.stringify(['Dev', 'Build', 'Testing', 'Deploy']),
      headers:      {
        'Content-Type': 'application/json'
      },
      status:       200
    });
  };

  /* eslint-disable camelcase */
  const allAgentsJSON = [
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
      "hostname":           "host-1",
      "ip_address":         "10.12.2.200",
      "sandbox":            "usr/local/foo",
      "operating_system":   "Linux",
      "free_space":         "unknown",
      "agent_config_state": "Disabled",
      "agent_state":        "Missing",
      "build_state":        "Building",
      "resources":          [
        "Firefox"
      ],
      "environments": [
        {
          "name": "Dev",
          "origin": {
            "type":   "gocd",
            "_links": {
              "self": {
                "href": "http://localhost:8153/go/admin/config_xml"
              },
              "doc":  {
                "href": "https://api.gocd.org/19.2.0/#get-configuration"
              }
            }
          }
        },
        {
          "name": "Test",
          "origin": {
            "type":   "gocd",
            "_links": {
              "self": {
                "href": "http://localhost:8153/go/admin/config_xml"
              },
              "doc":  {
                "href": "https://api.gocd.org/19.2.0/#get-configuration"
              }
            }
          }
        }
      ],
      "build_details":      {
        "_links":   {
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
        "pipeline": "up42",
        "stage":    "up42_stage",
        "job":      "up42_job"
      }
    },
    {
      "_links":             {
        "self": {
          "href": "https://ci.example.com/go/api/agents/dfdbe0b1-aa31-4a52-ac42d-ca0cf6bdaa3e"
        },
        "doc":  {
          "href": "https://api.gocd.org/#agents"
        },
        "find": {
          "href": "https://ci.example.com/go/api/agents/:uuid"
        }
      },
      "uuid":               "dfdbe0b1-aa31-4a52-ac42d-ca0cf6bdaa3e",
      "hostname":           "host-2",
      "ip_address":         "10.12.2.201",
      "sandbox":            "usr/local/bin",
      "operating_system":   "Linux",
      "free_space":         "unknown",
      "agent_config_state": "Disabled",
      "agent_state":        "Missing",
      "build_state":        "Unknown",
      "resources":          [
        "Chrome"
      ],
      "environments": [
        {
          "name": "Test",
          "origin": {
            "type":   "gocd",
            "_links": {
              "self": {
                "href": "http://localhost:8153/go/admin/config_xml"
              },
              "doc":  {
                "href": "https://api.gocd.org/19.2.0/#get-configuration"
              }
            }
          }
        }
      ]
    }
  ];
  /* eslint-enable camelcase */

  function buttonLabeled(name) {
    return Array.from(helper.qa("button")).find((b) => b.textContent === name);
  }

  function labelCalled(name) {
    return Array.from(helper.qa("label")).find((b) => b.textContent === name);
  }
});
