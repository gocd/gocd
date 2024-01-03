/*
 * Copyright 2024 Thoughtworks, Inc.
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

  function registerCheckboxStatesToRememberUnder(elementOrSelector) {
    const element = $(elementOrSelector);
    if (element.length > 0) {
      element.find('.job_selector').each(function (i, elem) {
        replicator.register(elem, elem.value);
      });
    }
  }

  // Remember checkbox selections between refreshes. Currently the re-run checkboxes are only displayed 
  // when the stage is completed. Arguably we could stop updating/refreshing the jobs grid once the 
  // stage is completed and avoid all of this, however not sure of the implications of doing so.
  // Historically (prior to April 2011 commit fa93257e07c3a7d) the checkboxes were available even when the 
  // stage was still running, so remembering the state UI side was definitely needed earlier, but that is 
  // not possible now so it's not clear if this (and FieldStateReplicator) is really needed at all.
  registerCheckboxStatesToRememberUnder('#jobs_grid');

  return new AjaxRefresher(url, {
    afterRefresh: function (receiver_id) {
      const callback = after_callback_map[receiver_id];
      callback && callback();
      if (receiver_id === 'jobs_grid') {
        // We've refreshed the grid. Stop watching the old elements we have now removed
        oldCheckboxes.each(function (i, elem) {
          replicator.unregister(elem, elem.value);
        });
        oldCheckboxes = null;
      }
    },
    dataFetcher: function () {
      const pageElement = $("#stage-history-page");
      return pageElement ? {
        "stage-history-page": pageElement.val()
      } : {};
    },
    manipulateReplacement: function (receiver_id, replaceElement) {
      if (receiver_id === 'jobs_grid') {
        // We are about to replace HTML in the jobs grid. We now need to watch these so we can remember checkbox
        // selections between refreshes.
        registerCheckboxStatesToRememberUnder(replaceElement);
        oldCheckboxes = $('.job_selector');
      }
    }
  });
}

function compare_link_handlers() {
  const individualStage = $(".stage_history .stage");
  individualStage.mouseover(function() {
    $(this).find(".compare_pipeline").removeClass("hidden");
  });
  individualStage.mouseout(function() {
    $(this).find(".compare_pipeline").addClass("hidden");
  });
}
