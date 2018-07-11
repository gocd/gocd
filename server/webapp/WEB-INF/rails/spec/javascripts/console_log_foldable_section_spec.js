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
;(function($, c, _) {
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
      return _.filter(t, function(val, key) {
        return section.isPartOfSection(val);
      }).sort();
    }

    beforeEach(reset);

    it("assignType", function () {
      fs.assignType(t.INFO);
      assertEquals("info", fs.type());

      fs.assignType(t.TASK_START);
      assertEquals("task", fs.type());

      fs.assignType(t.PREP);
      assertEquals("prep", fs.type());

      fs.assignType(t.PUBLISH);
      assertEquals("publish", fs.type());

      fs.assignType(t.COMPLETED);
      assertEquals("end", fs.type());

      fs.assignType(t.PUBLISH_ERR);
      assertEquals("publish", fs.type());

      fs.assignType(t.CANCEL_TASK_START);
      assertEquals("cancel", fs.type());

      fs.assignType(t.PREP_ERR);
      assertEquals("prep", fs.type());

      fs.assignType(t.CANCEL_TASK_FAIL);
      assertEquals("cancel", fs.type());

      fs.assignType(t.OUT);
      assertEquals("task", fs.type());

      fs.assignType(t.ERR);
      assertEquals("task", fs.type());

      fs.assignType(t.CANCEL_TASK_PASS);
      assertEquals("cancel", fs.type());

      fs.assignType(t.JOB_PASS);
      assertEquals("result", fs.type());

      fs.assignType(t.PASS);
      assertEquals("task", fs.type());

      fs.assignType(t.JOB_FAIL);
      assertEquals("result", fs.type());

      fs.assignType(t.FAIL);
      assertEquals("task", fs.type());
    });

    it("isPartOfSection info", function () {
      el.priv.type = "info";
      assert(_.isEqual([t.INFO], detectPrefixesWhichArePartOfSection(fs)));
    });

    it("isPartOfSection prep", function () {
      el.priv.type = "prep";
      assert(_.isEqual([t.PREP, t.PREP_ERR].sort(), detectPrefixesWhichArePartOfSection(fs)));
    });

    it("isPartOfSection publish", function () {
      el.priv.type = "publish";
      assert(_.isEqual([t.PUBLISH, t.PUBLISH_ERR].sort(), detectPrefixesWhichArePartOfSection(fs)));
    });

    it("isPartOfSection task", function () {
      el.priv.type = "task";
      assert(_.isEqual([t.OUT, t.ERR, t.PASS, t.FAIL, t.CANCELLED].sort(), detectPrefixesWhichArePartOfSection(fs)));
    });

    it("isPartOfSection cancel", function () {
      el.priv.type = "cancel";
      assert(_.isEqual([t.OUT, t.ERR, t.CANCEL_TASK_PASS, t.CANCEL_TASK_FAIL].sort(), detectPrefixesWhichArePartOfSection(fs)));
    });

    it("isPartOfSection result", function () {
      el.priv.type = "result";
      assertEquals(0, detectPrefixesWhichArePartOfSection(fs).length);
    });

    it("isPartOfSection end", function () {
      el.priv.type = "end";
      assertEquals(0, detectPrefixesWhichArePartOfSection(fs).length);
    });

    it("markMultiline prepends the toggle widget exactly once", function () {
      assert(!el.body); // initially, there is no body wrapping element

      el.appendChild(c("dt", "a message"));
      fs.markMultiline();

      // the widget should be the first child
      assertEquals(1, $(el).find(".toggle:first-child").length);

      // the wrapper element should be set
      assert(el.body instanceof Node);

      // subsequent invocations should not add more widgets; markMultiline() must be idempotent
      fs.markMultiline();
      assertEquals(1, $(el).find(".toggle").length);
    });

    it("closeAndStartNew collapses old section if no errors detected and creates a new sibling section", function () {
      var newSection = fs.closeAndStartNew(node);

      assert(!$(el).is(".open"));
      assert($(newSection.element()).is(".open"));

      assertEquals(el, node.childNodes[0]);
      assertEquals(newSection.element(), node.childNodes[1]);
    });

    it("closeAndStartNew leaves old section expanded if errors detected", function () {
      el.priv.errored = true;
      fs.closeAndStartNew(node);

      assert($(el).is(".open"));
    });

    it("detectStatus recognizes failure states", function () {
      fs.detectStatus(t.CANCELLED);
      assert(el.priv.errored);
      assert($(el).is(".log-fs-task-status-cancelled"));

      reset();

      fs.detectStatus(t.FAIL);
      assert(el.priv.errored);
      assert($(el).is(".log-fs-task-status-failed"));

      reset();

      fs.detectStatus(t.JOB_FAIL);
      assert(el.priv.errored);
      assert($(el).is(".log-fs-job-status-failed"));

      reset();

      fs.detectStatus(t.CANCEL_TASK_FAIL);
      assert(el.priv.errored);
      assert($(el).is(".log-fs-task-status-failed"));

      reset();

      fs.assignType(t.TASK_START);
      fs.detectStatus(t.CANCEL_TASK_START);
      assert(el.priv.errored);
      assert($(el).is(".log-fs-task-status-cancelled"));
    });

    it("detectStatus recognizes success states", function () {
      fs.detectStatus(t.PASS);
      assert(!el.priv.errored);
      assert($(el).is(".log-fs-task-status-passed"));

      reset();

      fs.detectStatus(t.JOB_PASS);
      assert(!el.priv.errored);
      assert($(el).is(".log-fs-job-status-passed"));

      reset();

      fs.detectStatus(t.CANCEL_TASK_PASS);
      assert(!el.priv.errored);
      assert($(el).is(".log-fs-task-status-passed"));
    });

    it("isExplicitEndBoundary only identifies prefixes are are terminal", function () {
      assert(_.reduce([t.PASS, t.FAIL, t.CANCELLED, t.JOB_PASS, t.JOB_FAIL, t.CANCEL_TASK_PASS, t.CANCEL_TASK_FAIL], function(result, prefix) {
        result = result && fs.isExplicitEndBoundary(prefix);
        return result;
      }, true));

      assertEquals(false, _.reduce([[t.INFO, t.PREP, t.PREP_ERR, t.TASK_START, t.OUT, t.ERR, t.CANCEL_TASK_START]], function(result, prefix) {
        result = result || fs.isExplicitEndBoundary(prefix);
        return result;
      }, false));
    });

    it("LineWriter insertPlain", function () {
      var h = $($(lw.insertPlain(fs, "00:00:00.000", "Starting build")));
      assert(h.is("dd"));
      assertEquals("Starting build", h.text());
    });

    it("LineWriter insertHeader", function () {
      var h = $($(lw.insertHeader(fs, t.INFO, "00:00:00.000", "Starting build")));
      assert(h.is("dt.log-fs-line-INFO"));
    });

    it("LineWriter insertPlain handles ANSI color", function () {
      var l = $(lw.insertPlain(fs, "00:00:00.000", "Starting \u001B[1;33;40mcolor"));
      assertEquals(1, l.find(".ansi-bright-yellow-fg.ansi-black-bg").length);
      assertEquals("color", l.find(".ansi-bright-yellow-fg.ansi-black-bg").text());
    });

    it("LineWriter insertContent/Header handles ANSI color", function () {
      fs.markMultiline(); // insertContent() requires a section body element
      var l = $(lw.insertContent(fs, t.OUT, "00:00:00.000", "Starting \u001B[1;33;40mcolor"));
      assertEquals(1, l.find(".ansi-bright-yellow-fg.ansi-black-bg").length);
      assertEquals("color", l.find(".ansi-bright-yellow-fg.ansi-black-bg").text());
    });

    it("LineWriter formats exit code", function () {
      var h = $($(lw.insertHeader(fs, t.INFO, "00:00:00.000", "Starting build")));
      lw.markWithAnnotations(fs, {exitCode: 127});
      assertEquals("exited: 127", h.find(".log-fs-exitcode").text());
    });

    it("LineWriter parses exit code from status line if available", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.FAIL, "00:00:00.000", "[go] Task status: failed (exit code: 1)")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      assertEquals("exited: 1", h.find(".log-fs-exitcode").text());
      assertEquals("[go] Task status: failed, exited: 1", l.text());
    });

    it("LineWriter formats durations from milliseconds into a human-friendly stamp", function () {
      var h = $($(lw.insertHeader(fs, t.INFO, "00:00:00.000", "Starting build")));
      lw.markWithAnnotations(fs, {duration: 4998315});
      assertEquals("took: 1h 23m 18.315s", h.find(".log-fs-duration").text());
    });

    it("LineWriter parses durations from status line if available", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.PASS, "00:00:00.000", "[go] Task status: passed (8675309 ms)")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      assertEquals("took: 2h 24m 35.309s", h.find(".log-fs-duration").text());
      assertEquals("[go] Task status: passed, took: 2h 24m 35.309s", l.text());
    });

    it("LineWriter parses both durations and exit code from status line if available", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.PASS, "00:00:00.000", "[go] Task status: passed (8675309 ms) (exit code: 127)")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      assertEquals("took: 2h 24m 35.309s", h.find(".log-fs-duration").text());
      assertEquals("exited: 127", h.find(".log-fs-exitcode").text());
      assertEquals("[go] Task status: passed, took: 2h 24m 35.309s, exited: 127", l.text());
    });

    it("LineWriter just prints if there are no annotations", function () {
      var h = $($(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: doing stuff")));
      fs.markMultiline();
      var l = $($(lw.insertContent(fs, t.PASS, "00:00:00.000", "[go] Task status: passed")));
      fs.closeAndStartNew(c("dl"), lw); // this triggers the duration stamping

      assertEquals(0, h.find(".log-fs-duration").length);
      assertEquals("[go] Task status: passed", l.text());
    });

    it("LineWriter insertContent", function () {
      fs.markMultiline(); // insertContent() requires a section body element
      var l = $(lw.insertContent(fs, t.OUT, "00:00:00.000", "Starting build"));
      assertEquals(l.is("dd.log-fs-line-OUT"));
    });

    it("LineWriter parses task", function () {
      var h = $(lw.insertHeader(fs, t.TASK_START, "00:00:00.000", "[go] Task: about to happen"));
      assertEquals(1, h.find("code").length);
      assertEquals("about to happen", h.find("code").text());
    });

    it("LineWriter parses job status", function () {
      var h = $(lw.insertHeader(fs, t.JOB_PASS, "00:00:00.000", "[go] Current job status: passed"));
      assertEquals(1, h.find("code").length);
      assertEquals("passed", h.find("code").text());
    });

    it("LineWriter parses task status", function () {
      var h = $(lw.insertHeader(fs, t.FAIL, "00:00:00.000", "[go] Task status: failed"));
      assertEquals(1, h.find("code").length);
      assertEquals("failed", h.find("code").text());
    });

    it("LineWriter parses cancel task", function () {
      var h = $(lw.insertHeader(fs, t.CANCEL_TASK_START, "00:00:00.000", "[go] On Cancel Task: not so great"));
      assertEquals(1, h.find("code").length);
      assertEquals("not so great", h.find("code").text());
    });

    it("LineWriter handles empty lines", function () {
      var h = $(lw.insertHeader(fs, t.INFO, "00:00:00.000", ""));
      assertEquals("\n", h.text());
    });

  });

})(jQuery, crel, _);
