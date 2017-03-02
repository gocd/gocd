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
      var transformer, output;

      function extractText(collection) {
        return _.map(collection, function (el) { return $(el).text(); });
      }

      if ("function" !== window.requestAnimationFrame) {
        window.requestAnimationFrame = function (callback) { callback(); };
      }

      beforeEach(function () {
          setFixtures("<div id=\"console\"></div>");
          transformer = new LogOutputTransformer(output = $("#console"), FoldableSection);
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
        var section = output.find("[data-type='info']")
        assertTrue(!!section.length);

        var timestamps = extractText(section.find(".ts")).join(",");
        assertEquals(["01:01:00.123", "01:02:00.123"].join(","), timestamps);

        output.find(".ts").remove(); // exclude timestamps so it's easier to assert content
        var actual = extractText(output.find("[data-prefix='##']"));
        assertEquals(["Starting build", "Build finished in no time!"].join("\n"), actual.join("\n")); // can't assertEquals() on arrays, so compare as strings
      });

  });

})(jQuery);
