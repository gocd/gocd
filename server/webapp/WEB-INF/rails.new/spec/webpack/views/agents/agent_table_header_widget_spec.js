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

//for skipped tests, jasmine isnt calling afterEach,
// so skipped the test suit to make sure randomize doesnt cause any problem
describe("Agent Table Header Widget", () => {

  var $      = require("jquery");
  var _      = require('lodash');
  var m      = require("mithril");
  var Stream = require("mithril/stream");

  require('jasmine-jquery');

  var AgentsTableHeader = require("views/agents/agent_table_header");
  var SortOrder         = require('views/agents/models/sort_order');

  var $root, root, sortOrder;
  beforeEach(() => {
    [$root, root] = window.createDomElementForTest();
  });
  afterEach(window.destroyDomElementForTest);

  beforeEach(() => {
    sortOrder = Stream(new SortOrder());
    sortOrder().perform = _.noop;
    route(true);
  });

  afterEach(() => {
    unmount();
  });

  var route = isUserAdmin => {
    m.route(root, '', {
      '':                  {
        view: function () {
          return agentTableHeaderComponent(isUserAdmin);
        }
      },
      '/:sortBy/:orderBy': {
        view: function () {
          return agentTableHeaderComponent(isUserAdmin);
        }
      }
    });

    m.route.set('');
    m.redraw();
  };

  var unmount = () => {
    m.route.set('');
    m.mount(root, null);
    m.redraw();
  };


  it('should select the checkbox depending upon the "checkboxValue" ', () => {
    var checkbox = $root.find('thead input')[0];
    expect(checkbox.checked).toBe(checkboxValue());
  });

  it('should not display checkbox for non-admin user', () => {
    route(false);
    expect($('thead input')).not.toBeInDOM();
  });


  it('should add the ascending css class to table header cell attribute when table is sorted ascending on the corresponding attribute', () => {
    sortOrder().toggleSortingOrder('hostname');
    m.redraw();
    var headerAttribute = $root.find("th:contains('Agent Name') .sort");
    expect(headerAttribute).toHaveClass('asc');
  });

  it('should add the descending css class to table header cell attribute when table is sorted descending on the corresponding attribute', () => {
    sortOrder().toggleSortingOrder('hostname');
    sortOrder().toggleSortingOrder('hostname');
    m.redraw();
    var headerAttribute = $root.find("th:contains('Agent Name') .sort");
    expect(headerAttribute).toHaveClass('desc');
  });

  var agentTableHeaderComponent = isUserAdmin => m(AgentsTableHeader, {
    onCheckboxClick: _.noop,
    checkboxValue:   checkboxValue,
    sortOrder:       sortOrder,
    isUserAdmin:     isUserAdmin
  });

  var checkboxValue = () => false;
});
