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
describe("TrackingTool Model", () => {

  const s = require('string-plus');

  const TrackingTool = require("models/pipeline_configs/tracking_tool");
  let trackingTool;
  describe("Generic", () => {
    beforeEach(() => {
      trackingTool = new TrackingTool.Generic({
        urlPattern: 'http://example.com/bugzilla?id=${ID}',
        regex:      "bug-(\\d+)"
      });
    });

    it("should initialize trackingTool model with type", () => {
      expect(trackingTool.type()).toBe("generic");
    });

    it("should initialize trackingTool model with urlPattern", () => {
      expect(trackingTool.urlPattern()).toBe("http://example.com/bugzilla?id=${ID}");
    });

    it("should initialize trackingTool model with regex", () => {
      expect(trackingTool.regex()).toBe("bug-(\\d+)");
    });

    describe('validations', () => {
      beforeEach(() => {
        trackingTool = new TrackingTool.Generic({});
        trackingTool.validate();
      });

      it('should validate presence and format of urlPattern', () => {
        expect(trackingTool.errors().errors('urlPattern')).toEqual(['URL pattern must be present', "Url pattern must contain the string '${ID}'"]);
      });

      it('should validate urlPattern to be a valid url', () => {
        trackingTool.urlPattern("ftp://foo.bar '${ID}'");
        trackingTool.validate();

        expect(trackingTool.errors().errors('urlPattern')).toEqual(["Url pattern must be a valid http(s) url"]);
      });

      it('should validate presence and format of regex', () => {
        expect(trackingTool.errors().errors('regex')).toEqual(['Regex must be present']);
      });
    });

    describe("Deserialization to/from JSON", () => {
      beforeEach(() => {
        trackingTool = TrackingTool.fromJSON(sampleJSON());
      });

      it("should initialize from json", () => {
        expect(trackingTool.type()).toBe("generic");
        expect(trackingTool.urlPattern()).toBe('http://example.com/bugzilla?id=${ID}');
        expect(trackingTool.regex()).toBe("bug-(\\d+)");
      });

      it('should serialize to json', () => {
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

  describe("Mingle", () => {
    beforeEach(() => {
      trackingTool = new TrackingTool.Mingle({
        baseUrl:               'http://mingle.example.com',
        projectIdentifier:     "gocd",
        mqlGroupingConditions: "status > 'In Dev'"
      });
    });

    it("should initialize trackingTool model with type", () => {
      expect(trackingTool.type()).toBe("mingle");
    });

    it("should initialize trackingTool model with baseUrl", () => {
      expect(trackingTool.baseUrl()).toBe("http://mingle.example.com");
    });

    it("should initialize trackingTool model with projectIdentifier", () => {
      expect(trackingTool.projectIdentifier()).toBe("gocd");
    });

    it("should initialize trackingTool model with mqlGroupingConditions", () => {
      expect(trackingTool.mqlGroupingConditions()).toBe("status > 'In Dev'");
    });

    describe('validations', () => {
      beforeEach(() => {
        trackingTool = new TrackingTool.Mingle({});
        trackingTool.validate();
      });

      it('should validate presence and format of baseUrl', () => {
        expect(trackingTool.errors().errors('baseUrl')).toEqual(['Base URL must be present']);
      });

      it('should validate baseUrl to be a valid url', () => {
        trackingTool.baseUrl("ftp://foo.bar");
        trackingTool.validate();

        expect(trackingTool.errors().errors('baseUrl')).toEqual(["Base url must be a valid http(s) url"]);
      });

      it('should validate presence and format of projectIdentifier', () => {
        expect(trackingTool.errors().errors('projectIdentifier')).toEqual(['Project identifier must be present']);
      });

      it('should validate presence and format of mqlGroupingConditions', () => {
        expect(trackingTool.errors().errors('mqlGroupingConditions')).toEqual(['Mql grouping conditions must be present']);
      });
    });

    describe("Deserialization from JSON", () => {
      beforeEach(() => {
        trackingTool = TrackingTool.fromJSON(sampleJSON());
      });

      it("should initialize from json", () => {
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
