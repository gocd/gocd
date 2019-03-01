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

//for skipped tests, jasmine isnt calling afterEach,
// so skipped the test suit to make sure randomize doesnt cause any problem
import {TestHelper} from "views/pages/artifact_stores/spec/test_helper";

describe("Agent Table Header Widget", () => {

  const $      = require("jquery");
  const _      = require('lodash');
  const m      = require("mithril");
  const Stream = require("mithril/stream");

  require('jasmine-jquery');

  const AgentsTableHeader = require("views/agents/agent_table_header");
  const RouteHandler      = require('views/agents/models/route_handler');
  const helper            = new TestHelper();

  let routeHandler;

  beforeEach(() => {
    routeHandler           = Stream(new RouteHandler());
    routeHandler().perform = _.noop;
    route(true);
  });

  afterEach(() => {
    unmount();
  });

  const route = (isUserAdmin) => {
    helper.route('', () => {
      return {
        '':                  {
          view() {
            return agentTableHeaderComponent(isUserAdmin);
          }
        },
        '/:sortBy/:orderBy': {
          view() {
            return agentTableHeaderComponent(isUserAdmin);
          }
        }
      };
    });

    m.route.set('');
    m.redraw();
  };

  const unmount = () => {
    helper.unmount();
  };


  it('should select the checkbox depending upon the "checkboxValue" ', () => {
    const checkbox = helper.find('thead input')[0];
    expect(checkbox.checked).toBe(checkboxValue());
  });

  it('should not display checkbox for non-admin user', () => {
    unmount();
    route(false);
    expect($('thead input')).not.toBeInDOM();
  });


  it('should add the ascending css class to table header cell attribute when table is sorted ascending on the corresponding attribute', () => {
    routeHandler().toggleSortingOrder('hostname');
    m.redraw();
    const headerAttribute = helper.find("th:contains('Agent Name') .sort");
    expect(headerAttribute).toHaveClass('asc');
  });

  it('should add the descending css class to table header cell attribute when table is sorted descending on the corresponding attribute', () => {
    routeHandler().toggleSortingOrder('hostname');
    routeHandler().toggleSortingOrder('hostname');
    m.redraw();
    const headerAttribute = helper.find("th:contains('Agent Name') .sort");
    expect(headerAttribute).toHaveClass('desc');
  });

  const agentTableHeaderComponent = (isUserAdmin) => m(AgentsTableHeader, {
    onCheckboxClick: _.noop,
    checkboxValue,
    sortOrder:       routeHandler,
    isUserAdmin
  });

  const checkboxValue = () => false;
});
