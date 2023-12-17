/*
 * Copyright 2023 Thoughtworks, Inc.
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
function StageDetailAjaxRefresher(url, after_callback_map) {
  const replicator = new FieldStateReplicator();
  let oldCheckboxes = null;

  function registerAgentSelectorsUnder(elementOrSelector) {
    const element = jQuery(elementOrSelector);
    if (element.length > 0) {
      element.find('.job_selector').each(function (i, elem) {
        replicator.register(elem, elem.value);
      });
    }
  }

  registerAgentSelectorsUnder('#jobs_grid');

  return new AjaxRefresher(url, {
    afterRefresh: function (receiver_id) {
      const callback = after_callback_map[receiver_id];
      callback && callback();
      if (receiver_id === 'jobs_grid') {
        oldCheckboxes.each(function (i, elem) {
          replicator.unregister(elem, elem.value);
        });
        oldCheckboxes = null;
      }
    },
    dataFetcher: function () {
      const pageElement = jQuery("#stage-history-page");
      return pageElement ? {
        "stage-history-page": pageElement.val()
      } : {};
    },
    manipulateReplacement: function (receiver_id, replaceElement) {
      if (receiver_id === 'jobs_grid') {
        registerAgentSelectorsUnder(replaceElement);
        oldCheckboxes = jQuery('.job_selector');
      }
    }
  });
}

function compare_link_handlers() {
  const individualStage = jQuery(".stage_history .stage");
  individualStage.mouseover(function() {
    jQuery(this).find(".compare_pipeline").removeClass("hidden");
  });
  individualStage.mouseout(function() {
    jQuery(this).find(".compare_pipeline").addClass("hidden");
  });
}
