/*
 * Copyright 2017 ThoughtWorks, Inc.
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
;(function ($) {
  "use strict";

  function BuildSummaryObserver(summaryElement) {
    this.summaryElement = summaryElement;
  }

  $.extend(BuildSummaryObserver.prototype, {
    notify:                  function notifySummaryObserver(json) {
      var jobInfo = json[0];
      if (!this.isComplete) {
        this.updateBuildSummary(jobInfo);
        this.isComplete = jobInfo.building_info.is_completed.toString() === "true";
      } else {
        this.updateBuildResult(jobInfo);
        this.displayAnyErrorMessages(jobInfo);
      }
    },
    displayAnyErrorMessages: function displayErrorMessagesOnBuildDetails(json) {
      if (is_result_unknown(json)) {
        var text = $$WordBreaker.break_text(json.building_info.name);
        $('#trans_content').html("Failed to find log in <br/>" + text);
        new TransMessage('trans_message', $('#build_detail_summary_container')[0], {
          type:     TransMessage.TYPE_ERROR,
          autoHide: false,
          height:   50
        });
      }
    },
    updateBuildResult:       function updateBuildResult(json) {
      var result    = json.building_info.result.toLowerCase();
      var container = $(".build-status");

      clean_active_css_class_on_element(container[0]);
      container.addClass(result.toLowerCase())
    },
    updateBuildSummary:      function updateBuildSummary(json) {

      function toHumanReadable(timestamp) {
        timestamp = parseInt(timestamp);
        if (isNaN(timestamp) || timestamp === -1) {
          return "N/A";
        }
        var time = new Date(timestamp);
        return moment(time).format('DD MMM YYYY [at] HH:mm:ss [Local Time]');
      };

      $(".job_details_content").attr("data-result", status).removeData("data-result");
      $('#build_scheduled_date').text(toHumanReadable(json.building_info.build_scheduled_date));
      $('#build_assigned_date').text(toHumanReadable(json.building_info.build_assigned_date));
      $('#build_preparing_date').text(toHumanReadable(json.building_info.build_preparing_date));
      $('#build_building_date').text(toHumanReadable(json.building_info.build_building_date));
      $('#build_completing_date').text(toHumanReadable(json.building_info.build_completing_date));
      $('#build_completed_date').text(toHumanReadable(json.building_info.build_completed_date));
      $('#agent_name').attr("href", context_path("agents/" + json.building_info.agent_uuid));
      $('#agent_name').text(json.building_info.agent + ' (' + json.building_info.agent_ip + ')');

      // TODO: update css on building panel
      json_to_css.update_build_detail_header(json);
    }
  });

  // export
  window.BuildSummaryObserver = BuildSummaryObserver;
})(jQuery);
