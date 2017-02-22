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

  function ConsoleParsingObserver(url, transformer, options) {
    var me   = this;

    var startLineNumber = 0;
    var inFlight = false;
    var finished = false;

    function acquireLock() {
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

        if (nextLine !== startLineNumber) {
          transformer.transform(data.match(/^.*([\n\r]+|$)/gm));
          startLineNumber = nextLine;
        }

        finished = jobResultJson[0].building_info.is_completed.toLowerCase() === "true";
        if (options && "function" === typeof options.onUpdate) {
          options.onUpdate();
        }

        if (finished && options && "function" === typeof options.onComplete) {
          options.onComplete();
        }
      }).fail($.noop).always(clearLock);
    }

    me.notify = consumeBuildLog;
  }

  var Types = {
    INFO: "##", ALERT: "@@", PREP: "pr", TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1"
  };

  function LogOutputTransformer(consoleElement) {
    var me = this;
    var currentSection;

    var re = /^([^|]{2})\|(.*)/;

    consoleElement.on("click", ".toggle", function toggleSectionCollapse(e) {
      e.stopPropagation();
      e.preventDefault();

      var section = $(e.currentTarget).closest(".section");
      section.toggleClass("open");
    });

    if (!consoleElement.find(".section").length) {
      currentSection = addBlankSection(consoleElement);
    }

    function addBlankSection(element) {
      var section = $("<dl class='section open'>");
      element.append(section);
      return section;
    }

    function adoptSection(section, prefix, line) {
      if ([Types.INFO, Types.ALERT].indexOf(prefix) > -1) {
        section.attr("data-type", "info");
      } else if ([Types.PREP].indexOf(prefix) > -1) {
        section.attr("data-type", "prep");
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "task");
      } else {
        section.attr("data-type", "info");
      }
      insertHeader(section, prefix, line);
    }

    function insertHeader(section, prefix, line) {
      var header = $("<dt>").attr("data-prefix", prefix).text(line);
      section.append(header);
    }

    function insertLine(section, prefix, line) {
      if (!section.data("multiline")) {
        section.prepend($("<a class='fa toggle'>"));
      }
      section.data("multiline", true);
      section.append($("<dd>").attr("data-prefix", prefix).text(line));
    }

    function isPartOfSection(section, prefix, line) {
      if (section.data("type") === "info") {
        return [Types.INFO, Types.ALERT].indexOf(prefix) > -1;
      }

      if (section.data("type") === "prep") {
        return prefix === Types.PREP;
      }

      if (section.data("type") === "task") {
        if (prefix === Types.TASK_START && !line.match(/\[go] Start to execute task:/)) {
          return true;
        }
        return [Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1;
      }

      return false;
    }

    me.transform = function buildDomFromLogs(logLines) {
      var line, match, prefix, body;

      for (var i = 0, len = logLines.length; i < len; i++) {
        line = logLines[i];
        match = line.match(re);

        if (match) {
          prefix = match[1];
          body = match[2];

          if (currentSection.is(":empty")) {
            adoptSection(currentSection, prefix, body);
          } else if (isPartOfSection(currentSection, prefix, body)) {
            insertLine(currentSection, prefix, body);
          } else {
            // close section and start a new one
            currentSection = addBlankSection(consoleElement);
            adoptSection(currentSection, prefix, body);
          }
        } else {
          // the last line is usually blank, and we don't want this extra element
          currentSection.append($("<dt>").text(line));
        }
      }
    };
  }

  // export
  window.ConsoleParsingObserver = ConsoleParsingObserver;
  window.LogOutputTransformer = LogOutputTransformer;
})(jQuery);