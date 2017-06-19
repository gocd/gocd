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

describe("Agents Widget", () => {

  const $         = require("jquery");
  const m         = require('mithril');
  const Stream    = require('mithril/stream');
  const _         = require('lodash');
  const Routes    = require('gen/js-routes');
  const SortOrder = require('views/agents/models/sort_order');

  const simulateEvent = require('simulate-event');

  require('jasmine-jquery');
  require('jasmine-ajax');

  const Agents       = require('models/agents/agents');
  const AgentsWidget = require("views/agents/agents_widget");
  const AgentsVM     = require("views/agents/models/agents_widget_view_model");

  let $root, root;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  const showSpinner      = Stream();
  const permanentMessage = Stream({});

  let agentsVM;
  let agents;
  let sortOrder;

  beforeEach(() => {
    sortOrder           = Stream(new SortOrder());
    sortOrder().perform = _.noop;
  });

  const route = (isUserAdmin) => {
    m.route(root, '', {
      '':                  {
        view() {
          return m(AgentsWidget, {
            vm:                   agentsVM,
            allAgents:            agents,
            isUserAdmin,
            showSpinner,
            permanentMessage,
            sortOrder,
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
            sortOrder,
            doCancelPolling:      _.noop,
            doRefreshImmediately: _.noop
          });
        }
      }
    });
    m.route.set('');
    m.redraw();
  };

  const unmount = () => {
    m.route.set('');
    m.mount(root, null);
    m.redraw();
  };

  beforeEach(() => {
    agentsVM = new AgentsVM();
    agents   = Stream(Agents.fromJSON(allAgentsJSON));
    agentsVM.initializeWith(agents());

    route(true);
  });

  afterEach(() => {
    unmount();
  });

  it('should contain the agent rows equal to the number of agents', () => {
    const agentRows = $root.find('table tbody tr');
    expect(agentRows).toHaveLength(2);
  });

  it('should contain the agent row information', () => {
    const agentInfo      = $root.find('table tbody tr')[0];
    const firstAgentInfo = $(agentInfo).find('td');
    expect(firstAgentInfo).toHaveLength(10);
    expect($(firstAgentInfo[0]).find(':checkbox')).toExist();
    expect($(firstAgentInfo[2]).find('.content')).toHaveText('host-1');
    expect($(firstAgentInfo[3]).find('.content')).toHaveText('usr/local/foo');
    expect($(firstAgentInfo[4]).find('.content')).toHaveText('Linux');
    expect($(firstAgentInfo[5]).find('.content')).toHaveText('10.12.2.200');
    expect($(firstAgentInfo[6]).find('.content')).toContainText('Disabled (Building)');
    expect($(firstAgentInfo[7]).find('.content')).toHaveText('Unknown');
    expect($(firstAgentInfo[8]).find('.content')).toHaveText('Firefox');
    expect($(firstAgentInfo[9]).find('.content')).toHaveText('Dev, Test');
  });

  it('should select all the agents when selectAll checkbox is checked', () => {
    const allBoxes          = $root.find('tbody :checkbox');
    const selectAllCheckbox = $root.find('thead :checkbox');

    expect(selectAllCheckbox[0]).not.toBeChecked();
    expect(allBoxes[0]).not.toBeChecked();
    expect(allBoxes[1]).not.toBeChecked();

    $(selectAllCheckbox).click();
    m.redraw();

    expect(allBoxes[0]).toBeChecked();
    expect(allBoxes[1]).toBeChecked();

  });

  it('should check select all checkbox on selecting all the checkboxes', () => {
    const allBoxes          = $root.find('tbody :checkbox');
    const selectAllCheckbox = $root.find('thead :checkbox');

    expect(selectAllCheckbox[0]).not.toBeChecked();
    expect(allBoxes[0]).not.toBeChecked();

    $(allBoxes[0]).click();
    $(allBoxes[1]).click();
    m.redraw();

    expect(selectAllCheckbox[0]).toBeChecked();
  });

  it('should hide all dropdown on click of the body', () => {
    clickAllAgents();

    jasmine.Ajax.withMock(() => {
      stubResourcesList();
      const resourceButton = $root.find("button:contains('Resources')");
      resourceButton.click();
      m.redraw();

      expect($(resourceButton).parent().attr('class')).toContain('is-open');

      const body = $root.find('.search-panel');
      $(body).click();
      m.redraw(true);

      expect($(resourceButton).parent().attr('class')).not.toContain('is-open');
    });
  });

  it('should not hide dropdown on click of dropdown list', () => {
    clickAllAgents();

    jasmine.Ajax.withMock(() => {
      stubResourcesList();

      const resourceButton = $root.find("button:contains('Resources')");
      simulateEvent.simulate(resourceButton.get(0), 'click');
      m.redraw();
      expect(resourceButton.parent()).toHaveClass('is-open');
      simulateEvent.simulate($root.find('.resource-dropdown').get(0), 'click');
      m.redraw();
      expect(resourceButton.parent()).toHaveClass('is-open');
    });
  });

  it('should not allow operations when user is non-admin', () => {
    unmount();
    route(false);
    clickAllAgents();
    const sampleButton = $root.find("button:contains('Resources')");
    expect(sampleButton).toBeDisabled();
  });

  it('should show message after disabling the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();

      allowBulkUpdate('PATCH');
      let message = $root.find('.callout');
      expect(message).toHaveLength(0);
      simulateEvent.simulate($root.find("button:contains('Disable')").get(0), 'click');
      m.redraw();
      message = $root.find('.callout');
      expect(message).toHaveText('Disabled 2 agents');
    });
  });


  it('should show message after enabling the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('PATCH');

      let message = $root.find('.callout');
      expect(message).toHaveLength(0);
      simulateEvent.simulate($root.find("button:contains('Enable')").get(0), 'click');
      m.redraw();
      message = $root.find('.callout');
      expect(message).toHaveText('Enabled 2 agents');
    });
  });

  it('should show message after deleting the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('DELETE');

      let message = $root.find('.callout');
      expect(message).toHaveLength(0);
      simulateEvent.simulate($root.find("button:contains('Delete')").get(0), 'click');
      m.redraw();
      message = $root.find('.callout');
      expect(message).toHaveText('Deleted 2 agents');
    });
  });

  it('should show message after updating resource of the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('PATCH');
      stubResourcesList();

      let message = $root.find('.callout');
      expect(message).toHaveLength(0);

      simulateEvent.simulate($root.find("button:contains('Resources')").get(0), 'click');
      m.redraw();

      simulateEvent.simulate($root.find(".add-resource button:contains('Apply')").get(0), 'click');
      m.redraw();

      message = $root.find('.callout');
      expect(message).toHaveText('Resources modified on 2 agents');
    });
  });

  it('should show message after updating environment of the agents', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      allowBulkUpdate('PATCH');
      stubEnvironmentsList();

      let message = $root.find('.callout');
      expect(message).toHaveLength(0);

      simulateEvent.simulate($root.find("button:contains('Environments')").get(0), 'click');
      m.redraw();

      simulateEvent.simulate($root.find(".env-dropdown button:contains('Apply')").get(0), 'click');
      m.redraw();

      message = $root.find('.callout');
      expect(message).toHaveText('Environments modified on 2 agents');
    });
  });

  it('should show only filtered agents after inserting filter text', () => {
    const searchField       = $root.find('#filter-agent').get(0);
    let agentsCountOnPage = $root.find('.agents-table-body .agents-table tbody tr');
    expect(agentsCountOnPage).toHaveLength(2);

    $(searchField).val('host-2');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    agentsCountOnPage = $root.find('.agents-table-body .agents-table tbody tr');
    expect(agentsCountOnPage).toHaveLength(1);

    $(searchField).val('host-');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    agentsCountOnPage = $root.find('.agents-table-body .agents-table tbody tr');
    expect(agentsCountOnPage).toHaveLength(2);
  });

  it('should preserve the selection of agents during filter', () => {
    const searchField = $root.find('#filter-agent').get(0);

    $(searchField).val('host-1');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    let allBoxes = $root.find('.agents-table-body .agents-table tbody tr input[type="checkbox"]');
    allBoxes.each((_i, checkbox) => {
      simulateEvent.simulate(checkbox, 'click');
    });

    $(searchField).val('');
    simulateEvent.simulate(searchField, 'input');
    m.redraw();

    allBoxes = $root.find('.agents-table tbody tr input[type="checkbox"]');
    expect(allBoxes).toHaveLength(2);
    expect(allBoxes[0]).toBeChecked();
    expect(allBoxes[1]).not.toBeChecked();
  });

  it('should allow sorting', () => {
    const getHostnamesInTable = () => {
      const hostnameCells = $root.find(".agents-table tbody td:nth-child(3)");

      return hostnameCells.map((_i, cell) => $(cell).find('.content').text()).toArray();
    };

    let agentNameHeader = $root.find("label:contains('Agent Name')");
    simulateEvent.simulate(agentNameHeader.get(0), 'click');
    m.redraw();

    expect(getHostnamesInTable()).toEqual(['host-1', 'host-2']);

    agentNameHeader = $root.find("label:contains('Agent Name')");
    simulateEvent.simulate(agentNameHeader.get(0), 'click');
    m.redraw();

    expect(getHostnamesInTable()).toEqual(['host-2', 'host-1']);

    agentNameHeader = $root.find("label:contains('Agent Name')");
    simulateEvent.simulate(agentNameHeader.get(0), 'click');
    m.redraw();

    expect(getHostnamesInTable()).toEqual(['host-1', 'host-2']);
  });

  it('should toggle the resources list on click of the resources button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubResourcesList();
      const resourceButton = $root.find("button:contains('Resources')");
      let resourcesList  = $root.find('.has-dropdown')[0];
      expect(resourcesList.classList).not.toContain('is-open');

      $(resourceButton).click();
      m.redraw();

      resourcesList = $root.find('.has-dropdown')[0];
      expect(resourcesList.classList).toContain('is-open');

      resourceButton.click();
      m.redraw();
      expect(resourcesList.classList).not.toContain('is-open');
    });
  });

  it('should toggle the environments list on click of the environments button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubEnvironmentsList();

      const environmentButton = $root.find("button:contains('Environments')");
      let environmentsList  = $root.find('.has-dropdown')[1];
      expect(environmentsList.classList).not.toContain('is-open');

      $(environmentButton).click();
      m.redraw();
      environmentsList = $root.find('.has-dropdown')[1];
      expect(environmentsList.classList).toContain('is-open');

      environmentButton.click();
      m.redraw();
      expect(environmentsList.classList).not.toContain('is-open');
    });
  });

  it('should hide the resources list on click of the environments button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubResourcesList();
      stubEnvironmentsList();
      const environmentButton = $root.find("button:contains('Environments')");
      const resourcesButton   = $root.find("button:contains('Resources')");
      const resourcesDropdown = $root.find("button:contains('Resources')").parent()[0];

      resourcesButton.click();
      m.redraw();

      expect(resourcesDropdown.classList).toContain('is-open');

      environmentButton.click();
      m.redraw();

      expect(resourcesDropdown.classList).not.toContain('is-open');

      hideAllDropDowns();
    });
  });

  it('should hide the environment list on click of the resource button', () => {
    jasmine.Ajax.withMock(() => {
      clickAllAgents();
      stubResourcesList();
      stubEnvironmentsList();

      const environmentButton    = $root.find("button:contains('Environments')");
      const resourcesButton      = $root.find("button:contains('Resources')");
      const environmentsDropdown = $root.find("button:contains('Environments')").parent()[0];

      environmentButton.click();
      m.redraw();

      expect(environmentsDropdown.classList).toContain('is-open');

      resourcesButton.click();
      m.redraw();

      expect(environmentsDropdown.classList).not.toContain('is-open');

      hideAllDropDowns();
    });
  });

  it('should show build details dropdown for building agent', () => {
    let buildingAgentStatus = $root.find(".agents-table tbody td:nth-child(7)")[0];
    expect(buildingAgentStatus).not.toHaveClass('is-open');
    const buildingDetailsLink = $(buildingAgentStatus).find('.has-build-details-drop-down')[0];

    $(buildingDetailsLink).click();
    m.redraw();

    buildingAgentStatus = $root.find(".agents-table tbody td:nth-child(7)")[0];
    expect(buildingAgentStatus).toHaveClass('is-open');
  });

  const clickAllAgents = () => {
    const uuids = agents().collectAgentProperty('uuid');
    _.forEach(uuids, (uuid) => {
      agentsVM.agents.checkboxFor(uuid)(true);
    });
    m.redraw();
  };

  const hideAllDropDowns = () => {
    agentsVM.dropdown.hideAllDropDowns();
    m.redraw();
  };

  const allowBulkUpdate = (method) => {
    jasmine.Ajax.stubRequest(Routes.apiv4AgentsPath(), null, method).andReturn({
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
      "environments":       [
        "Dev",
        "Test"
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
      "environments":       [
        "Test"
      ]
    }
  ];
  /* eslint-enable camelcase */
});
