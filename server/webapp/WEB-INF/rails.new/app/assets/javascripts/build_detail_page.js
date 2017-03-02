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

  function MultiplexingTransformer(transformers) {
    this.transform = function processAllTransformers(logLines) {
      for (var i = 0, len = transformers.length; i < len; i++) {
        transformers[i].transform(logLines);
      }
    };
  }

  $(function initConsolePageDomReady() {
    var jobDetails = $(".job_details_content");

    if (!jobDetails.length) return;

    var jobStatusUrl = "jobStatus.json?pipelineName=" + jobDetails.data("pipeline") + "&stageName=" + jobDetails.data("stage") + "&jobId=" + jobDetails.data("job");
    var executor     = new DashboardPeriodicalExecutor(jobStatusUrl, function detectJobCompleted(jobInfo) {
      return jobInfo[0].building_info.is_completed.toString() === "true";
    });

    var build = $("[data-console-url]");

    if (build.length) {
      var consoleUrl = context_path("files/" + build.data("console-url"));
      var containers = build.find(".buildoutput_pre"), transformers = [];

      $.each(containers, function initEachConsoleArea(i, area) {
        var container = $(area);
        transformers.push(new LogOutputTransformer(container, new Foldable()));

        if (container.is("#tab-content-of-console *")) {
          new ConsoleScroller(container, $("#build_console"), $('.auto-scroll')).startScroll();
        }

        container.find(".console-action-bar").on("click", ".toggle-timestamps", function toggleLogTimestamps(e) {
          e.stopPropagation();
          e.preventDefault();

          container.toggleClass("with-timestamps");
        });
      });

      build.find(".console-action-bar").on("click", ".toggle-timestamps", function toggleLogTimestamps(e) {
        e.stopPropagation();
        e.preventDefault();

        $(e.currentTarget).closest(".console-area").toggleClass("with-timestamps");
      });

      executor.register(new ConsoleParsingObserver(consoleUrl, new MultiplexingTransformer(transformers), {
        onUpdate:   function () {
          containers.trigger("consoleUpdated");
        },
        onComplete: function () {
          containers.trigger("consoleCompleted");
        }
      }));
    }

    executor.register(new TimerObserver(jobDetails.data("build")));
    executor.register(new BuildSummaryObserver($('.build_detail_summary')));

    executor.start();

    build.on('click.changeTheme', '.change-theme', function () {
      $('.sub_tab_container_content').toggleClass('white-theme');
    });
  });

})(jQuery);
