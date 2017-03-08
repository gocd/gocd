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
;(function($, c) {
  "use strict";

  describe("FoldableSectionSpec", function FoldableSectionSpec() {
    var fs, lw, node, el, t = FoldableSection.Types;

    function reset() {
      node = document.createDocumentFragment();
      el = $("<dl class=\"foldable-section open\">");
      fs = new FoldableSection.Cursor(node, el);
      lw = new FoldableSection.LineWriter();
      node.appendChild(el[0]);
    }

    beforeEach(reset);

    it("assignType", function () {
      fs.assignType(t.INFO);
      assertEquals("info", el.attr("data-type"));

      fs.assignType(t.TASK_START);
      assertEquals("task", el.attr("data-type"));

      fs.assignType(t.PREP);
      assertEquals("prep", el.attr("data-type"));

      fs.assignType(t.CANCEL_TASK_START);
      assertEquals("cancel", el.attr("data-type"));

      fs.assignType(t.PREP_ERR);
      assertEquals("prep", el.attr("data-type"));

      fs.assignType(t.CANCEL_TASK_FAIL);
      assertEquals("cancel", el.attr("data-type"));

      fs.assignType(t.OUT);
      assertEquals("task", el.attr("data-type"));

      fs.assignType(t.ERR);
      assertEquals("task", el.attr("data-type"));

      fs.assignType(t.CANCEL_TASK_PASS);
      assertEquals("cancel", el.attr("data-type"));

      fs.assignType(t.JOB_PASS);
      assertEquals("result", el.attr("data-type"));

      fs.assignType(t.PASS);
      assertEquals("task", el.attr("data-type"));

      fs.assignType(t.JOB_FAIL);
      assertEquals("result", el.attr("data-type"));

      fs.assignType(t.FAIL);
      assertEquals("task", el.attr("data-type"));
    });

    it("isPartOfSection info", function () {
      el.attr("data-type", "info");
      assert(fs.isPartOfSection(t.INFO));
      assert(!fs.isPartOfSection(t.PREP));
      assert(!fs.isPartOfSection(t.PREP_ERR));
      assert(!fs.isPartOfSection(t.TASK_START));
      assert(!fs.isPartOfSection(t.OUT));
      assert(!fs.isPartOfSection(t.ERR));
      assert(!fs.isPartOfSection(t.PASS));
      assert(!fs.isPartOfSection(t.FAIL));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_START));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_PASS));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_FAIL));
      assert(!fs.isPartOfSection(t.JOB_PASS));
      assert(!fs.isPartOfSection(t.JOB_FAIL));
    });

    it("isPartOfSection prep", function () {
      el.attr("data-type", "prep");
      assert(!fs.isPartOfSection(t.INFO));
      assert(fs.isPartOfSection(t.PREP));
      assert(fs.isPartOfSection(t.PREP_ERR));
      assert(!fs.isPartOfSection(t.TASK_START));
      assert(!fs.isPartOfSection(t.OUT));
      assert(!fs.isPartOfSection(t.ERR));
      assert(!fs.isPartOfSection(t.PASS));
      assert(!fs.isPartOfSection(t.FAIL));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_START));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_PASS));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_FAIL));
      assert(!fs.isPartOfSection(t.JOB_PASS));
      assert(!fs.isPartOfSection(t.JOB_FAIL));
    });

    it("isPartOfSection task", function () {
      el.attr("data-type", "task");
      assert(!fs.isPartOfSection(t.INFO));
      assert(!fs.isPartOfSection(t.PREP));
      assert(!fs.isPartOfSection(t.PREP_ERR));
      assert(!fs.isPartOfSection(t.TASK_START));
      assert(fs.isPartOfSection(t.OUT));
      assert(fs.isPartOfSection(t.ERR));
      assert(fs.isPartOfSection(t.PASS));
      assert(fs.isPartOfSection(t.FAIL));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_START));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_PASS));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_FAIL));
      assert(!fs.isPartOfSection(t.JOB_PASS));
      assert(!fs.isPartOfSection(t.JOB_FAIL));
    });

    it("isPartOfSection cancel", function () {
      el.attr("data-type", "cancel");
      assert(!fs.isPartOfSection(t.INFO));
      assert(!fs.isPartOfSection(t.PREP));
      assert(!fs.isPartOfSection(t.PREP_ERR));
      assert(!fs.isPartOfSection(t.TASK_START));
      assert(fs.isPartOfSection(t.OUT));
      assert(fs.isPartOfSection(t.ERR));
      assert(!fs.isPartOfSection(t.PASS));
      assert(!fs.isPartOfSection(t.FAIL));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_START));
      assert(fs.isPartOfSection(t.CANCEL_TASK_PASS));
      assert(fs.isPartOfSection(t.CANCEL_TASK_FAIL));
      assert(!fs.isPartOfSection(t.JOB_PASS));
      assert(!fs.isPartOfSection(t.JOB_FAIL));
    });

    it("isPartOfSection result", function () {
      el.attr("data-type", "result");
      assert(!fs.isPartOfSection(t.INFO));
      assert(!fs.isPartOfSection(t.PREP));
      assert(!fs.isPartOfSection(t.PREP_ERR));
      assert(!fs.isPartOfSection(t.TASK_START));
      assert(!fs.isPartOfSection(t.OUT));
      assert(!fs.isPartOfSection(t.ERR));
      assert(!fs.isPartOfSection(t.PASS));
      assert(!fs.isPartOfSection(t.FAIL));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_START));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_PASS));
      assert(!fs.isPartOfSection(t.CANCEL_TASK_FAIL));
      assert(!fs.isPartOfSection(t.JOB_PASS));
      assert(!fs.isPartOfSection(t.JOB_FAIL));
    });

    it("markMultiline prepends the toggle widget exactly once", function () {
      el.append(c("dt", "a message"));
      fs.markMultiline();
      // "the widget should be the first child"
      assertEquals(1, el.find(".toggle:first-child").length);
      fs.markMultiline();
      // subsequent invocations should not add more widgets; markMultiline() must be idempotent
      assertEquals(1, el.find(".toggle").length);
    });

    it("closeAndStartNew collapses old section if no errors detected and creates a new sibling section", function () {
      var newSection = fs.closeAndStartNew(node);

      assert(!el.is(".open"));
      assert(newSection.element().is(".open"));

      assertEquals(el[0], node.childNodes[0]);
      assertEquals(newSection.element()[0], node.childNodes[1]);
    });

    it("closeAndStartNew leaves old section expanded if errors detected", function () {
      el.data("errored", true);
      fs.closeAndStartNew(node);

      assert(el.is(".open"));
    });

    it("detectStatus recognizes failure states", function () {
      fs.detectStatus(t.FAIL);
      assert(el.data("errored"));
      assertEquals("failed", el.attr("data-task-status"));

      reset();

      fs.detectStatus(t.JOB_FAIL);
      assert(el.data("errored"));
      assertEquals("failed", el.attr("data-job-status"));

      reset();

      fs.detectStatus(t.CANCEL_TASK_FAIL);
      assert(el.data("errored"));
      assertEquals("failed", el.attr("data-task-status"));

      reset();

      fs.assignType(t.TASK_START);
      fs.detectStatus(t.CANCEL_TASK_START);
      assert(el.data("errored"));
      assertEquals("cancelled", el.attr("data-task-status"));
    });

    it("detectStatus recognizes success states", function () {
      fs.detectStatus(t.PASS);
      assert(!el.data("errored"));
      assertEquals("passed", el.attr("data-task-status"));

      reset();

      fs.detectStatus(t.JOB_PASS);
      assert(!el.data("errored"));
      assertEquals("passed", el.attr("data-job-status"));

      reset();

      fs.detectStatus(t.CANCEL_TASK_PASS);
      assert(!el.data("errored"));
      assertEquals("passed", el.attr("data-task-status"));
    });

    it("isExplicitEndBoundary only identifies prefixes are are terminal", function () {
      assert(_.reduce([t.PASS, t.FAIL, t.JOB_PASS, t.JOB_FAIL, t.CANCEL_TASK_PASS, t.CANCEL_TASK_FAIL], function(result, prefix) {
        result = result && fs.isExplicitEndBoundary(prefix);
        return result;
      }, true));

      assertEquals(false, _.reduce([[t.INFO, t.PREP, t.PREP_ERR, t.TASK_START, t.OUT, t.ERR, t.CANCEL_TASK_START]], function(result, prefix) {
        result = result || fs.isExplicitEndBoundary(prefix);
        return result;
      }, false));
    });

    it("LineWriter insertBasic", function () {
      var h = lw.insertBasic(fs, "Starting build");
      assert(h.is("dd"));
      assertEquals("Starting build", h.text());
    });

    it("LineWriter insertHeader", function () {
      var h = lw.insertHeader(fs, t.INFO, "Starting build");
      assertEquals(t.INFO, h.data("prefix"));
      assert(h.is("dt[data-prefix]"));
    });

    it("LineWriter insertBasic handles ANSI color", function () {
      var l = lw.insertBasic(fs, "Starting \u001B[1;33;40mcolor");
      assertEquals(1, l.find(".ansi-bright-yellow-fg.ansi-black-bg").length);
      assertEquals("color", l.find(".ansi-bright-yellow-fg.ansi-black-bg").text());
    });

    it("LineWriter insertLine/Header handles ANSI color", function () {
      var l = lw.insertLine(fs, t.OUT, "Starting \u001B[1;33;40mcolor");
      assertEquals(1, l.find(".ansi-bright-yellow-fg.ansi-black-bg").length);
      assertEquals("color", l.find(".ansi-bright-yellow-fg.ansi-black-bg").text());
    });

    it("LineWriter insertLine", function () {
      var l = lw.insertLine(fs, t.OUT, "Starting build");
      assertEquals(t.OUT, l.data("prefix"));
      assert(l.is("dd[data-prefix]"));
    });

    it("LineWriter parses task", function () {
      var h = lw.insertHeader(fs, t.TASK_START, "[go] Task: about to happen");
      assertEquals(1, h.find("code").length);
      assertEquals("about to happen", h.find("code").text());
    });

    it("LineWriter parses job status", function () {
      var h = lw.insertHeader(fs, t.JOB_PASS, "[go] Current job status: passed");
      assertEquals(1, h.find("code").length);
      assertEquals("passed", h.find("code").text());
    });

    it("LineWriter parses task status", function () {
      var h = lw.insertHeader(fs, t.FAIL, "[go] Task status: failed");
      assertEquals(1, h.find("code").length);
      assertEquals("failed", h.find("code").text());
    });

    it("LineWriter parses cancel task", function () {
      var h = lw.insertHeader(fs, t.CANCEL_TASK_START, "[go] On Cancel Task: not so great");
      assertEquals(1, h.find("code").length);
      assertEquals("not so great", h.find("code").text());
    });

    it("LineWriter handles empty lines", function () {
      var h = lw.insertHeader(fs, t.INFO, "");
      assertEquals(1, h.find("br").length);
    });

  });

})(jQuery, crel);
