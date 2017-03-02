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
      // Might be a bug with the "beforeSend" AJAX option in this version of jQuery.
      // When acquireLock() returns false, $.ajax() returns false, and thus fails
      // to attach the done() and always() callbacks.
      //
      // Thus, just return early if acquireLock() returns false. this might be fixed
      // by upgrading jQuery.
      if (!acquireLock()) return;

      $.ajax({
        url:  url,
        type: "GET",
        dataType: "text",
        data: {"startLineNumber": startLineNumber}
      }).done(function processLogOutput(data, status, xhr) {
        var lineSet, slice, nextLine = JSON.parse(xhr.getResponseHeader("X-JSON") || "[]")[0];

        if (nextLine !== startLineNumber) {
          lineSet = data.match(/^.*([\n\r]+|$)/gm);

          while (lineSet.length) {
            slice = lineSet.splice(0, 1000);
            transformer.transform(slice);
          }
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

  function Foldable() {
    var Types = {
      INFO: "##", ALERT: "@@",
      PREP: "pr", PREP_ERR: "pe",
      TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1",
      CANCEL_TASK_START: "!x", CANCEL_TASK_PASS: "x0", CANCEL_TASK_FAIL: "x1",
      JOB_PASS: "j0", JOB_FAIL: "j1"
    };

    var ansi = new AnsiUp();

    ansi.use_classes = true;

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
      if ([Types.FAIL, Types.JOB_FAIL, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1) {
        section.data("errored", true);
      }

      // canceling a build generally leaves no task status, so infer it
      // by detecting the CANCEL_TASK_START prefix
      if (section.data("type") === "task" && Types.CANCEL_TASK_START === prefix) {
        // No, "cancelled" is not misspelled. We use both the British and American spellings inconsistently in our codebase,
        // but we should go with whatever JobResult.Cancelled is, which uses the British spelling "cancelled"
        section.attr("data-task-status", "cancelled").removeData("task-status");
        section.data("errored", true);
      }

      if (Types.PASS === prefix || Types.CANCEL_TASK_PASS === prefix) {
        section.attr("data-task-status", "passed").removeData("task-status");
      }

      if (Types.FAIL === prefix || Types.CANCEL_TASK_FAIL === prefix) {
        section.attr("data-task-status", "failed").removeData("task-status");
      }

      if (Types.JOB_PASS === prefix) {
        section.attr("data-job-status", "passed").removeData("job-status");
      }

      if (Types.JOB_FAIL === prefix) {
        section.attr("data-job-status", "failed").removeData("job-status");
      }
    }

    function assignSection(section, prefix) {
      section.removeData("data-type");

      if (Types.INFO ===prefix) {
        section.attr("data-type", "info");
      } else if ([Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1) {
        section.attr("data-type", "prep");
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "task");
      } else if (Types.CANCEL_TASK_START === prefix) {
        section.attr("data-type", "cancel");
      } else if ([Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "result");
      } else {
        section.attr("data-type", "info");
      }
    }

    function parseSpecialLineContent(rawLineElement, prefix, line) {
      var parts,
          cmd_re = /^(\s*\[go] (?:On Cancel )?Task: )(.*)/,
          status_re = /^(\s*\[go] (?:Current job|Task) status: )(.*)/;

      if ([Types.TASK_START, Types.CANCEL_TASK_START].indexOf(prefix) > -1) {
        parts = line.match(cmd_re);
        c(rawLineElement, parts[1], c("code", parts[2]));
      } else if (isExplicitEndBoundary(prefix)) {
        parts = line.match(status_re)
        if (parts) {
          c(rawLineElement, parts[1], c("code", parts[2]));
        } else {
          c(rawLineElement, line); // Usually the end of an onCancel task
        }
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
        return [Types.OUT, Types.ERR, Types.PASS, Types.FAIL].indexOf(prefix) > -1;
      }

      if (section.data("type") === "cancel") {
        return [Types.OUT, Types.ERR, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
      }

      return false;
    }

    function isExplicitEndBoundary(prefix) {
      return [Types.PASS, Types.FAIL, Types.JOB_PASS, Types.JOB_FAIL, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
    }

    function closeSectionAndStartNext(section, container) {
      // close section and start a new one
      onFinishSection(section);

      return addBlankSection(container);
    }

    this.detectError = detectError;
    this.isExplicitEndBoundary = isExplicitEndBoundary;

    this.addBlankSection = addBlankSection;
    this.assignSection = assignSection;
    this.isPartOfSection = isPartOfSection;
    this.onFinishSection = onFinishSection;
    this.closeSectionAndStartNext = closeSectionAndStartNext;

    this.insertHeader = insertHeader;
    this.insertLine = insertLine;
  }

  function LogOutputTransformer(consoleElement, fm /* formatter */) {
    var me = this;
    var currentSection, currentLine;

    var re = /^([^|]{2})\|(\d\d:\d\d:\d\d\.\d\d\d) (.*)/; // parses prefix, timestamp, and line content
    var legacy = /^(\d\d:\d\d:\d\d\.\d\d\d )?(.*)/; // timestamps were not guaranteed to precede content in the old format

    var lineNumber = 0;

    consoleElement.on("click", ".toggle", function toggleSectionCollapse(e) {
      e.stopPropagation();
      e.preventDefault();

      var section = $(e.currentTarget).closest(".foldable-section");
      section.toggleClass("open");
    });

    if (!consoleElement.find(".section").length) {
      currentSection = fm.addBlankSection(consoleElement);
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
          line = match[3] || "";

          fm.detectError(currentSection, prefix);

          if (currentSection.is(":empty")) {
            fm.assignSection(currentSection, prefix);
            currentLine = fm.insertHeader(currentSection, prefix, line);

            if (fm.isExplicitEndBoundary(prefix)) {
              currentSection = fm.closeSectionAndStartNext(currentSection, consoleElement);
            }
          } else if (fm.isPartOfSection(currentSection, prefix, line)) {
            currentLine = fm.insertLine(currentSection, prefix, line);

            if (fm.isExplicitEndBoundary(prefix)) {
              currentSection = fm.closeSectionAndStartNext(currentSection, consoleElement);
            }
          } else {
            currentSection = fm.closeSectionAndStartNext(currentSection, consoleElement);

            fm.assignSection(currentSection, prefix);
            currentLine = fm.insertHeader(currentSection, prefix, line);
          }
        } else {

          if (match = rawLine.match(legacy)) {
            timestamp = $.trim(match[1] || "");
            line = match[2] || "";
          } else {
            timestamp = "", line = rawLine;
          }

          c(currentSection[0], currentLine = c("dt", line));
          currentLine = $(currentLine);
        }

        currentLine.attr("data-line", lineNumber).prepend(c("span", {class: "ts"}, timestamp));
      }
    };
  }

  // export
  window.ConsoleParsingObserver = ConsoleParsingObserver;
  window.LogOutputTransformer = LogOutputTransformer;
  window.Foldable = Foldable;
})(jQuery, crel);
