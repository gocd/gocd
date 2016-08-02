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

define(["jquery", "mithril", 'models/agents/agents', 'models/agents/resources', "views/agents/resources_list_widget", "foundation.dropdown"], function ($, m, Agents, Resources, ResourcesListWidget) {
  describe("Resources List Widget", function () {

    var $root = $('#mithril-mount-point'), root = $root.get(0);

    var vm = {
      agentsCheckedState: {}
    };

    vm.dropdown = {
      reset:  m.prop(true),
      states: {},
      add:    function () {
      },

      hide: function (name) {
        this.states[name](false);
      }
    };

    vm.dropdown.states['environment'] = m.prop(false);
    vm.dropdown.states['resource']    = m.prop(false);

    var updateResources = function (resourcesToBeAdded, resourcesToBeRemoved) {
    };

    beforeAll(function () {
      jasmine.Ajax.install();
      jasmine.Ajax.stubRequest(/\/api\/admin\/internal\/resources/).andReturn({
        "responseText": JSON.stringify(['Linux', 'Gauge', 'Java', 'Windows']),
        "status":       200
      });
      jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({"status": 304});
    });

    beforeEach(function () {
      var selectedAgents = [
        {
          uuid:      '1',
          resources: function () {
            return ['Linux', 'Java'];
          }
        },
        {
          uuid:      '2',
          resources: function () {
            return ['Gauge', 'Java'];
          }
        }
      ];

      Resources.init(selectedAgents);

      mount();
    });

    afterAll(function () {
      jasmine.Ajax.uninstall();
    });

    it('should contain all the resources checkbox', function () {
      var all_resources = $.find('.resources-items :checkbox');
      expect(all_resources.length).toBe(4);
      expect(all_resources[0].value).toBe('Linux');
      expect(all_resources[1].value).toBe('Gauge');
      expect(all_resources[2].value).toBe('Java');
      expect(all_resources[3].value).toBe('Windows');
    });

    it('should check resources that are present on all the agents', function () {
      var all_resources = $.find('.resources-items :checkbox');
      expect(all_resources[2].value).toBe('Java');
      expect(all_resources[2].checked).toBe(true);
    });

    it('should select resources as indeterminate that are present on some of the agents', function () {
      var all_resources = $.find('.resources-items :checkbox');
      expect(all_resources[0].value).toBe('Linux');
      expect(all_resources[0].indeterminate).toBe(true);

      expect(all_resources[1].value).toBe('Gauge');
      expect(all_resources[1].indeterminate).toBe(true);
    });

    it('should uncheck resources that are not present on any the agents', function () {
      var all_resources = $.find('.resources-items :checkbox');
      expect(all_resources[3].value).toBe('Windows');
      expect(all_resources[3].checked).toBe(false);
      expect(all_resources[3].indeterminate).toBe(false);
    });

    it('should have button to add resources', function () {
      var add_button = $root.find('.add-resource :button')[0];
      expect(add_button.textContent).toBe("Add");
    });

    it('should have button to apply resources', function () {
      var apply_button = $root.find('.add-resource :button')[1];
      expect(apply_button.textContent).toBe("Apply");
    });

    it('should add resource after invoking add button', function () {
      var all_resources = $root.find('.resources-items :checkbox');
      expect(all_resources.length).toBe(4);

      var input_box = $root.find('.add-resource :text')[0];
      $(input_box).val('Chrome').trigger('input');


      var add_button = $root.find('.add-resource :button')[0];
      add_button.click();
      m.redraw(true);

      var all_resources = $root.find('.resources-items :checkbox');
      expect(all_resources.length).toBe(5);
    });


    it('should clear input-text box after adding resource', function () {
      var input_box = $root.find('.add-resource input');
      $(input_box).val('Chrome').trigger('change');

      expect(input_box.val()).toBe('Chrome');
      var add_button = $root.find('.add-resource button')[0];
      add_button.click();
      m.redraw(true);

      var input_box = $root.find('.add-resource input');
      expect(input_box.val()).toBe('');
    });

    it('should not add duplicate resources', function () {
      var all_resources = $root.find('.resources-items input[type="Checkbox"]');
      expect(all_resources.length).toBe(4);
      expect(all_resources[0].value).toBe('Linux');

      var input_box = $root.find('.add-resource :input')[0];
      $(input_box).val('Linux').trigger('change');

      var add_button = $root.find('.add-resource :button')[1];
      add_button.click();
      m.redraw(true);

      var all_resources = $root.find('.resources-items input[type="Checkbox"]');
      expect(all_resources.length).toBe(4);
    });

    var mount = function () {
      m.mount(root, m.component(ResourcesListWidget, {
          'dropDownState':     vm.dropdown,
          'onResourcesUpdate': updateResources
        })
      );
      m.redraw(true);
    };

  });
});
