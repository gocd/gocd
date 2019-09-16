/*
 * Copyright 2019 ThoughtWorks, Inc.
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
TabsWizard = function() {

    function init(tab_manager) {
        this.tab_manager = tab_manager;
    }

    init.prototype.hookupCancel = function(cancelButtonClass, url) {
        jQuery('.' + cancelButtonClass).click(function() {
            window.location.href = url;
            return false;
        });
    };

    init.prototype.wireButtons = function(button_tabname_mapping) {
        var self = this;
        for (var button_name in button_tabname_mapping) {
            var _ = function() {
                var tab = self.tab_manager.subTabByName(button_tabname_mapping[button_name]);
                jQuery('#' + button_name).click(function() {
                    tab.open();
                    return false;
                });
            }();
        }
    };

    init.EnvironmentWizard = function() {
        sub_init.prototype = new init();

        function sub_init() {
            init.apply(this, arguments);
        }

        return sub_init;
    }();

    return init;
}();
