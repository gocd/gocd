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

describe("ResourcesListWidget", () => {
  const $                = require("jquery");
  const simulateEvent    = require('simulate-event');
  const m                = require('mithril');
  const Stream           = require('mithril/stream');
  const TriStateCheckbox = require('models/agents/tri_state_checkbox');
  require("foundation-sites");

  const ResourcesListWidget = require("views/agents/resources_list_widget");


  let $root, root, resources;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  let vm;
  beforeEach(() => {
    vm = {
      agentsCheckedState: {}
    };

    vm.dropdown = {
      reset:  Stream(true),
      states: {},
      add() {
      },

      hide(name) {
        this.states[name](false);
      }
    };

    vm.dropdown.states['environment'] = Stream(false);
    vm.dropdown.states['resource']    = Stream(false);

    const selectedAgentsResources = [['Linux', 'Java'], ['Gauge', 'Java']];

    resources = [
      new TriStateCheckbox('Gauge', selectedAgentsResources),
      new TriStateCheckbox('Java', selectedAgentsResources),
      new TriStateCheckbox('Linux', selectedAgentsResources),
      new TriStateCheckbox('Windows', selectedAgentsResources),
    ];

    mount(resources);
  });

  afterEach(() => {
    unmount();
  });

  it('should contain all the resources checkbox', () => {
    const allResources = $.find('.resources-items :checkbox');
    expect(allResources).toHaveLength(4);
    expect(allResources[0]).toHaveValue('Gauge');
    expect(allResources[1]).toHaveValue('Java');
    expect(allResources[2]).toHaveValue('Linux');
    expect(allResources[3]).toHaveValue('Windows');
  });

  it('should check resources that are present on all the agents', () => {
    const allResources = $.find('.resources-items :checkbox');
    expect(allResources[1]).toHaveValue('Java');
    expect(allResources[1]).toBeChecked();
  });

  it('should select resources as indeterminate that are present on some of the agents', () => {
    const allResources = $.find('.resources-items :checkbox');
    expect(allResources[2]).toHaveValue('Linux');
    expect(allResources[2].indeterminate).toBe(true);

    expect(allResources[0]).toHaveValue('Gauge');
    expect(allResources[0].indeterminate).toBe(true);
  });

  it('should uncheck resources that are not present on any the agents', () => {
    const allResources = $.find('.resources-items :checkbox');
    expect(allResources[3]).toHaveValue('Windows');
    expect(allResources[3]).not.toBeChecked();
    expect(allResources[3].indeterminate).toBe(false);
  });

  it('should have button to add resources', () => {
    const addButton = $root.find('.add-resource :button')[0];
    expect(addButton).toHaveText("Add");
  });

  it('should have button to apply resources', () => {
    const applyButton = $root.find('.add-resource :button')[1];
    expect(applyButton).toHaveText("Apply");
  });

  it('should add resource after invoking add button', () => {
    let allResources = $root.find('.resources-items :checkbox');
    expect(allResources).toHaveLength(4);

    const inputBox = $root.find('.add-resource-input').get(0);
    $(inputBox).val('Chrome');
    simulateEvent.simulate(inputBox, 'input');

    expect(inputBox).toHaveValue('Chrome');
    const addButton = $root.find('.add-resource .add-resource-btn')[0];
    simulateEvent.simulate(addButton, 'click');
    m.redraw();

    allResources = $root.find('.resources-items :checkbox');
    expect(allResources).toHaveLength(5);
  });


  it('should clear input-text box after adding resource', () => {
    let inputBox = $root.find('.add-resource input');
    $(inputBox).val('Chrome').trigger('change');

    expect(inputBox).toHaveValue('Chrome');
    const addButton = $root.find('.add-resource button')[0];
    addButton.click();
    m.redraw();

    inputBox = $root.find('.add-resource input');
    expect(inputBox).toHaveValue('');
  });

  it('should not add duplicate resources', () => {
    let allResources = $root.find('.resources-items input[type="Checkbox"]');
    expect(allResources).toHaveLength(4);
    expect(allResources[2]).toHaveValue('Linux');

    const inputBox = $root.find('.add-resource :input')[0];
    $(inputBox).val('Linux').trigger('change');

    const addButton = $root.find('.add-resource :button')[1];
    addButton.click();
    m.redraw();

    allResources = $root.find('.resources-items input[type="Checkbox"]');
    expect(allResources).toHaveLength(4);
  });

  describe('Page Spinner', () => {
    it('should show page spinner until resources are fetched', () => {
      mount(undefined);
      expect($('.page-spinner')).toBeInDOM();
      mount(resources);
      expect($('.page-spinner')).not.toBeInDOM();
    });
  });

  describe('Fetch Error', () => {
    it('should show error when resources fetch fails', () => {
      const err           = 'BOOM!';
      resourcesFetchError = function () {
        return err;
      };
      mount(resources);
      expect($('.alert')).toContainText(err);
    });
  });

  const mount = (resources) => {
    m.mount(root, {
      view() {
        return m(ResourcesListWidget, {
          hideDropDown,
          dropDownReset,
          resourcesFetchError,
          'onResourcesUpdate': Stream(),
          'resources':         Stream(resources)
        });
      }
    });

    m.redraw();
  };

  const hideDropDown = () => {
  };

  const dropDownReset = () => {
  };

  let resourcesFetchError = () => {
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

});
