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

  function LogOutputTransformer(consoleElement, Section, deferTransform) {
    var self = this, writer = new Section.LineWriter();
    var currentSection, currentLine;
    var deferred = [];

    var PREFIXED_LOG_LINE = /^([^|]{2})\|(\d\d:\d\d:\d\d\.\d\d\d) (.*)/, // parses prefix, timestamp, and line content
        LEGACY_LOG_LINE   = /^(\d\d:\d\d:\d\d\.\d\d\d )?(.*)/; // timestamps are not guaranteed on each line in the old format

    consoleElement.on("click", ".toggle", function toggleSectionCollapse(e) {
      e.stopPropagation();
      e.preventDefault();

      var section = $(e.currentTarget).closest(".foldable-section");
      section.toggleClass("open");
      consoleElement.trigger("consoleInteraction");
    });

    var loading = consoleElement.siblings(".console-log-loading");

    function removeLoadingBar() {
      if (loading) {
        loading.remove();
        loading = null;
      }
    }

    consoleElement.on("consoleCompleted", function () {
      removeLoadingBar();
    });

    function injectFragment(fragment, parentElement) {
      if (!!fragment.childNodes.length) {
        window.requestAnimationFrame(function attachSubtree() {
          parentElement.appendChild(fragment);
        });
      }
    }

    function enqueue(fn, args) {
      deferred.push([fn, args]);
    }

    function dequeue() {
      var entry; // entry is [fn, args]
      while (entry = deferred.shift()) {
        entry[0].apply(self, entry[1]);
      }
      deferTransform = !consoleElement.is(":visible");
    }

    function invokeOrDefer(callback, args) {
      args = "undefined" === typeof args ? [] : args;

      if (deferTransform) {
        enqueue(callback, args);
        return;
      }

      dequeue();

      callback.apply(self, args);
    }

    function buildDomFromLogs(logLines) {
      removeLoadingBar();

      var rawLine, match, continuedSection;
      var residual, queue;

      function resetBuffers() {
        residual = document.createDocumentFragment();
        queue = document.createDocumentFragment();

        if (!currentSection) {
          currentSection = new Section.Cursor(residual);
          consoleElement.append(currentSection.element());
        } else {
          currentSection = currentSection.cloneTo(residual);
        }

        continuedSection = currentSection;
      }

      function flushToDOM() {
        var leftover = residual, incomplete = continuedSection.element();
        var added = queue, topLevel = consoleElement[0];

        resetBuffers();

        injectFragment(leftover, incomplete);
        injectFragment(added, topLevel);
      }

      resetBuffers();

      for (var i = 0, prefix, line, timestamp, len = logLines.length; i < len; i++) {
        rawLine = logLines[i];
        match = rawLine.match(PREFIXED_LOG_LINE);

        if (match) {
          prefix = match[1];
          timestamp = match[2];
          line = match[3] || "";

          currentSection.detectStatus(prefix);

          if (!currentSection.type()) {
            currentSection.assignType(prefix);
            currentLine = writer.insertHeader(currentSection, prefix, timestamp, line);

            if (currentSection.isExplicitEndBoundary(prefix)) {
              currentSection = currentSection.closeAndStartNew(queue, writer);
            }
          } else if (currentSection.isPartOfSection(prefix)) {
            currentSection.markMultiline();
            currentLine = writer.insertContent(currentSection, prefix, timestamp, line);

            if (currentSection.isExplicitEndBoundary(prefix)) {
              currentSection = currentSection.closeAndStartNew(queue, writer);
            }
          } else {
            currentSection = currentSection.closeAndStartNew(queue, writer);

            currentSection.assignType(prefix);
            currentLine = writer.insertHeader(currentSection, prefix, timestamp, line);
          }
        } else {

          if (match = rawLine.match(LEGACY_LOG_LINE)) {
            timestamp = $.trim(match[1] || "");
            line = match[2] || "";
          } else {
            timestamp = "", line = rawLine;
          }

          currentLine = writer.insertPlain(currentSection, timestamp, line);
        }
      }

      flushToDOM();
    }


    self.transform = function buildOrDeferLogLines(logLines) {
      invokeOrDefer(buildDomFromLogs, [logLines]);
    };

    self.invoke = invokeOrDefer;
    self.dequeue = dequeue;
  }

  // export
  window.LogOutputTransformer = LogOutputTransformer;
})(jQuery, crel);
