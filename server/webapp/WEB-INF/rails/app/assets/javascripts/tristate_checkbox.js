/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

TriStateCheckbox = function() {

    function refresh_view(view, value_holder) {
        view.className = 'tristate ' + value_holder.value;
    }

    function checkbox(view, value_holder, enabled) {
        value_holder.addClassName('hidden');
        refresh_view(view, value_holder);
        if (enabled) {
            var popupHandler = MicroContentPopup.lookupHandler(view);
            view.observe('mousedown', function (event) {
                popupHandler && (typeof(popupHandler.tristate_clicked) === "function") && popupHandler.tristate_clicked();
                var options = value_holder.options;
                var selected_index = find_selected_index(options);
                var next = (selected_index + 1) % options.length;
                value_holder.value = options[next].value;

                refresh_view(view, value_holder);
                Event.stop(event);
            });
        }
    }

    function find_selected_index(options) {
        for (var i = 0; i < options.length; i++) {
            if (options[i].selected) {
                return i;
            }
        }
        return 0;
    }

    return checkbox;
}();

