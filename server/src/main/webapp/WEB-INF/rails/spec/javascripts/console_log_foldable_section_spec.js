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
(function ($, c, _) {
  "use strict";

  describe("FoldableSectionSpec", function FoldableSectionSpec() {
    var fs, lw, node, el, t = FoldableSection.Types;

    function reset() {
      node = document.createDocumentFragment();
      el = c("dl", {class: "foldable-section open"});
      fs = new FoldableSection.Cursor(node, el);
      lw = new FoldableSection.LineWriter();
      node.appendChild(el);
    }

    function detectPrefixesWhichArePartOfSection(section) {
      return _.filter(t, function (val, key) {
        return section.isPartOfSection(val);
      }).sort();
    }

    beforeEach(reset);

    it("assignType", function () {
      fs.assignType(t.INFO);
      expect(fs.type()).toBe("info");

      fs.assignType(t.TASK_START);
      expect(fs.type()).toBe("task");

      fs.assignType(t.PREP);
      expect(fs.type()).toBe("prep");

      fs.assignType(t.PUBLISH);
      expect(fs.type()).toBe("publish");

      fs.assignType(t.COMPLETED);
      expect(fs.type()).toBe("end");

      fs.assignType(t.PUBLISH_ERR);
      expect(fs.type()).toBe("publish");

      fs.assignType(t.CANCEL_TASK_START);
      expect(fs.type()).toBe("cancel");

      fs.assignType(t.PREP_ERR);
      expect(fs.type()).toBe("prep");

      fs.assignType(t.CANCEL_TASK_FAIL);
      expect(fs.type()).toBe("cancel");

      fs.assignType(t.OUT);
      expect(fs.type()).toBe("task");

      fs.assignType(t.ERR);
      expect(fs.type()).toBe("task");

      fs.assignType(t.CANCEL_TASK_PASS);
      expect(fs.type()).toBe("cancel");

      fs.assignType(t.JOB_PASS);
      expect(fs.type()).toBe("result");

      fs.assignType(t.PASS);
      expect(fs.type()).toBe("task");

      fs.assignType(t.JOB_FAIL);
      expect(fs.type()).toBe("result");

      fs.assignType(t.FAIL);
      expect(fs.type()).toBe("task");
    });

    it("isPartOfSection info", function () {
      el.priv.type = "info";
      expect(_.isEqual([t.INFO], detectPrefixesWhichArePartOfSection(fs))).toBe(true);
    });

    it("isPartOfSection prep", function () {
      el.priv.type = "prep";
      expect(_.isEqual([t.PREP, t.PREP_ERR].sort(), detectPrefixesWhichArePartOfSection(fs))).toBe(true);
    });

    it("isPartOfSection publish", function () {
      el.priv.type = "publish";
      expect(_.isEqual([t.PUBLISH, t.PUBLISH_ERR].sort(), detectPrefixesWhichArePartOfSection(fs))).toBe(true);
    });

    it("isPartOfSection task", function () {
      el.priv.type = "task";
      expect(_.isEqual([t.OUT, t.ERR, t.PASS, t.FAIL, t.CANCELLED].sort(), detectPrefixesWhichArePartOfSection(fs))).toBe(true);
    });

    it("isPartOfSection cancel", function () {
      el.priv.type = "cancel";
      expect(_.isEqual([t.OUT, t.ERR, t.CANCEL_TASK_PASS, t.CANCEL_TASK_FAIL].sort(), detectPrefixesWhichArePartOfSection(fs))).toBe(true);
    });

    it("isPartOfSection result", function () {
      el.priv.type = "result";
      expect(detectPrefixesWhichArePartOfSection(fs).length).toBe(0);
    });

    it("isPartOfSection end", function () {
      el.priv.type = "end";
      expect(detectPrefixesWhichArePartOfSection(fs).length).toBe(0);
    });

    it("markMultiline prepends the toggle widget exactly once", function () {
      expect(!el.body).toBe(true); // initially, there is no body wrapping element

      el.appendChild(c("dt", "a message"));
      fs.markMultiline();

      // the widget should be the first child
      expect($(el).find(".toggle:first-child").length).toBe(1);

      // the wrapper element should be set
      expect(el.body instanceof Node).toBe(true);

      // subsequent invocations should not add more widgets; markMultiline() must be idempotent
      fs.markMultiline();
      expect($(el).find(".toggle").length).toBe(1);
    });

    it("closeAndStartNew collapses old section if no errors detected and creates a new sibling section", function () {
      var newSection = fs.closeAndStartNew(node);

      expect(!$(el).is(".open")).toBe(true);
      expect($(newSection.element()).is(".open")).toBe(true);

      expect(node.childNodes[0]).toBe(el);
      expect(node.childNodes[1]).toBe(newSection.element());
    });

    it("closeAndStartNew leaves old section expanded if errors detected", function () {
      el.priv.errored = true;
      fs.closeAndStartNew(node);

      expect($(el).is(".open")).toBe(true);
    });

    it("detectStatus recognizes failure states", function () {
      fs.detectStatus(t.CANCELLED);
      expect(el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-task-status-cancelled")).toBe(true);

      reset();

      fs.detectStatus(t.FAIL);
      expect(el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-task-status-failed")).toBe(true);

      reset();

      fs.detectStatus(t.JOB_FAIL);
      expect(el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-job-status-failed")).toBe(true);

      reset();

      fs.detectStatus(t.CANCEL_TASK_FAIL);
      expect(el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-task-status-failed")).toBe(true);

      reset();

      fs.assignType(t.TASK_START);
      fs.detectStatus(t.CANCEL_TASK_START);
      expect(el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-task-status-cancelled")).toBe(true);
    });

    it("detectStatus recognizes success states", function () {
      fs.detectStatus(t.PASS);
      expect(!el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-task-status-passed")).toBe(true);

      reset();

      fs.detectStatus(t.JOB_PASS);
      expect(!el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-job-status-passed")).toBe(true);

      reset();

      fs.detectStatus(t.CANCEL_TASK_PASS);
      expect(!el.priv.errored).toBe(true);
      expect($(el).is(".log-fs-task-status-passed")).toBe(true);
    });

    it("isExplicitEndBoundary only identifies prefixes are are terminal", function () {
      expect(_.reduce([t.PASS, t.FAIL, t.CANCELLED, t.JOB_PASS, t.JOB_FAIL, t.CANCEL_TASK_PASS, t.CANCEL_TASK_FAIL], function (result, prefix) {
        result = result && fs.isExplicitEndBoundary(prefix);
        return result;
      }, true)).toBe(true);

      expect(_.reduce([[t.INFO, t.PREP, t.PREP_ERR, t.TASK_START, t.OUT, t.ERR, t.CANCEL_TASK_START]], function (result, prefix) {
        result = result || fs.isExplicitEndBoundary(prefix);
        return result;
      }, false)).toBe(false);
    });

    it("LineWriter insertPlain", function () {
      var h = $($(lw.insertPlain(fs, "00:00:00.000", "Starting build")));
      expect(h.is("dd")).toBe(true);
      expect(h.text()).toBe("Starting build");
    });

    it("LineWriter insertHeader", function () {
      var h = $($(lw.insertHeader(fs, t.INFO, "00:00:00.000", "Starting build")));
      expect(h.is("dt.log-fs-line-INFO")).toBe(true);
    });

    it("LineWriter insertPlain handles ANSI color", function () {
      var l = $(lw.insertPlain(fs, "00:00:00.000", "Starting \u001B[1;33;40mcolor"));
      expect(l.find(".ansi-bright-yellow-fg.ansi-black-bg").length).toBe(1);
      expect(l.find(".ansi-bright-yellow-fg.ansi-black-bg").text()).toBe("color");
    });

    it("LineWriter insertContent/Header handles ANSI color", function () {
      fs.markMultiline(); // insertContent() requires a section body element
      var l = $(lw.insertContent(fs, t.OUT, "00:00:00.000", "Starting \u001B[1;33;40mcolor"));
      expect(l.find(".ansi-bright-yellow-fg.ansi-black-bg").length).toBe(1);
      expect(l.find(".ansi-bright-yellow-fg.ansi-black-bg").text()).toBe("color");
    });

    it("LineWriter formats exit code", function () {
      var h = $($(lw.insertHeader(fs, t.INFO, "00:00:00.000", "Starting build")));
      lw.markWithAnnotations(fs, {exitCode: 127});
      expect(h.find(".log-fs-exitcode").text()).toBe("exited: 127");
    });

    it("LineWriter parses exit code from status line if available", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.FAIL, "00:00:00.000", "[go] Task status: failed (exit code: 1)")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      expect(h.find(".log-fs-exitcode").text()).toBe("exited: 1");
      expect(l.text()).toBe("[go] Task status: failed, exited: 1");
    });

    it("LineWriter formats durations from milliseconds into a human-friendly stamp", function () {
      var h = $($(lw.insertHeader(fs, t.INFO, "00:00:00.000", "Starting build")));
      lw.markWithAnnotations(fs, {duration: 4998315});
      expect(h.find(".log-fs-duration").text()).toBe("took: 1h 23m 18.315s");
    });

    it("LineWriter parses durations from status line if available", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.PASS, "00:00:00.000", "[go] Task status: passed (8675309 ms)")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      expect(h.find(".log-fs-duration").text()).toBe("took: 2h 24m 35.309s");
      expect(l.text()).toBe("[go] Task status: passed, took: 2h 24m 35.309s");
    });

    it("LineWriter parses both durations and exit code from status line if available", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.PASS, "00:00:00.000", "[go] Task status: passed (8675309 ms) (exit code: 127)")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      expect(h.find(".log-fs-duration").text()).toBe("took: 2h 24m 35.309s");
      expect(h.find(".log-fs-exitcode").text()).toBe("exited: 127");
      expect(l.text()).toBe("[go] Task status: passed, took: 2h 24m 35.309s, exited: 127");
    });

    it("LineWriter just prints if there are no annotations", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.PASS, "00:00:00.000", "[go] Task status: passed")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      expect(h.find(".log-fs-duration").length).toBe(0);
      expect(l.text()).toBe("[go] Task status: passed");
    });

    it("LineWriter insertContent", function () {
      fs.markMultiline(); // insertContent() requires a section body element
      var l = $(lw.insertContent(fs, t.OUT, "00:00:00.000", "Starting build"));
      expect(l.is("dd.log-fs-line-OUT")).toBe(false);
    });

    it("LineWriter parses task", function () {
      var h = $(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: about to happen"));
      expect(h.find("code").length).toBe(1);
      expect(h.find("code").text()).toBe("about to happen");
    });

    it("LineWriter parses job status", function () {
      var h = $(lw.insertHeader(fs, t.JOB_PASS, "00:00:00.000", "[go] Current job status: passed"));
      expect(h.find("code").length).toBe(1);
      expect(h.find("code").text()).toBe("passed");
    });

    it("LineWriter parses task status", function () {
      var h = $(lw.insertHeader(fs, t.FAIL, "00:00:00.000", "[go] Task status: failed"));
      expect(h.find("code").length).toBe(1);
      expect(h.find("code").text()).toBe("failed");
    });

    it("LineWriter parses cancel task", function () {
      var h = $(lw.insertHeader(fs, t.CANCEL_TASK_START, "00:00:00.000", "[go] On Cancel Task: not so great"));
      expect(h.find("code").length).toBe(1);
      expect(h.find("code").text()).toBe("not so great");
    });

    it("LineWriter handles empty lines", function () {
      var h = $(lw.insertHeader(fs, t.INFO, "00:00:00.000", ""));
      expect(h.text()).toBe("\n");
    });

  });

})(jQuery, crel, _);
