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
;(function ($, c) {
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
    INFO: "##", ALERT: "@@", PREP: "pr", PREP_ERR: "pe", TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1", JOB_PASS: "j0", JOB_FAIL: "j1"
  };

  function LogOutputTransformer(consoleElement) {
    var me = this;
    var currentSection, lineCursor;
    var ansi = new AnsiUp();
    ansi.use_classes = true;

    var re = /^([^|]{2})\|(\d\d:\d\d:\d\d\.\d\d\d)(.*)/; // prefix parsing regex
    var BEGIN_TASK_REGEX = /^(\s*\[go] (?:Cancel t|T)ask: )(.*)/;
    var lineNumber = 0;

    consoleElement.on("click", ".toggle", function toggleSectionCollapse(e) {
      e.stopPropagation();
      e.preventDefault();

      var section = $(e.currentTarget).closest(".foldable-section");
      section.toggleClass("open");
    });

    if (!consoleElement.find(".section").length) {
      currentSection = addBlankSection(consoleElement);
    }

    function addBlankSection(element) {
      var section = c("dl", {class: "foldable-section open"})
      c(element[0], section);

      return $(section);
    }

    function onFinishSection(section) {
      if (!section.data("errored")) {
        section.removeClass("open");
      }
    }

    function detectError(section, prefix) {
      if ([Types.ALERT, Types.FAIL, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.data("errored", true);
      }

      if (Types.PASS === prefix) {
        section.attr("data-task-status", "passed").removeData("task-status");
      }

      if (Types.FAIL === prefix) {
        section.attr("data-task-status", "failed").removeData("task-status");
      }

      if (Types.JOB_PASS === prefix) {
        section.attr("data-job-status", "passed").removeData("job-status");
      }

      if (Types.JOB_FAIL === prefix) {
        section.attr("data-job-status", "failed").removeData("job-status");
      }
    }

    function adoptSection(section, prefix, line) { // TODO: name adoptSectionAndAddHeader?
      if ([Types.INFO, Types.ALERT].indexOf(prefix) > -1) {
        section.attr("data-type", "info");
      } else if ([Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1) {
        section.attr("data-type", "prep");
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "task");
      } else if ([Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "result");
      } else {
        section.attr("data-type", "info");
      }

      return insertHeader(section, prefix, line);
    }

    function parseSpecialLineContent(rawLineElement, prefix, line) {
      var parts;

      if (prefix === Types.TASK_START && line.match(BEGIN_TASK_REGEX)) {
        parts = line.match(BEGIN_TASK_REGEX);
        c(rawLineElement, parts[1], c("code", parts[2]));
      } else if (isExplicitEndBoundary(prefix)) {
        parts = line.match(/^(\s*\[go] (?:Current job|Task) status: )(.*)/)
        c(rawLineElement, parts[1], c("code", parts[2]));
      } else {
        if ("" === line) {
          c(rawLineElement, c("br"));
        } else {
          rawLineElement.innerHTML = ansi.ansi_to_html(line);
        }
      }
    }

    function insertHeader(section, prefix, line) {
      var header = c("dt", {"data-prefix": prefix});

      parseSpecialLineContent(header, prefix, line);
      c(section[0], header);
      return $(header);
    }

    function insertLine(section, prefix, line) {
      var output = c("dd", {"data-prefix": prefix});

      if (!section.data("multiline")) {
        section.prepend(c("a", {class: "fa toggle"}));
      }
      section.data("multiline", true);

      parseSpecialLineContent(output, prefix, line);
      c(section[0], output);

      return $(output);
    }

    function isPartOfSection(section, prefix, line) {
      if (section.data("type") === "info") {
        return [Types.INFO, Types.ALERT].indexOf(prefix) > -1;
      }

      if (section.data("type") === "prep") {
        return [Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1;
      }

      if (section.data("type") === "task") {
        if (prefix === Types.TASK_START) {
          return !line.match(BEGIN_TASK_REGEX);
        }
        return [Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1;
      }

      return false;
    }

    function isExplicitEndBoundary(prefix) {
      return [Types.PASS, Types.FAIL, Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1;
    }

    function closeSectionAndStartNext(section, container) {
      // close section and start a new one
      onFinishSection(section);

      return addBlankSection(container);
    }

    me.transform = function buildDomFromLogs(logLines) {
      var rawLine, match;

      for (var i = 0, prefix, line, timestamp, len = logLines.length; i < len; i++) {
        lineNumber++;
        rawLine = logLines[i];
        match = rawLine.match(re);

        if (match) {
          prefix = match[1];
          timestamp = match[2];
          line = $.trim(match[3] || "");

          detectError(currentSection, prefix);

          if (currentSection.is(":empty")) {
            lineCursor = adoptSection(currentSection, prefix, line);

            if (isExplicitEndBoundary(prefix)) {
              currentSection = closeSectionAndStartNext(currentSection, consoleElement);
            }
          } else if (isPartOfSection(currentSection, prefix, line)) {
            lineCursor = insertLine(currentSection, prefix, line);

            if (isExplicitEndBoundary(prefix)) {
              currentSection = closeSectionAndStartNext(currentSection, consoleElement);
            }
          } else {
            currentSection = closeSectionAndStartNext(currentSection, consoleElement);
            lineCursor = adoptSection(currentSection, prefix, line);
          }

          lineCursor.attr("data-line", lineNumber).prepend($("<span class='ts'>").text(timestamp));
        } else {
          c(currentSection[0], lineCursor = c("dt", rawLine));
          lineCursor = $(lineCursor);
        }
      }
    };
  }

  // export
  window.ConsoleParsingObserver = ConsoleParsingObserver;
  window.LogOutputTransformer = LogOutputTransformer;
})(jQuery, crel);
