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

ViewTemplate = function(view_template_parametrized_url) {

    function _addListener(view_template_link, template_selector) {
        template_selector = template_selector || selected_template_name;
        jQuery(view_template_link).click(function() {
            var template_name = template_selector();
            var constructed_url = view_template_parametrized_url.replace("__template_name__", template_name);
            Util.ajax_modal(constructed_url, {overlayClose: false, title: template_name}, function(text){return text});
            return false;
        })
    }

    selected_template_name = function(){
        return jQuery("#select_template").val();
    }

    return {
        addListener: _addListener
    }
}