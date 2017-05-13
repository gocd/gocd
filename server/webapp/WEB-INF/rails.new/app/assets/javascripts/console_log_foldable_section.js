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
    INFO: "##", COMPLETED: "ex",
    PREP: "pr", PREP_ERR: "pe",
    PUBLISH: "ar", PUBLISH_ERR: "ae",
    TASK_START: "!!", OUT: "&1", ERR: "&2", PASS: "?0", FAIL: "?1", CANCELLED: "^C",
    CANCEL_TASK_START: "!x", CANCEL_TASK_PASS: "x0", CANCEL_TASK_FAIL: "x1",
    JOB_PASS: "j0", JOB_FAIL: "j1"
  };

  var ReverseTypes = _.invert(Types);

  function LineWriter() {

    var cmd_re = /^(\s*\[go] (?:On Cancel )?Task: )(.*)/,
        status_re = /^(\s*\[go] (?:Current job|Task) status: )(?:(\w+)(?: \((\d+) ms\))?(?: \(exit code: (\d+)\))?.*)$/,
        ansi = new AnsiUp(),
        formatter = new AnsiFormatter();

    function isTaskLine(prefix) {
      return [Types.TASK_START, Types.CANCEL_TASK_START].indexOf(prefix) > -1;
    }

    function isStatusLine(prefix) {
      return [Types.PASS, Types.FAIL, Types.CANCELLED, Types.JOB_PASS, Types.JOB_FAIL, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1;
    }

    function formatContent(cursor, node, prefix, line) {
      var parts, duration, result, exitCode;

      if (isTaskLine(prefix)) {
        parts = line.match(cmd_re);
        c(node, parts[1], c("code", parts[2]));
      } else if (isStatusLine(prefix)) {
        parts = line.match(status_re);
        if (parts) {
          result = parts[2];

          if (parts[3] && !isNaN(parseInt(parts[3], 10))) {
            duration = parseInt(parts[3], 10);
            cursor.annotate("duration", duration);

            result += ", took: " + humanizeMilliseconds(duration);
          }

          if (parts[4] && !isNaN(parseInt(parts[4], 10))) {
            exitCode = parseInt(parts[4], 10);
            cursor.annotate("exitCode", exitCode);

            result += ", exited: " + exitCode;
          }

          c(node, parts[1], c("code", result));
        } else {
          c(node, line); // Usually the end of an onCancel task
        }
      } else {
        if ("" === $.trim(line)) {
          c(node, "\n");
        } else {
          c(node, ansi.ansi_to(line, formatter));
        }
      }
    }

    function humanizeMilliseconds(duration) {
      var d = moment.duration(duration, "ms");
      return d.humanizeForGoCD();
    }

    function insertPlain(cursor, timestamp, line) {
      var node = c("dd", {class: "log-fs-line", "data-timestamp": timestamp}, ansi.ansi_to(line, formatter));

      cursor.write(node);
      return node;
    }

    function insertHeader(cursor, prefix, timestamp, line) {
      var node = c("dt", {"class": "log-fs-line log-fs-line-" + ReverseTypes[prefix], "data-timestamp": timestamp});

      formatContent(cursor, node, prefix, line);
      cursor.writeHeader(node);
      return node;
    }

    function insertContent(cursor, prefix, timestamp, line) {
      var node = c("div", {"class": "log-fs-line log-fs-line-" + ReverseTypes[prefix], "data-timestamp": timestamp});

      formatContent(cursor, node, prefix, line);
      cursor.writeBody(node);
      return node;
    }

    function markWithAnnotations(cursor, annotations) {
      var node = cursor.header();

      if (!node) {
        return;
      }

      if ("number" === typeof annotations.duration) {
        node.appendChild(c("span", {class: "log-fs-duration"}, "took: " + humanizeMilliseconds(annotations.duration)));
      }

      if ("number" === typeof annotations.exitCode) {
        node.appendChild(c("span", {class: "log-fs-exitcode"}, "exited: " + annotations.exitCode));
      }
    }

    this.markWithAnnotations = markWithAnnotations;
    this.insertHeader = insertHeader;
    this.insertContent = insertContent;
    this.insertPlain = insertPlain;
  }

  function SectionCursor(node, section) {
    var cursor, self = this;

    if (!section) section = blankSectionElement();

    if (node instanceof $) node = node[0];
    if (section instanceof $) section = section[0];

    if ("undefined" === typeof section.priv) section.priv = {};

    // the internal cursor reference is the Node object to append new content.
    // sometimes this is the section element, and sometimes it is the "node" argument,
    // which may be a DocumentFragment that is a continuation of an unclosed section.
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
      if (section.body) newNode.body = section.body;
      return new SectionCursor(newNode, section);
    }

    function write(childNode) {
      cursor.appendChild(childNode);
    }

    function writeHeader(childNode) {
      section.priv.header = childNode;
      cursor.appendChild(childNode);
    }

    function writeBody(childNode) {
      cursor.body.appendChild(childNode);
    }

    function annotate(key, value) {
      if (!section.priv.meta) {
        section.priv.meta = {};
      }
      section.priv.meta[key] = value;
    }

    function type() {
      return section.priv.type;
    }

    function getSection() {
      return section;
    }

    function getHeader() {
      return section.priv.header;
    }

    function markMultiline() {
      if (!section.priv.multiline) {
        section.body = cursor.body = c("dd", {class: "fs-multiline"});
        cursor.appendChild(cursor.body);
        section.insertBefore(c("a", {class: "fa toggle"}), section.childNodes[0]);
        section.priv.multiline = true;
      }
    }

    function onFinishSection(writer) {
      if (!section.priv.errored) {
        section.classList.remove("open");
      }

      if ("undefined" !== typeof section.priv.meta && writer) {
        writer.markWithAnnotations(self, section.priv.meta);
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
      } else if (Types.PUBLISH_ERR === prefix) { // we only care if it failed, so no need to detect a success status
        section.classList.add("log-fs-status");
        section.classList.add("log-fs-publish-failed");
        section.priv.errored = true;
      }
    }

    function assignType(prefix) {
      if (Types.INFO === prefix) {
        section.priv.type = "info";
      } else if ([Types.PREP, Types.PREP_ERR].indexOf(prefix) > -1) {
        section.priv.type = "prep";
      } else if ([Types.PUBLISH, Types.PUBLISH_ERR].indexOf(prefix) > -1) {
        section.priv.type = "publish";
      } else if ([Types.TASK_START, Types.OUT, Types.ERR, Types.PASS, Types.FAIL, Types.CANCELLED].indexOf(prefix) > -1) {
        section.priv.type = "task";
      } else if ([Types.CANCEL_TASK_START, Types.CANCEL_TASK_PASS, Types.CANCEL_TASK_FAIL].indexOf(prefix) > -1) {
        section.priv.type = "cancel";
      } else if ([Types.JOB_PASS, Types.JOB_FAIL].indexOf(prefix) > -1) {
        section.priv.type = "result";
      } else if (Types.COMPLETED === prefix) {
        section.priv.type = "end";
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

      if (section.priv.type === "publish") {
        return [Types.PUBLISH, Types.PUBLISH_ERR].indexOf(prefix) > -1;
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

    function closeAndStartNew(parentNode, writer) {
      // close section and start a new one
      onFinishSection(writer);

      return addAnotherCursor(parentNode);
    }

    // public API

    this.detectStatus = detectStatus;
    this.markMultiline = markMultiline;

    this.type = type;
    this.assignType = assignType;
    this.annotate = annotate;
    this.isPartOfSection = isPartOfSection;
    this.isExplicitEndBoundary = isExplicitEndBoundary;
    this.closeAndStartNew = closeAndStartNew;

    this.write = write;
    this.writeHeader = writeHeader;
    this.writeBody = writeBody;

    this.element = getSection;
    this.header = getHeader;
    this.cloneTo = cloneTo;
  }

  window.FoldableSection = {
    Types: Types,
    Cursor: SectionCursor,
    LineWriter: LineWriter
  };

})(jQuery, crel, _);
