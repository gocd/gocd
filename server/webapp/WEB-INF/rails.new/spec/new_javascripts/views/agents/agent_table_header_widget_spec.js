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

define(["jquery", "mithril", "views/agents/agent_table_header"], function ($, m, AgentsTableHeader) {
  describe("Agent Table Header Widget", function () {
    var $root = $('#mithril-mount-point'), root = $root.get(0);

    beforeEach(function () {
      route();
    });

    afterEach(function () {
      unmount();
    });

    var route = function () {
      m.route.mode = "hash";
      m.route(root, '',
        {
          '':                  agentTableHeaderComponent(),
          '/:sortBy/:orderBy': agentTableHeaderComponent()
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


    it('should select the checkbox depending upon the "checkboxValue" ', function () {
      var checkbox = $root.find('thead input')[0];
      expect(checkbox.checked).toBe(checkboxValue());
    });


    it('should add the ascending css class to table header cell attribute when table is sorted ascending on the corresponding attribute', function () {
      m.route('/agentState/asc');
      m.redraw(true);
      var headerAttribute = $root.find("th:contains('Status') .sort");
      expect(headerAttribute).toHaveClass('asc');
    });


    it('should add the descending css class to table header cell attribute when table is sorted descending on the corresponding attribute', function () {
      m.route('/agentState/desc');
      m.redraw(true);
      var headerAttribute = $root.find("th:contains('Status') .sort");
      expect(headerAttribute).toHaveClass('desc');
    });

    var agentTableHeaderComponent = function () {
      return m.component(AgentsTableHeader,
        {
          onCheckboxClick: onCheckboxClick,
          checkboxValue:   checkboxValue,
          sortBy:          sortBy
        });
    };

    var onCheckboxClick = function () {
    };

    var sortBy = function () {
    };

    var checkboxValue = function () {
      return false;
    };
  });
});
