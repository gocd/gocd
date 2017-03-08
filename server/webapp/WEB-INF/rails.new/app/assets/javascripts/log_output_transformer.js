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

  function LogOutputTransformer(consoleElement, Section) {
    var self = this, writer = new Section.LineWriter();
    var currentSection, currentLine, lineNumber = 0;

    var PREFIXED_LOG_LINE = /^([^|]{2})\|(\d\d:\d\d:\d\d\.\d\d\d) (.*)/, // parses prefix, timestamp, and line content
        LEGACY_LOG_LINE   = /^(\d\d:\d\d:\d\d\.\d\d\d )?(.*)/; // timestamps are not guaranteed on each line in the old format

    consoleElement.on("click", ".toggle", function toggleSectionCollapse(e) {
      e.stopPropagation();
      e.preventDefault();

      var section = $(e.currentTarget).closest(".foldable-section");
      section.toggleClass("open");
    });

    function injectFragment(fragment, parentElement) {
      if (!!fragment.childNodes.length) {
        window.requestAnimationFrame(function attachSubtree() {
          parentElement.appendChild(fragment);
        });
      }
    }

    self.transform = function buildDomFromLogs(logLines) {
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
        var leftover = residual, incomplete = continuedSection.element()[0];
        var added = queue, topLevel = consoleElement[0];

        resetBuffers();

        injectFragment(leftover, incomplete);
        injectFragment(added, topLevel);
      }

      resetBuffers();

      for (var i = 0, prefix, line, timestamp, len = logLines.length; i < len; i++) {
        lineNumber++;
        rawLine = logLines[i];
        match = rawLine.match(PREFIXED_LOG_LINE);

        if (match) {
          prefix = match[1];
          timestamp = match[2];
          line = match[3] || "";

          currentSection.detectStatus(prefix);

          if (!currentSection.hasType()) {
            currentSection.assignType(prefix);
            currentLine = writer.insertHeader(currentSection, prefix, line);

            if (currentSection.isExplicitEndBoundary(prefix)) {
              currentSection = currentSection.closeAndStartNew(queue);
            }
          } else if (currentSection.isPartOfSection(prefix)) {
            currentSection.markMultiline();
            currentLine = writer.insertLine(currentSection, prefix, line);

            if (currentSection.isExplicitEndBoundary(prefix)) {
              currentSection = currentSection.closeAndStartNew(queue);
            }
          } else {
            currentSection = currentSection.closeAndStartNew(queue);

            currentSection.assignType(prefix);
            currentLine = writer.insertHeader(currentSection, prefix, line);
          }
        } else {

          if (match = rawLine.match(LEGACY_LOG_LINE)) {
            timestamp = $.trim(match[1] || "");
            line = match[2] || "";
          } else {
            timestamp = "", line = rawLine;
          }

          currentLine = writer.insertBasic(currentSection, line);
        }

        currentLine.attr("data-line", lineNumber).prepend(c("span", {class: "ts"}, timestamp));
      }

      flushToDOM();
    };
  }

  // export
  window.LogOutputTransformer = LogOutputTransformer;
})(jQuery, crel);
