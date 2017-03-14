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

  var Types = {
    INFO: "##",
    PREP: "pr", PREP_ERR: "pe",
    TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1", CANCELLED: "^C",
    CANCEL_TASK_START: "!x", CANCEL_TASK_PASS: "x0", CANCEL_TASK_FAIL: "x1",
    JOB_PASS: "j0", JOB_FAIL: "j1"
  };

  function LineWriter() {

    var cmd_re = /^(\s*\[go] (?:On Cancel )?Task: )(.*)/,
        status_re = /^(\s*\[go] (?:Current job|Task) status: )(.*)/,
        ansi = new AnsiUp();
    ansi.use_classes = true;

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
          c(node, c("br"));
        } else {
          node.innerHTML = ansi.ansi_to_html(line);
        }
      }
    }

    function insertBasic(cursor, line) {
      var output = c("dd");

      output.innerHTML = ansi.ansi_to_html(line);
      cursor.write(output);
      return output;
    }

    function insertHeader(cursor, prefix, line) {
      var header = c("dt", {"data-prefix": prefix});

      formatContent(header, prefix, line);
      cursor.write(header);
      return header;
    }

    function insertLine(cursor, prefix, line) {
      var output = c("dd", {"data-prefix": prefix});

      formatContent(output, prefix, line);
      cursor.write(output);

      return output;
    }

    this.insertHeader = insertHeader;
    this.insertLine = insertLine;
    this.insertBasic = insertBasic;
  }

  function SectionCursor(node, section) {
    var cursor, me = this;

    if (!section) section = $(blankSectionElement());

    if (node instanceof $) node = node[0];
    if (!(section instanceof $)) section = $(section);

    // the internal cursor reference is the Node object to append new content.
    // sometimes this is the section element, and sometimes it is the "node" argument,
    // which may be a DocumentFragment that
    cursor = $.contains(node, section[0]) ? section[0] : node;

    function blankSectionElement() {
      return c("dl", {class: "foldable-section open"});
    }

    function addAnotherCursor(parentNode) {
      if (parentNode instanceof $) parentNode = parentNode[0]; // parentNode may be a real element or document fragment

      var element = blankSectionElement();
      parentNode.appendChild(element);

      return new SectionCursor(parentNode, $(element));
    }

    function cloneTo(newNode) {
      return new SectionCursor(newNode, section);
    }

    function write(childNode) {
      cursor.appendChild(childNode);
    }

    function hasType() {
      return section[0].hasAttribute("data-type");
    }

    function getSection() {
      return section;
    }

    function markMultiline() {
      if (!section.data("multiline")) {
        section.prepend(c("a", {class: "fa toggle"}));
        section.data("multiline", true);
      }
    }

    function onFinishSection() {
      if (!section.data("errored")) {
        section.removeClass("open");
      }
    }

    function detectStatus(prefix) {
      // either and explicit cancelled status or an implicit boundary (i.e. start of a cancel-task)
      if (prefix === Types.CANCELLED || (section.data("type") === "task" && Types.CANCEL_TASK_START === prefix)) {
        // While "canceled" and "cancelled" are both correct spellings and are inconsistently used in our codebase.
        // However, we should use the one that matches the JobResult enum, which is "cancelled"
        section.attr("data-task-status", "cancelled").removeData("task-status");
        section.data("errored", true);
      } else if (Types.PASS === prefix || Types.CANCEL_TASK_PASS === prefix) {
        section.attr("data-task-status", "passed").removeData("task-status");
      } else if (Types.FAIL === prefix || Types.CANCEL_TASK_FAIL === prefix) {
        section.attr("data-task-status", "failed").removeData("task-status");
        section.data("errored", true);
      } else if (Types.JOB_PASS === prefix) {
        section.attr("data-job-status", "passed").removeData("job-status");
      } else if (Types.JOB_FAIL === prefix) {
        section.attr("data-job-status", "failed").removeData("job-status");
        section.data("errored", true);
      }
    }

    function assignType(prefix) {
      if (Types.INFO === prefix) {
        section.attr("data-type", "info");
      } else if ([Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1) {
        section.attr("data-type", "prep");
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL, Types.CANCELLED].indexOf(prefix) > -1) {
        section.attr("data-type", "task");
      } else if ([Types.CANCEL_TASK_START, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "cancel");
      } else if ([Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.attr("data-type", "result");
      } else {
        section.attr("data-type", "info");
      }

      section.removeData("data-type");
    }

    function isPartOfSection(prefix) {
      if (section.data("type") === "info") {
        return Types.INFO === prefix;
      }

      if (section.data("type") === "prep") {
        return [Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1;
      }

      if (section.data("type") === "task") {
        return [Types.OUT, Types.ERR, Types.PASS, Types.FAIL, Types.CANCELLED].indexOf(prefix) > -1;
      }

      if (section.data("type") === "cancel") {
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

    this.hasType = hasType;
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

})(jQuery, crel);
