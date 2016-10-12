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

define(["jquery", "mithril", "views/agents/tri_state_checkbox_widget", "models/agents/tri_state_checkbox"], function ($, m, TriStateCheckboxWidget, TriStateCheckbox) {
  describe("Resource Checkbox Widget", function () {
    var $root     = $('#mithril-mount-point'), root = $root.get(0);
    var resources = [['Firefox'], ['Firefox', 'Chrome']];

    afterEach(function () {
      unmount();
    });

    it('should have checkbox with value', function () {
      mount(new TriStateCheckbox('Firefox', resources));
      var checkbox = $root.find('input')[0];
      expect(checkbox).toHaveValue('Firefox');
    });

    it('should select the box as checked depending upon check field', function () {
      mount(new TriStateCheckbox('Firefox', resources));
      var checkbox = $root.find('input')[0];
      expect(checkbox).toBeChecked();
    });

    it('should select the box as unchecked depending upon check field', function () {
      mount(new TriStateCheckbox('Linux', resources));
      var checkbox = $root.find('input')[0];
      expect(checkbox).not.toBeChecked();
    });

    it('should select the box as indeterminate depending upon the isIndeterminate field', function () {
      mount(new TriStateCheckbox('Chrome', resources));
      var checkbox = $root.find('input')[0];
      expect(checkbox.indeterminate).toBe(true);
    });

    var mount = function (triStateCheckbox) {
      m.mount(root,
        m.component(TriStateCheckboxWidget, {'triStateCheckbox': triStateCheckbox, 'index': 1})
      );
      m.redraw(true);
    };

    var unmount = function () {
      m.mount(root, null);
      m.redraw(true);
    };
  });
});
