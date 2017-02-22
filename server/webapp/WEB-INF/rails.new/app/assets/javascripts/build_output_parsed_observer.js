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

  function Poller(interval) {
    var me = this;
    var pid;

    function startPolling(job) {
      if (pid !== null) {
        stopPolling();
      }

      job();
      pid = setInterval(job, interval);
    }

    function stopPolling() {
      clearInterval(pid);
    }

    me.start = startPolling;
    me.stop = stopPolling;
  }

  function ConsumeBuildLog(url, transformer) {
    var me   = this;
    var fail = $.noop, always = $.noop;

    var startLineNumber = 0;
    var inFlight = false;
    var finished = false;

    function acquireLock() {
      console.log("in flight: " + inFlight);
      console.log("finished: " + finished);
      if (!inFlight && !finished) {
        inFlight = !inFlight;
        return true;
      }
      return false;
    }

    function clearLock() {
      inFlight = false;
    }

    function consumeBuildLog(jobResultJson) {
      $.ajax({
        url:  url,
        type: "GET",
        dataType: "text",
        data: {"startLineNumber": startLineNumber},
        beforeSend: acquireLock
      }).done(function processLogOutput(data, status, xhr) {
        var nextLine = JSON.parse(xhr.getResponseHeader("X-JSON") || "[]")[0];

        console.log(startLineNumber);
        console.log(nextLine);

        if (nextLine !== startLineNumber) {
          transformer.transform(data.match(/^.*([\n\r]+|$)/gm));
          startLineNumber = nextLine;
        }

        finished = jobResultJson[0].building_info.is_completed.toLowerCase() === "true";
      }).fail(fail).always(clearLock);
    }

    me.notify = consumeBuildLog;
  }

  function LogOutputTransformer(element) {
    var me = this;

    var re = /^([^\|]{2})\|(.*)/;

    me.transform = function buildDomFromLogs(logLines) {
      var line, match, prefix, body;

      for (var i = 0, len = logLines.length; i < len; i++) {
        line = logLines[i];
        console.log(line)
        match = line.match(re);

        if (match) {
          prefix = match[1];
          body = match[2];

          element.append($("<div>").attr("data-prefix", prefix).text(body));
        } else {
          element.append($("<div>").text(line));
        }
      }
    };
  }

  $(function domReady() {
    var jobDetails = $(".job_details_content");

    if (!jobDetails.length) return;

    var jobStatusUrl = "jobStatus.json?pipelineName=" + jobDetails.data("pipeline") + "&stageName=" + jobDetails.data("stage") + "&jobId=" + jobDetails.data("job");
    var poller = new DashboardPeriodicalExecuter(jobStatusUrl, function detectJobCompleted(jobInfo) {
      return jobInfo[0].building_info.is_completed.toString() === "true";
    });

    poller.register(new TimerObserver(jobDetails.data("build")));

    //var consoleInterval = 5000; // 5 seconds
    var build           = $("[data-console-url]");

    if (build.length) {
      var consoleUrl = context_path("files/" + build.data("console-url"));
      var parser = new ConsumeBuildLog(consoleUrl, new LogOutputTransformer($(".buildoutput_pre")));
      poller.register(parser);
    }

    poller.start();

    $(function () {
      $(document).on('click.changeTheme', '.change-theme', function (evt) {
        $('.sub_tab_container_content').toggleClass('white-theme');
      });
    });


  });

})(jQuery);