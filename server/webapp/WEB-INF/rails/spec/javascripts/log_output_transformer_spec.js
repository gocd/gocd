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
;(function($) {
  "use strict";

  describe("LogOutputTransformerSpec", function LogOutputTransformerSpec() {
      var transformer, output, fixture;

      function extractText(collection) {
        return _.map(collection, function (el) { return $(el).text(); });
      }

      function extractAttr(collection, name) {
        return _.map(collection, function (el) { return $(el).attr(name); });
      }

      if ("function" !== window.requestAnimationFrame) {
        window.requestAnimationFrame = function (callback) { callback(); };
      }

      beforeEach(function () {
          setFixtures("<div id='fixture'><div class='console-log-loading'></div><div id=\"console\"></div></div>");
          fixture = $('#fixture');
          transformer = new LogOutputTransformer(output = $("#console"), FoldableSection, false);
      });

      it("basic unprefixed append to console", function () {
        var lines = [
          "Starting build",
          "Build finished in no time!"
        ];

        transformer.transform(lines);
        assertEquals(2, output.find("dd").length);

        var actual = extractText(output.find("dd"));
        assertEquals(lines.join("\n"), actual.join("\n")); // can't assertEquals() on arrays, so compare as strings
      });

      it("basic prefixed append to console", function () {
        var lines = [
          "##|01:01:00.123 Starting build",
          "##|01:02:00.123 Build finished in no time!"
        ];

        transformer.transform(lines);
        var section = output.find(".log-fs-type-info")
        assertTrue(!!section.length);

        var timestamps = extractAttr(section.find(".log-fs-line"), "data-timestamp").join(",");
        assertEquals(["01:01:00.123", "01:02:00.123"].join(","), timestamps);

        output.find(".ts").remove(); // exclude timestamps so it's easier to assert content
        var actual = extractText(output.find(".log-fs-line-INFO"));
        assertEquals(["Starting build", "Build finished in no time!"].join("\n"), actual.join("\n")); // can't assertEquals() on arrays, so compare as strings
      });

      it("should remove loading bar when console has lines to show", function() {
        var lines = [
          "##|01:01:00.123 Starting build",
          "##|01:02:00.123 Build finished in no time!"
        ];

        assertTrue(fixture.find(".console-log-loading").is(':visible'));
        transformer.transform(lines);

        assertFalse(fixture.find(".console-log-loading").is(':visible'));
      });

      it("should remove loading bar when build has finished and there is no output", function() {
        assertTrue(fixture.find(".console-log-loading").is(':visible'));
        output.trigger('consoleCompleted');

        assertFalse(fixture.find(".console-log-loading").is(':visible'));
      });
  });

})(jQuery);
