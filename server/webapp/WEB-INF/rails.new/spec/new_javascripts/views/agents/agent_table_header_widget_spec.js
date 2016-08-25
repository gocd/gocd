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
    beforeAll(function () {
      mount();
    });

    it('should contain the agents table header information', function () {
      var children = $root.find('thead tr').children();
      expect(children.length).toBe(9);
      expect($(children[0]).html()).toBe('<input type="checkbox">');
      expect($(children[1]).text()).toBe('Agent Name');
      expect($(children[2]).text()).toBe('Sandbox');
      expect($(children[3]).text()).toBe('OS');
      expect($(children[4]).text()).toBe('IP Address');
      expect($(children[5]).text()).toBe('Status');
      expect($(children[6]).text()).toBe('Free Space');
      expect($(children[7]).text()).toBe('Resources');
      expect($(children[8]).text()).toBe('Environments');
    });

    it('should select the checkbox depending upon the "checkboxValue" ', function () {
      var checkbox = $root.find('thead input')[0];
      expect(checkbox.checked).toBe(checkboxValue());
    });

    var mount = function () {
      m.mount(root,
        m.component(AgentsTableHeader,
          {
            'onCheckboxClick': onCheckboxClick,
            'checkboxValue':   checkboxValue
          })
      );
      m.redraw(true);
    };

    var onCheckboxClick = function () {
    };

    var checkboxValue = function () {
      return false;
    };
  });
});
