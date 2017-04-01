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
;(function ($, c, _) {
  "use strict";

  var Types = {
    INFO: "##",
    PREP: "pr", PREP_ERR: "pe",
    TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1", CANCELLED: "^C",
    CANCEL_TASK_START: "!x", CANCEL_TASK_PASS: "x0", CANCEL_TASK_FAIL: "x1",
    JOB_PASS: "j0", JOB_FAIL: "j1"
  };

  var ReverseTypes = _.invert(Types);

  function LineWriter() {

    var cmd_re = /^(\s*\[go] (?:On Cancel )?Task: )(.*)/,
        status_re = /^(\s*\[go] (?:Current job|Task) status: )(.*)/,
        ansi = new ANSIColors();

    function isTaskLine(prefix) {
      return [Types.TASK_START, Types.CANCEL_TASK_START].indexOf(prefix) > -1;
    }

    function isStatusLine(prefix) {
      return [Types.PASS, Types.FAIL, Types.CANCELLED, Types.JOB_PASS, Types.JOB_FAIL, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
    }

    function formatContent(node, prefix, line) {
      var parts;

      if (isTaskLine(prefix)) {
        parts = line.match(cmd_re);
        c(node, parts[1], c("code", parts[2]));
      } else if (isStatusLine(prefix)) {
        parts = line.match(status_re);
        if (parts) {
          c(node, parts[1], c("code", parts[2]));
        } else {
          c(node, line); // Usually the end of an onCancel task
        }
      } else {
        if ("" === $.trim(line)) {
          c(node, "\n");
        } else {
          c(node, ansi.process(line));
        }
      }
    }

    function insertBasic(cursor, line) {
      var output = c("dd", {class: "log-fs-line"}, ansi.process(line));

      cursor.write(output);
      return output;
    }

    function insertHeader(cursor, prefix, line) {
      var header = c("dt", {"class": "log-fs-line log-fs-line-" + ReverseTypes[prefix]});

      formatContent(header, prefix, line);
      cursor.write(header);
      return header;
    }

    function insertLine(cursor, prefix, line) {
      var output = c("dd", {"class": "log-fs-line log-fs-line-" + ReverseTypes[prefix]});

      formatContent(output, prefix, line);
      cursor.write(output);

      return output;
    }

    this.insertHeader = insertHeader;
    this.insertLine = insertLine;
    this.insertBasic = insertBasic;
  }

  function SectionCursor(node, section) {
    var cursor;

    if (!section) section = blankSectionElement();

    if (node instanceof $) node = node[0];
    if (section instanceof $) section = section[0];

    if ("undefined" === typeof section.priv) section.priv = {};

    // the internal cursor reference is the Node object to append new content.
    // sometimes this is the section element, and sometimes it is the "node" argument,
    // which may be a DocumentFragment that
    cursor = $.contains(node, section) ? section : node;

    function blankSectionElement() {
      return c("dl", {class: "foldable-section open"});
    }

    function addAnotherCursor(parentNode) {
      if (parentNode instanceof $) parentNode = parentNode[0]; // parentNode may be a real element or document fragment

      var element = blankSectionElement();
      parentNode.appendChild(element);

      return new SectionCursor(parentNode, element);
    }

    function cloneTo(newNode) {
      return new SectionCursor(newNode, section);
    }

    function write(childNode) {
      cursor.appendChild(childNode);
    }

    function type() {
      return section.priv.type;
    }

    function getSection() {
      return section;
    }

    function markMultiline() {
      if (!section.priv.multiline) {
        section.insertBefore(c("a", {class: "fa toggle"}), section.childNodes[0]);
        section.priv.multiline = true;
      }
    }

    function onFinishSection() {
      if (!section.priv.errored) {
        section.classList.remove("open");
      }
    }

    function detectStatus(prefix) {
      // either and explicit cancelled status or an implicit boundary (i.e. start of a cancel-task)
      if (prefix === Types.CANCELLED || (section.priv.type === "task" && Types.CANCEL_TASK_START === prefix)) {
        // While "canceled" and "cancelled" are both correct spellings and are inconsistently used in our codebase.
        // However, we should use the one that matches the JobResult enum, which is "cancelled"
        section.classList.add("log-fs-status");
        section.classList.add("log-fs-task-status-cancelled");
        section.priv.errored = true;
      } else if (Types.PASS === prefix || Types.CANCEL_TASK_PASS === prefix) {
        section.classList.add("log-fs-status");
        section.classList.add("log-fs-task-status-passed");
      } else if (Types.FAIL === prefix || Types.CANCEL_TASK_FAIL === prefix) {
        section.classList.add("log-fs-status");
        section.classList.add("log-fs-task-status-failed");
        section.priv.errored = true;
      } else if (Types.JOB_PASS === prefix) {
        section.classList.add("log-fs-status");
        section.classList.add("log-fs-job-status-passed");
      } else if (Types.JOB_FAIL === prefix) {
        section.classList.add("log-fs-status");
        section.classList.add("log-fs-job-status-failed");
        section.priv.errored = true;
      }
    }

    function assignType(prefix) {
      if (Types.INFO === prefix) {
        section.priv.type = "info";
      } else if ([Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1) {
        section.priv.type = "prep";
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL, Types.CANCELLED].indexOf(prefix) > -1) {
        section.priv.type = "task";
      } else if ([Types.CANCEL_TASK_START, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1) {
        section.priv.type = "cancel";
      } else if ([Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.priv.type = "result";
      } else {
        section.priv.type = "info";
      }

      section.classList.add("log-fs-type");
      section.classList.add("log-fs-type-" + section.priv.type);
    }

    function isPartOfSection(prefix) {
      if (section.priv.type === "info") {
        return Types.INFO === prefix;
      }

      if (section.priv.type === "prep") {
        return [Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1;
      }

      if (section.priv.type === "task") {
        return [Types.OUT, Types.ERR, Types.PASS, Types.FAIL, Types.CANCELLED].indexOf(prefix) > -1;
      }

      if (section.priv.type === "cancel") {
        return [Types.OUT, Types.ERR, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
      }

      return false;
    }

    function isExplicitEndBoundary(prefix) {
      return [Types.PASS, Types.FAIL, Types.CANCELLED, Types.JOB_PASS, Types.JOB_FAIL, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
    }

    function closeAndStartNew(parentNode) {
      // close section and start a new one
      onFinishSection();

      return addAnotherCursor(parentNode);
    }

    // public API

    this.detectStatus = detectStatus;
    this.markMultiline = markMultiline;

    this.type = type;
    this.assignType = assignType;
    this.isPartOfSection = isPartOfSection;
    this.isExplicitEndBoundary = isExplicitEndBoundary;
    this.closeAndStartNew = closeAndStartNew;

    this.write = write;

    this.element = getSection;
    this.cloneTo = cloneTo;
  }

  window.FoldableSection = {
    Types: Types,
    Cursor: SectionCursor,
    LineWriter: LineWriter
  };

})(jQuery, crel, _);
