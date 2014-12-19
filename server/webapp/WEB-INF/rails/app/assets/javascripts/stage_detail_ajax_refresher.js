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

function StageDetailAjaxRefresher(url, redirectUrl, after_callback_map) {
    var oldCheckboxes = null;
    var replicator = new FieldStateReplicator();

    function registerAgentSelectorsUnder(table) {
        $(table).select('.job_selector').each(function (elem) {
            replicator.register(elem, elem.value);
        });
    }

    var jobs_grid = $('jobs_grid');
    if (jobs_grid){
        registerAgentSelectorsUnder(jobs_grid);
    }

    return new AjaxRefresher(url, redirectUrl, {
        afterRefresh: function (receiver_id) {
            var callback = after_callback_map[receiver_id];  
            callback && callback();
            if (receiver_id === 'jobs_grid') {
                oldCheckboxes.each(function (elem) {
                    replicator.unregister(elem, elem.value);
                });
                oldCheckboxes = null;
            }
        },
        dataFetcher: function () {
            return $("stage-history-page")? {"stage-history-page": $("stage-history-page").value} : {};
        },
        manipulateReplacement: function (receiver_id, replaceElement, replacementOptions) {
            if (receiver_id === 'jobs_grid') {
                registerAgentSelectorsUnder(replaceElement);
                oldCheckboxes = $$('.job_selector');
            }
        }
    });
}

function compare_link_handlers() {
    jQuery(".stage_history .stage").mouseover(function() {
        jQuery(this).find(".compare_pipeline").removeClass("hidden");
    });
    jQuery(".stage_history .stage").mouseout(function() {
        jQuery(this).find(".compare_pipeline").addClass("hidden");
    });
}