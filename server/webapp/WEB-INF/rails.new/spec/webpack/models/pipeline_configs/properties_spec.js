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
describe("Property Model", () => {

  const Properties = require("models/pipeline_configs/properties");
  let properties, property;

  beforeEach(() => {
    properties = new Properties();

    property = properties.createProperty({
      name:   "coverage.class",
      source: "target/emma/coverage.xml",
      xpath:  "substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')"
    });
  });

  it("should initialize param model with name", () => {
    expect(property.name()).toBe("coverage.class");
  });

  it("should initialize param model with source", () => {
    expect(property.source()).toBe('target/emma/coverage.xml');
  });

  it("should initialize param model with xpath", () => {
    expect(property.xpath()).toBe("substring-before(//report/data/all/coverage[starts-with(@type,'class')]/@value, '%')");
  });

  describe("validations", () => {
    it("should add error when name is blank but source or xpath is not", () => {
      property.name("");
      const errors = property.validate();
      expect(errors.errors('name')).toEqual(['Name must be present']);
    });

    it("should NOT add error when both name and source and xpath are blank", () => {
      property.name("");
      property.source("");
      property.xpath('');

      const errors = property.validate();
      expect(errors._isEmpty()).toBe(true);
    });

    it("should not allow properties with duplicate names", () => {
      let errorsOnOriginal = property.validate();
      expect(errorsOnOriginal._isEmpty()).toBe(true);

      const duplicateProperty = properties.createProperty({
        name: property.name()
      });

      errorsOnOriginal = property.validate();
      expect(errorsOnOriginal.errors('name')).toEqual(['Name is a duplicate']);

      const errorsOnDuplicate = duplicateProperty.validate();
      expect(errorsOnDuplicate.errors('name')).toEqual(['Name is a duplicate']);
    });
  });


  describe("Deserialization from JSON", () => {
    beforeEach(() => {
      property = Properties.Property.fromJSON(sampleJSON());
    });

    it("should initialize from json", () => {
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
