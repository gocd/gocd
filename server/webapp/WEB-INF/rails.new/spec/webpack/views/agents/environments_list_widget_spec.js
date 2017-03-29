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

describe("Environments List Widget", () => {
  const _                = require('lodash');
  const $                = require("jquery");
  const m                = require("mithril");
  const Stream           = require("mithril/stream");
  const TriStateCheckbox = require('models/agents/tri_state_checkbox');

  require("foundation-sites");
  require("jasmine-jquery");
  require('jasmine-ajax');

  const EnvironmentsListWidget = require("views/agents/environments_list_widget");

  let root, environments;
  beforeEach(() => {
    [, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(() => {
    jasmine.Ajax.install();
    jasmine.Ajax.stubRequest(/\/api\/agents/).andReturn({"status": 304});
  });

  beforeEach((done) => {
    const selectedAgents = [
      {
        uuid: '1',
        environments() {
          return ['Dev', 'Testing'];
        }
      },
      {
        uuid: '2',
        environments() {
          return ['Build', 'Testing'];
        }
      }
    ];

    const selectedAgentsEnvironments = _.map(selectedAgents, (agent) => agent.environments());

    environments = [
      new TriStateCheckbox('Build', selectedAgentsEnvironments),
      new TriStateCheckbox('Deploy', selectedAgentsEnvironments),
      new TriStateCheckbox('Dev', selectedAgentsEnvironments),
      new TriStateCheckbox('Testing', selectedAgentsEnvironments),
    ];


    mount(done, environments);
  });

  afterEach(() => {
    unmount();
    jasmine.Ajax.uninstall();
  });

  it('should contain all the environments checkbox', () => {
    const allEnvironments = $.find('.resources-items :checkbox');
    expect(allEnvironments).toHaveLength(4);
    expect(allEnvironments[0]).toHaveValue('Build');
    expect(allEnvironments[1]).toHaveValue('Deploy');
    expect(allEnvironments[2]).toHaveValue('Dev');
    expect(allEnvironments[3]).toHaveValue('Testing');
  });

  it('should check environments that are present on all the agents', () => {
    const allEnvironments = $.find('.resources-items :checkbox');
    expect(allEnvironments[3]).toHaveValue('Testing');
    expect(allEnvironments[3]).toBeChecked();
  });

  it('should select environments as indeterminate that are present on some of the agents', () => {
    const allEnvironments = $.find('.resources-items :checkbox');
    expect(allEnvironments[2]).toHaveValue('Dev');
    expect(allEnvironments[2].indeterminate).toBe(true);

    expect(allEnvironments[0]).toHaveValue('Build');
    expect(allEnvironments[0].indeterminate).toBe(true);
  });

  it('should uncheck environments that are not present on any the agents', () => {
    const allEnvironments = $.find('.resources-items :checkbox');
    expect(allEnvironments[1]).toHaveValue('Deploy');
    expect(allEnvironments[1]).not.toBeChecked();
    expect(allEnvironments[1].indeterminate).toBe(false);
  });

  describe('Page Spinner', () => {
    it('should show page spinner until environments are fetched', () => {
      mount(_.noop);
      expect($('.page-spinner')).toBeInDOM();
      mount(_.noop, environments);
      expect($('.page-spinner')).not.toBeInDOM();
    });
  });

  describe('Fetch Error', () => {
    it('should show error when environments fetch fails', () => {
      const err              = 'BOOM!';
      environmentsFetchError = function () {
        return err;
      };
      mount(_.noop, environments);
      expect($('.alert')).toContainText(err);
    });
  });

  const mount = (done, environments) => {
    m.mount(root,
      {
        oncreate: done,
        view() {
          return m(EnvironmentsListWidget, {
            hideDropDown,
            dropDownReset,
            onEnvironmentsUpdate,
            environments: Stream(environments),
            environmentsFetchError
          });
        }
      }
    );
    m.redraw();
  };

  const hideDropDown         = () => {
  };
  const dropDownReset        = () => {
  };
  const onEnvironmentsUpdate = () => {
  };
  let environmentsFetchError = () => {
  };

  const unmount = () => {
    m.mount(root, null);
    m.redraw();
  };

});
