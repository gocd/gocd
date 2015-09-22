/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['lodash', "pipeline_configs/models/tracking_tool"], function (_, TrackingTool) {
  describe("TrackingTool Model", function () {
    var trackingTool;
    describe("Generic", function () {
      beforeEach(function () {
        trackingTool = new TrackingTool.Generic({
          urlPattern: 'http://example.com/bugzilla?id=${ID}',
          regex:      "bug-(\\d+)"
        });
      });

      it("should initialize trackingTool model with type", function () {
        expect(trackingTool.type()).toBe("generic");
      });

      it("should initialize trackingTool model with urlPattern", function () {
        expect(trackingTool.urlPattern()).toBe("http://example.com/bugzilla?id=${ID}");
      });

      it("should initialize trackingTool model with regex", function () {
        expect(trackingTool.regex()).toBe("bug-(\\d+)");
      });

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          trackingTool = TrackingTool.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
          expect(trackingTool.type()).toBe("generic");
          expect(trackingTool.urlPattern()).toBe('http://example.com/bugzilla?id=${ID}');
          expect(trackingTool.regex()).toBe("bug-(\\d+)");
        });

        function sampleJSON() {
          return {
            type:       "generic",
            attributes: {
              url_pattern: 'http://example.com/bugzilla?id=${ID}',
              regex:       "bug-(\\d+)"
            }
          };
        }
      });
    });

    describe("Mingle", function () {
      beforeEach(function () {
        trackingTool = new TrackingTool.Mingle({
          baseUrl:            'http://mingle.example.com',
          projectIdentifier:  "gocd",
          groupingConditions: "status > 'In Dev'"
        });
      });

      it("should initialize trackingTool model with type", function () {
        expect(trackingTool.type()).toBe("mingle");
      });

      it("should initialize trackingTool model with baseUrl", function () {
        expect(trackingTool.baseUrl()).toBe("http://mingle.example.com");
      });

      it("should initialize trackingTool model with projectIdentifier", function () {
        expect(trackingTool.projectIdentifier()).toBe("gocd");
      });

      it("should initialize trackingTool model with groupingConditions", function () {
        expect(trackingTool.groupingConditions()).toBe("status > 'In Dev'");
      });

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          trackingTool = TrackingTool.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
          expect(trackingTool.type()).toBe("mingle");
          expect(trackingTool.baseUrl()).toBe('http://mingle.example.com');
          expect(trackingTool.projectIdentifier()).toBe('gocd');
          expect(trackingTool.groupingConditions()).toBe("status > 'In Dev'");
        });

        function sampleJSON() {
          return {
            type:       "mingle",
            attributes: {
              base_url:            'http://mingle.example.com',
              project_identifier:  "gocd",
              grouping_conditions: "status > 'In Dev'"
            }
          };
        }
      });
    });

  });
});
