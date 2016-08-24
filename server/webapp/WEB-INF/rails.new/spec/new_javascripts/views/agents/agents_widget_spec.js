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

define(["jquery", "mithril", 'models/agents/agents', "views/agents/agents_widget"], function ($, m, Agents, AgentsWidget) {
  describe("Agents Widget", function () {

    var $root = $('#mithril-mount-point'), root = $root.get(0);
    var agents;

    beforeAll(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({
        "responseText": JSON.stringify(agentsData),
        "status":       200
      });
    });

    beforeEach(function () {
      mount();
    });

    afterEach(function () {
      unmount();
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
    });

    it('should contain the agents state count information', function () {
      var agentStateCount = $root.find('.search-summary')[0];
      var stateCountInfo  = "Total2Pending0Enabled0Disabled2";
      expect(agentStateCount.innerText).toBe(stateCountInfo);
    });

    it('should contain the agent rows equal to the number of agents', function () {
      var agentRows = $root.find('table tbody tr');
      expect(agentRows.length).toBe(2);
    });

    it('should contain the agent row information', function () {
      var agentInfo      = $root.find('table tbody tr')[0];
      var firstAgentInfo = $(agentInfo).find('td');
      expect(firstAgentInfo.length).toBe(9);
      expect(firstAgentInfo[0].innerHTML).toBe('<input type="checkbox">');
      expect(firstAgentInfo[1].innerText).toBe('host-1');
      expect(firstAgentInfo[2].innerText).toBe('usr/local/foo');
      expect(firstAgentInfo[3].innerText).toBe('Linux');
      expect(firstAgentInfo[4].innerText).toBe('10.12.2.200');
      expect(firstAgentInfo[5].innerText).toBe('Disabled');
      expect(firstAgentInfo[6].innerText).toBe('Unknown');
      expect(firstAgentInfo[7].innerText).toBe('Firefox');
      expect(firstAgentInfo[8].innerText).toBe('Dev, Test');
    });

    it('should select all the agents when selectAll checkbox is selected', function () {
      var allBoxes          = $root.find('tbody :checkbox');
      var selectAllCheckbox = $root.find('thead :checkbox');

      expect(selectAllCheckbox[0].checked).toBe(false);
      expect(allBoxes[0].checked).toBe(false);

      $(selectAllCheckbox).click();
      m.redraw(true);

      expect(selectAllCheckbox[0].checked).toBe(true);
      expect(allBoxes[0].checked).toBe(true);
    });

    it('should hide all dropdown on click of the body', function () {
      var selectAllCheckbox = $root.find('thead :checkbox');
      $(selectAllCheckbox).click();
      m.redraw(true);

      var resourceButton = $root.find("button:contains('Resources')");
      $(resourceButton).click();
      m.redraw(true);

      var resourcesList = $root.find("button:contains('Resources')").parent()[0];
      expect(resourcesList.classList).toContain('is-open');

      var body = $root.find('.search-panel');
      $(body).click();
      m.redraw(true);
      expect(resourcesList.classList).not.toContain('is-open');
    });

    it('should not hide dropdown on click of dropdown list', function () {
      var selectAllCheckbox = $root.find('thead :checkbox');

      selectAllCheckbox.click();

      var resource       = $root.find('.has-dropdown')[0];
      var resourceButton = $(resource).find('button')[0];
      $(resourceButton).click();
      m.redraw(true);

      var resourcesList = $root.find('.has-dropdown')[0];
      expect(resourcesList.classList).toContain('is-open');

      $(resourcesList).click();
      expect(resourcesList.classList).toContain('is-open');

      var disableButton = $root.find("button:contains('Disable')");
      $(disableButton).click();
    });

    it('should show message after disabling the agents', function () {
      var agents_checkbox = $root.find('.go-table thead tr input[type="checkbox"]');

      $(agents_checkbox[0]).click();
      m.redraw(true);

      var disableButton = $root.find("button:contains('Disable')");
      var message       = $root.find('.alert-box');
      expect(message.length).toBe(0);
      $(disableButton).click();
      m.redraw(true);

      var message = $root.find('.alert-box');
      expect(message.text()).toBe('Disabled 2 agents');
    });

    it('should show message after enabling the agents', function () {
      var agents_checkbox = $root.find('.go-table thead tr input[type="checkbox"]');

      $(agents_checkbox[0]).click();
      m.redraw(true);

      var buttons = $root.find('.agent-button-group button');
      var message = $root.find('.alert-box');

      expect(message.length).toBe(0);
      buttons[2].click();
      m.redraw(true);

      var message = $root.find('.alert-box');
      expect(message.text()).toBe('Enabled 2 agents');
    });

    it('should show message after deleting the agents', function () {
      var agents_checkbox = $root.find('.go-table thead tr input[type="checkbox"]');
      var delete_button   = $root.find('.agent-button-group button')[0];

      $(agents_checkbox[0]).click();
      m.redraw(true);

      expect(delete_button.disabled).toBe(false);
      delete_button.click();
      m.redraw(true);

      var message = $root.find('.alert-box');
      expect(message.text()).toBe('Deleted 2 agents');
      expect(delete_button.disabled).toBe(true);
    });

    it('should show message after updating resource of the agents', function () {
      var resource = $root.find('.add-resource');

      var agents_checkbox = $root.find('.go-table thead tr input[type="checkbox"]');
      var apply_resource  = resource.find('button')[1];

      $(agents_checkbox[0]).click();
      m.redraw(true);

      apply_resource.click();
      m.redraw(true);

      var message = $root.find('.alert-box');
      expect(message.text()).toBe('Resources modified on 2 agents');
    });

    it('should show message after updating environment of the agents', function () {
      var environment       = $root.find('.env-dropdown');
      var agents_checkbox   = $root.find('.go-table thead tr input[type="checkbox"]');
      var apply_environment = environment.find('button');

      $(agents_checkbox[0]).click();
      m.redraw(true);

      apply_environment.click();
      m.redraw(true);

      var message = $root.find('.alert-box');
      expect(message.text()).toBe('Environment modified on 2 agents');
    });

    it('should show only filtered agents after inserting filter text', function () {
      var search_field         = $root.find('#filter-agent')[0];
      var no_of_agents_on_page = $root.find('table tbody tr');
      expect(no_of_agents_on_page.length).toBe(2);

      $(search_field).val('host-2').trigger('input');
      m.redraw(true);

      expect($(search_field).val()).toBe('host-2');
      no_of_agents_on_page = $root.find('table tbody tr');
      expect(no_of_agents_on_page.length).toBe(1);
    });

    it('should filter the agents based on filter text value', function () {
      var search_field = $root.find('#filter-agent')[0];
      $(search_field).val('').trigger('input');
      m.redraw(true);

      var no_of_agents_on_page = $root.find('table tbody tr');
      expect(no_of_agents_on_page.length).toBe(2);

      $(search_field).val('invalidtextnotvalid').trigger('input');
      m.redraw(true);

      expect($(search_field).val()).toBe('invalidtextnotvalid');

      no_of_agents_on_page = $root.find('table tbody tr');
      expect(no_of_agents_on_page.length).toBe(0);

      $(search_field).val('').trigger('input');
      m.redraw(true);

      expect($(search_field).val()).toBe('');

      no_of_agents_on_page = $root.find('table tbody tr');
      expect(no_of_agents_on_page.length).toBe(2);
    });

    it('should preserve the selection of agents during filter', function () {
      var search_field = $root.find('#filter-agent')[0];

      $(search_field).val('host-1').trigger('input');
      m.redraw(true);

      var allBoxes = $root.find('.go-table tbody tr input[type="checkbox"]');

      expect(allBoxes.length).toBe(1);
      $(allBoxes[0]).prop('checked', true).trigger('input');
      expect(allBoxes[0].checked).toBe(true);

      $(search_field).val('').trigger('input');
      m.redraw(true);

      var allBoxes = $root.find('.go-table tbody tr input[type="checkbox"]');
      expect(allBoxes.length).toBe(2);
      expect(allBoxes[0].checked).toBe(true);
      expect(allBoxes[1].checked).toBe(false);
    });


    var mount = function () {
      m.mount(root,
        m.component(AgentsWidget)
      );
      m.redraw(true);
    };

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };

    var agentsData = {
      "_embedded": {
        "agents": [
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
            "build_state":        "Unknown",
            "resources":          [
              "Firefox"
            ],
            "environments":       [
              "Dev",
              "Test"
            ]
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
        ]
      }
    };
  });
});
