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

define(['lodash', "models/pipeline_configs/tracking_tool", 'string-plus'], function (_, TrackingTool, s) {
  describe("TrackingTool Model", function () {
    var trackingTool;
    describe("Generic", function () {
      beforeAll(function () {
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

      describe('validations', function () {
        beforeAll(function () {
          trackingTool = new TrackingTool.Generic({});
          trackingTool.validate();
        });

        it('should validate presence and format of urlPattern', function () {
          expect(trackingTool.errors().errors('urlPattern')).toEqual(['URL pattern must be present', "Url pattern must contain the string '${ID}'"]);
        });

        it('should validate urlPattern to be a valid url', function () {
          trackingTool.urlPattern("ftp://foo.bar '${ID}'");
          trackingTool.validate();

          expect(trackingTool.errors().errors('urlPattern')).toEqual(["Url pattern must be a valid http(s) url"]);
        });

        it('should validate presence and format of regex', function () {
          expect(trackingTool.errors().errors('regex')).toEqual(['Regex must be present']);
        });
      });

      describe("Deserialization to/from JSON", function () {
        beforeEach(function () {
          trackingTool = TrackingTool.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
          expect(trackingTool.type()).toBe("generic");
          expect(trackingTool.urlPattern()).toBe('http://example.com/bugzilla?id=${ID}');
          expect(trackingTool.regex()).toBe("bug-(\\d+)");
        });

        it('should serialize to json', function () {
          expect(JSON.parse(JSON.stringify(trackingTool, s.snakeCaser))).toEqual(sampleJSON());
        });

        function sampleJSON() {
          return {
            type:       "generic",
            attributes: {
              url_pattern: 'http://example.com/bugzilla?id=${ID}', // eslint-disable-line camelcase
              regex:       "bug-(\\d+)"
            }
          };
        }
      });
    });

    describe("Mingle", function () {
      beforeAll(function () {
        trackingTool = new TrackingTool.Mingle({
          baseUrl:               'http://mingle.example.com',
          projectIdentifier:     "gocd",
          mqlGroupingConditions: "status > 'In Dev'"
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

      it("should initialize trackingTool model with mqlGroupingConditions", function () {
        expect(trackingTool.mqlGroupingConditions()).toBe("status > 'In Dev'");
      });

      describe('validations', function () {
        beforeAll(function () {
          trackingTool = new TrackingTool.Mingle({});
          trackingTool.validate();
        });

        it('should validate presence and format of baseUrl', function () {
          expect(trackingTool.errors().errors('baseUrl')).toEqual(['Base URL must be present']);
        });

        it('should validate baseUrl to be a valid url', function () {
          trackingTool.baseUrl("ftp://foo.bar");
          trackingTool.validate();

          expect(trackingTool.errors().errors('baseUrl')).toEqual(["Base url must be a valid http(s) url"]);
        });

        it('should validate presence and format of projectIdentifier', function () {
          expect(trackingTool.errors().errors('projectIdentifier')).toEqual(['Project identifier must be present']);
        });

        it('should validate presence and format of mqlGroupingConditions', function () {
          expect(trackingTool.errors().errors('mqlGroupingConditions')).toEqual(['Mql grouping conditions must be present']);
        });
      });

      describe("Deserialization from JSON", function () {
        beforeEach(function () {
          trackingTool = TrackingTool.fromJSON(sampleJSON());
        });

        it("should initialize from json", function () {
          expect(trackingTool.type()).toBe("mingle");
          expect(trackingTool.baseUrl()).toBe('http://mingle.example.com');
          expect(trackingTool.projectIdentifier()).toBe('gocd');
          expect(trackingTool.mqlGroupingConditions()).toBe("status > 'In Dev'");
        });

        function sampleJSON() {
          /* eslint-disable camelcase */
          return {
            type:       "mingle",
            attributes: {
              base_url:                'http://mingle.example.com',
              project_identifier:      "gocd",
              mql_grouping_conditions: "status > 'In Dev'"
            }
          };
          /* eslint-enable camelcase */
        }
      });
    });

  });
});
