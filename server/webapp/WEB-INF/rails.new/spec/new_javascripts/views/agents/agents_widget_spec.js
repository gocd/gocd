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

define(["jquery", "mithril", 'lodash', 'models/agents/agents', "views/agents/agents_widget", "views/agents/models/agents_widget_view_model"], function ($, m, _, Agents, AgentsWidget, AgentsVM) {
  describe("Agents Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var agentsVM;
    var agents;

    var route = function () {
      m.route.mode = "hash";

      m.route(root, '',
        {
          '':                  m.component(AgentsWidget, {vm: agentsVM, allAgents: agents}),
          '/:sortBy/:orderBy': m.component(AgentsWidget, {vm: agentsVM, allAgents: agents})
        }
      );
      m.route('');
      m.redraw(true);
    };

    var unmount = function () {
      m.route('');
      m.route.mode = "search";
      m.mount(root, null);
      m.redraw(true);
    };

    beforeEach(function () {
      agentsVM = new AgentsVM();
      agents   = m.prop(new Agents());

      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({
        "responseText": JSON.stringify(agentsData),
        "status":       200
      });
      jasmine.Ajax.stubRequest(/\/api\/admin\/internal\/resources/).andReturn({
        "responseText": JSON.stringify(['Linux', 'Gauge', 'Java', 'Windows']),
        "status":       200
      });
      jasmine.Ajax.stubRequest(/\/api\/admin\/internal\/environments/).andReturn({
        "responseText": JSON.stringify(['Dev', 'Build', 'Testing', 'Deploy']),
        "status":       200
      });
      route();
    });

    afterEach(function () {
      unmount();
      jasmine.Ajax.uninstall();
    });

    it('should contain the agent rows equal to the number of agents', function () {
      var agentRows = $root.find('table tbody tr');
      expect(agentRows).toHaveLength(2);
    });

    it('should contain the agent row information', function () {
      var agentInfo      = $root.find('table tbody tr')[0];
      var firstAgentInfo = $(agentInfo).find('td');
      expect(firstAgentInfo).toHaveLength(9);
      expect(firstAgentInfo[0]).toHaveHtml('<input type="checkbox">');
      expect(firstAgentInfo[1]).toHaveText('host-1');
      expect(firstAgentInfo[2]).toHaveText('usr/local/foo');
      expect(firstAgentInfo[3]).toHaveText('Linux');
      expect(firstAgentInfo[4]).toHaveText('10.12.2.200');
      expect(firstAgentInfo[5]).toContainText('Disabled (Building)');
      expect(firstAgentInfo[6]).toHaveText('Unknown');
      expect(firstAgentInfo[7]).toHaveText('Firefox');
      expect(firstAgentInfo[8]).toHaveText('Dev, Test');
    });

    it('should select all the agents when selectAll checkbox is checked', function () {
      var allBoxes          = $root.find('tbody :checkbox');
      var selectAllCheckbox = $root.find('thead :checkbox');

      expect(selectAllCheckbox[0]).not.toBeChecked();
      expect(allBoxes[0]).not.toBeChecked();
      expect(allBoxes[1]).not.toBeChecked();

      $(selectAllCheckbox).click();
      m.redraw(true);

      expect(allBoxes[0]).toBeChecked();
      expect(allBoxes[1]).toBeChecked();

    });

    it('should check select all checkbox on selecting all the checkboxes', function () {
      var allBoxes          = $root.find('tbody :checkbox');
      var selectAllCheckbox = $root.find('thead :checkbox');

      expect(selectAllCheckbox[0]).not.toBeChecked();
      expect(allBoxes[0]).not.toBeChecked();

      $(allBoxes[0]).click();
      $(allBoxes[1]).click();
      m.redraw(true);

      expect(selectAllCheckbox[0]).toBeChecked();
    });

    it('should hide all dropdown on click of the body', function () {
      clickAllAgents();

      var resourceButton = $root.find("button:contains('Resources')");
      resourceButton.click();
      m.redraw(true);

      expect($(resourceButton).parent().attr('class')).toContain('is-open');

      var body = $root.find('.search-panel');
      $(body).click();
      m.redraw(true);

      expect($(resourceButton).parent().attr('class')).not.toContain('is-open');
    });

    it('should not hide dropdown on click of dropdown list', function () {
      clickAllAgents();

      var resourceButton = $root.find("button:contains('Resources')");
      $(resourceButton).click();
      m.redraw(true);

      expect($(resourceButton).parent().attr('class')).toContain('is-open');

      $(resourceButton).parent().click();

      expect($(resourceButton).parent().attr('class')).toContain('is-open');
    });

    it('should show message after disabling the agents', function () {
      clickAllAgents();

      var disableButton = $root.find("button:contains('Disable')");
      var message       = $root.find('.callout');
      expect(message).toHaveLength(0);
      $(disableButton).click();

      message = $root.find('.callout');
      expect(message).toHaveText('Disabled 2 agents');
    });

    it('should show message after enabling the agents', function () {
      clickAllAgents();
      m.redraw(true);

      var enableButton = $root.find("button:contains('Enable')");
      var message      = $root.find('.callout');

      expect(message).toHaveLength(0);
      $(enableButton).click();

      message = $root.find('.callout');
      expect(message).toHaveText('Enabled 2 agents');
    });

    it('should show message after deleting the agents', function () {
      clickAllAgents();
      var deleteButton = $root.find("button:contains('Delete')");

      expect(deleteButton).not.toBeDisabled();
      $(deleteButton).click();

      var message = $root.find('.callout');
      expect(message).toHaveText('Deleted 2 agents');
      expect(deleteButton).toBeDisabled();
    });

    it('should show message after updating resource of the agents', function () {
      clickAllAgents();
      var resourceButton = $root.find("button:contains('Resources')");
      $(resourceButton).click();
      m.redraw(true);

      var resource      = $root.find('.add-resource');
      var applyResource = resource.find('button')[1];

      applyResource.click();

      var message = $root.find('.callout');
      expect(message).toHaveText('Resources modified on 2 agents');
    });

    it('should show message after updating environment of the agents', function () {
      clickAllAgents();
      var environmentButton = $root.find("button:contains('Environments')");
      $(environmentButton).click();
      m.redraw(true);

      var environment      = $root.find('.env-dropdown');
      var applyEnvironment = environment.find('button');

      applyEnvironment.click();

      var message = $root.find('.callout');
      expect(message).toHaveText('Environments modified on 2 agents');
    });

    it('should show only filtered agents after inserting filter text', function () {
      var searchField       = $root.find('#filter-agent')[0];
      var agentsCountOnPage = $root.find('table tbody tr');
      expect(agentsCountOnPage).toHaveLength(2);

      $(searchField).val('host-2').trigger('input');
      m.redraw(true);

      expect($(searchField)).toHaveValue('host-2');
      agentsCountOnPage = $root.find('table tbody tr');
      expect(agentsCountOnPage).toHaveLength(1);
    });

    it('should filter the agents based on filter text value', function () {
      var searchField = $root.find('#filter-agent')[0];
      $(searchField).val('').trigger('input');
      m.redraw(true);

      var agentsCountOnPage = $root.find('table tbody tr');
      expect(agentsCountOnPage).toHaveLength(2);

      $(searchField).val('invalidtextnotvalid').trigger('input');
      m.redraw(true);

      expect($(searchField)).toHaveValue('invalidtextnotvalid');

      agentsCountOnPage = $root.find('table tbody tr');
      expect(agentsCountOnPage).toHaveLength(0);

      $(searchField).val('').trigger('input');
      m.redraw(true);

      expect($(searchField)).toHaveValue('');

      agentsCountOnPage = $root.find('table tbody tr');
      expect(agentsCountOnPage).toHaveLength(2);
    });

    it('should preserve the selection of agents during filter', function () {
      var searchField = $root.find('#filter-agent')[0];

      $(searchField).val('host-1').trigger('input');
      m.redraw(true);

      var allBoxes = $root.find('.go-table tbody tr input[type="checkbox"]');

      expect(allBoxes).toHaveLength(1);
      $(allBoxes[0]).prop('checked', true).trigger('input');
      expect(allBoxes[0]).toBeChecked();

      $(searchField).val('').trigger('input');
      m.redraw(true);

      allBoxes = $root.find('.go-table tbody tr input[type="checkbox"]');
      expect(allBoxes).toHaveLength(2);
      expect(allBoxes[0]).toBeChecked();
      expect(allBoxes[1]).not.toBeChecked();
    });

    it('should sort the agents in ascending order based on hostname', function () {

      var agentNameHeader = $root.find("label:contains('Agent Name')");
      $(agentNameHeader).click();
      m.redraw(true);

      var hostnameCells = $root.find(".go-table tbody td:nth-child(2)");

      var hostNames = hostnameCells.map(function (i, cell) {
        return $(cell).text();
      }).toArray();

      expect(hostNames).toEqual(_.map(allAgentsJSON, 'hostname').sort());
    });

    it('should sort the agents in descending order based on hostname', function () {
      var agentNameHeader = $root.find("label:contains('Agent Name')");
      $(agentNameHeader).click();
      m.redraw(true);
      agentNameHeader = $root.find("label:contains('Agent Name')");
      $(agentNameHeader).click();
      m.redraw(true);

      var hostnameCells = $root.find(".go-table tbody td:nth-child(2)");

      var hostNames = hostnameCells.map(function (i, cell) {
        return $(cell).text();
      }).toArray();

      expect(hostNames).toEqual(_.reverse(_.map(allAgentsJSON, 'hostname').sort()));
    });

    it('should toggle the resources list on click of the resources button', function () {
      clickAllAgents();

      var resourceButton = $root.find("button:contains('Resources')");
      var resourcesList  = $root.find('.has-dropdown')[0];
      expect(resourcesList.classList).not.toContain('is-open');

      $(resourceButton).click();
      m.redraw(true);

      resourcesList = $root.find('.has-dropdown')[0];
      expect(resourcesList.classList).toContain('is-open');

      resourceButton.click();
      m.redraw(true);
      expect(resourcesList.classList).not.toContain('is-open');
    });

    it('should toggle the environments list on click of the environments button', function () {
      clickAllAgents();

      var environmentButton = $root.find("button:contains('Environments')");
      var environmentsList  = $root.find('.has-dropdown')[1];
      expect(environmentsList.classList).not.toContain('is-open');

      $(environmentButton).click();
      m.redraw(true);
      environmentsList = $root.find('.has-dropdown')[1];
      expect(environmentsList.classList).toContain('is-open');

      environmentButton.click();
      m.redraw(true);
      expect(environmentsList.classList).not.toContain('is-open');
    });

    it('should hide the resources list on click of the environments button', function () {
      clickAllAgents();

      var environmentButton = $root.find("button:contains('Environments')");
      var resourcesButton   = $root.find("button:contains('Resources')");
      var resourcesDropdown = $root.find("button:contains('Resources')").parent()[0];

      resourcesButton.click();
      m.redraw(true);

      expect(resourcesDropdown.classList).toContain('is-open');

      environmentButton.click();
      m.redraw(true);

      expect(resourcesDropdown.classList).not.toContain('is-open');

      hideAllDropDowns();
    });

    it('should hide the environment list on click of the resource button', function () {
      clickAllAgents();
      var environmentButton    = $root.find("button:contains('Environments')");
      var resourcesButton      = $root.find("button:contains('Resources')");
      var environmentsDropdown = $root.find("button:contains('Environments')").parent()[0];

      environmentButton.click();
      m.redraw(true);

      expect(environmentsDropdown.classList).toContain('is-open');

      resourcesButton.click();
      m.redraw(true);

      expect(environmentsDropdown.classList).not.toContain('is-open');

      hideAllDropDowns();
    });

    it('should show error message for elastic agent resource update', function () {
      clickAllAgents();
      var resourceButton = $root.find("button:contains('Resources')");
      $(resourceButton).click();
      m.redraw(true);

      var resource      = $root.find('.add-resource');
      var applyResource = resource.find('button')[1];

      mockAjaxCalWithErrorOnUpdatingElasticAgentResource();

      applyResource.click();

      var message = $root.find('.callout');
      expect(message).toHaveText('Resources on elastic agents with uuids [uuid] can not be updated.');
    });

    it('should show build details dropdown for building agent', function () {
      var buildingAgentStatus = $root.find(".agents-table tbody td:nth-child(6)")[0];
      expect(buildingAgentStatus).not.toHaveClass('is-open');
      var buildingDetailsLink = $(buildingAgentStatus).find('.has-build-details-drop-down')[0];

      $(buildingDetailsLink).click();
      m.redraw(true);

      buildingAgentStatus = $root.find(".agents-table tbody td:nth-child(6)")[0];
      expect(buildingAgentStatus).toHaveClass('is-open');
    });

    var clickAllAgents = function () {
      var uuids = agents().collectAgentProperty('uuid');
      _.forEach(uuids, function (uuid) {
        agentsVM.agents.checkboxFor(uuid)(true);
      });
      m.redraw(true);
    };

    var hideAllDropDowns = function () {
      agentsVM.dropdown.hideAllDropDowns();
      m.redraw(true);
    };

    var mockAjaxCalWithErrorOnUpdatingElasticAgentResource = function () {
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({
        "responseText": JSON.stringify({"message": "Resources on elastic agents with uuids [uuid] can not be updated."}),
        "status":       400
      });
    };

    /* eslint-disable camelcase */
    var allAgentsJSON = [
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
            "href": "http://api.go.cd/#agents"
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

    var agentsData = {
      "_embedded": {
        "agents": allAgentsJSON
      }
    };

    /* eslint-enable camelcase */
  });
});
