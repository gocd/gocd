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

define(["models/pipeline_configs/properties"], function (Properties) {
  describe("Property Model", function () {
    var properties, property;
    beforeEach(function () {
      properties = new Properties();

      property = properties.createProperty({
        name:   "coverage.class",
        source: "target/emma/coverage.xml",
        xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
      });
    });

    it("should initialize param model with name", function () {
      expect(property.name()).toBe("coverage.class");
    });

    it("should initialize param model with source", function () {
      expect(property.source()).toBe('target/emma/coverage.xml');
    });

    it("should initialize param model with xpath", function () {
      expect(property.xpath()).toBe("substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')");
    });

    describe("validations", function () {
      it("should add error when name is blank but source or xpath is not", function () {
        property.name("");
        var errors = property.validate();
        expect(errors.errors('name')).toEqual(['Name must be present']);
      });

      it("should NOT add error when both name and source and xpath are blank", function () {
        property.name("");
        property.source("");
        property.xpath('');

        var errors = property.validate();
        expect(errors._isEmpty()).toBe(true);
      });

      it("should not allow properties with duplicate names", function () {
        var errorsOnOriginal = property.validate();
        expect(errorsOnOriginal._isEmpty()).toBe(true);

        var duplicateProperty = properties.createProperty({
          name: property.name()
        });

        errorsOnOriginal = property.validate();
        expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

        var errorsOnDuplicate = duplicateProperty.validate();
        expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
      });
    });


    describe("Deserialization from JSON", function () {
      beforeEach(function () {
        property = Properties.Property.fromJSON(sampleJSON());
      });

      it("should initialize from json", function () {
        expect(property.name()).toBe("coverage.class");
        expect(property.source()).toBe('target/emma/coverage.xml');
        expect(property.xpath()).toBe("substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')");
      });

      function sampleJSON() {
        return {
          name:   "coverage.class",
          source: "target/emma/coverage.xml",
          xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
        };
      }
    });
  });
});
