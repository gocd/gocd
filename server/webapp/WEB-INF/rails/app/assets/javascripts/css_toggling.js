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

function make_collapsable(container_id) {
    var container_id_selector = "#" + container_id.replace(/\./g,"\\.");
    jQuery(container_id_selector + " .hidereveal_expander").click(function (event) {
        jQuery(container_id_selector).toggleClass("hidereveal_collapsed");
        new ElementAligner().alignAll();
        event.stopPropagation();
    });
}

var MaterialDetailsToggler = function () {
    var material_details = "not set";
    var material_summaries = "not set";
    var last_open_details = "not set";
    var last_open_summary = "not set";
    var callback;

    function bind(cur_summary, cur_detail) {
        cur_summary.observe('click', function (evt) {
            cur_summary.toggleClassName("selected");
            cur_detail.toggleClassName("hidden");

            last_open_summary.toggleClassName("selected");
            last_open_details.toggleClassName("hidden");

            last_open_summary = cur_summary;
            last_open_details = cur_detail;
            if (callback) callback(cur_summary, cur_detail);
            Event.stop(evt);
        });
    }

    function stop() {
        material_summaries.each(function(summary) {
            summary.stopObserving('click')
        });
    }

    function start(callback_fn) {
        if (callback_fn) callback = callback_fn;
        material_details = $$(".materials .material_details .material_detail");
        material_summaries = $$(".materials .material_summaries .material_summary");
        last_open_details = material_details[0];
        last_open_summary = material_summaries[0];
        for (var i = 0; i < material_summaries.length; i++) {
            bind(material_summaries[i], material_details[i]);
        }
    }

    return {
        start_observing: start,
        stop_observing: stop,
        reset: function () {
            stop();
            start();
        }
    };

}();



