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

function AgentsAjaxRefresher(url, redirectUrl, replicator, replicated_checkbox_group_ids, replicated_field_id_reader, time) {
    var oldCheckboxes = {};

    for (var i = 0; i < replicated_checkbox_group_ids.length; i++) {
        oldCheckboxes[replicated_checkbox_group_ids] = [];
    }

    function is_replicated_checkbox_group_id(id) {
        for (var i = 0; i < replicated_checkbox_group_ids.length; i++) {
            if (replicated_checkbox_group_ids[i] === id) {
                return true;
            }
        }
        return false;
    }

    return new AjaxRefresher(url, redirectUrl, {
        time: time,

        manipulateReplacement: function (receiver_id, replaceElement, replacementOptions) {
            if (is_replicated_checkbox_group_id(receiver_id)) {
                replicator.register_all_matching(replaceElement, '.agent_select', replicated_field_id_reader);
                oldCheckboxes[receiver_id] = jQuery('#' + receiver_id + " .agent_select");
            }
        },

        afterRefresh: function (receiver_id) {
            if (is_replicated_checkbox_group_id(receiver_id)) {
                oldCheckboxes[receiver_id].each(function () {
                    var elem = $(this);
                    replicator.unregister(elem, replicated_field_id_reader(elem));
                });
                oldCheckboxes[receiver_id] = [];
            }
        }
    });
}
