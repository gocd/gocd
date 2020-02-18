/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import _ from "lodash";
import Stream from "mithril/stream";
import {ErrorMessages} from "models/mixins/error_messages";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin, Validator, ValidatorOptions} from "models/mixins/new_validatable_mixin";
import s from "underscore.string";

describe("Validatable", () => {
  describe("validateNonEmptyCollection", () => {
    describe("validates that Iterables are not empty:", () => {
      it("String", () => {
        const t = new Thang();

        expect(t.validate("string").hasErrors("string")).toBe(true);
        expect(t.validate("string").errorsForDisplay("string")).toBe("this string is empty.");
        t.string = "foo";
        expect(t.validate("string").hasErrors("string")).toBe(false);
        expect(t.validate("string").errorsForDisplay("string")).toBe("");
      });

      it("Array", () => {
        const t = new Thang();

        expect(t.validate("array").hasErrors("array")).toBe(true);
        expect(t.validate("array").errorsForDisplay("array")).toBe("this array is empty.");
        t.array = [1];
        expect(t.validate("array").hasErrors("array")).toBe(false);
        expect(t.validate("array").errorsForDisplay("array")).toBe("");
      });

      it("Map", () => {
        const t = new Thang();

        expect(t.validate("map").hasErrors("map")).toBe(true);
        expect(t.validate("map").errorsForDisplay("map")).toBe("this map is empty.");
        t.map = new Map<string, number>().set("a", 1);
        expect(t.validate("map").hasErrors("map")).toBe(false);
        expect(t.validate("map").errorsForDisplay("map")).toBe("");
      });

      it("Set", () => {
        const t = new Thang();

        expect(t.validate("set").hasErrors("set")).toBe(true);
        expect(t.validate("set").errorsForDisplay("set")).toBe("this set is empty.");
        t.set = new Set<number>([1]);
        expect(t.validate("set").hasErrors("set")).toBe(false);
        expect(t.validate("set").errorsForDisplay("set")).toBe("");
      });
    });
  });

  class Thang extends ValidatableMixin {
    string: string = "";
    map: Map<string, number> = new Map<string, number>();
    array: number[] = [];
    set: Set<number> = new Set<number>();

    constructor() {
      super();
      this.validateNonEmptyCollection("string", {message: "this string is empty"});
      this.validateNonEmptyCollection("array", {message: "this array is empty"});
      this.validateNonEmptyCollection("map", {message: "this map is empty"});
      this.validateNonEmptyCollection("set", {message: "this set is empty"});
    }
  }

  describe("validatePresenceOf", () => {

    it("should validate the presence of a given field", () => {
      //tslint:disable-next-line
      interface Material extends ValidatableMixin {
      }

      class Material implements ValidatableMixin {
        readonly name: Stream<string>;

        constructor(name: string) {
          this.name = Stream(name);
          ValidatableMixin.call(this);
          this.validatePresenceOf("name");
        }
      }

      applyMixins(Material, ValidatableMixin);

      const material = new Material("");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("name")).toEqual(["Name must be present"]);
    });

    it("should be conditional", () => {
      //tslint:disable-next-line
      interface EnvVariable extends ValidatableMixin {
      }

      class EnvVariable implements ValidatableMixin {
        name: Stream<string>;
        value: Stream<string>;

        constructor() {
          ValidatableMixin.call(this);
          this.name  = Stream("");
          this.value = Stream("");
          this.validatePresenceOf("value", {condition: () => !s.isBlank(this.name())});
        }
      }

      applyMixins(EnvVariable, ValidatableMixin);

      const variable = new EnvVariable();
      variable.validate();
      expect(variable.errors().hasErrors()).toBe(false);
    });
  });

  describe("validateUniquenessOf", () => {

    //tslint:disable-next-line
    interface Variable extends ValidatableMixin {
    }

    class Variable implements ValidatableMixin {
      key: Stream<string>;
      parent: Stream<Variables>;

      constructor(key: string) {
        ValidatableMixin.call(this);
        this.key    = Stream(key);
        this.parent = Stream();
        this.validateUniquenessOf("key", () => this.parent().variables()
                                                   .filter((variable: Variable) => this !== variable));
      }
    }

    class Variables {
      variables: Stream<Variable[]>;

      constructor(variables: Variable[]) {
        this.variables = Stream(variables);
      }
    }

    applyMixins(Variable, ValidatableMixin);

    it("should validate uniqueness of a given field", () => {
      const var1      = new Variable("name");
      const var2      = new Variable("name");
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var2.parent(variables);

      var1.validate();

      expect(var1.errors().hasErrors()).toBe(true);
      expect(var1.errors().errors("key")).toEqual(["Key is a duplicate"]);
    });

    it("should skip validation if attribute is empty", () => {
      const var1      = new Variable("");
      const var2      = new Variable("");
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var2.parent(variables);

      var1.validate();

      expect(var1.errors().hasErrors()).toBe(false);
    });

    it("should not give validation errors if fields are unique", () => {
      const var1      = new Variable("name1");
      const var2      = new Variable("name2");
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var2.parent(variables);

      var1.validate();

      expect(var1.errors().hasErrors()).toBe(false);
    });
  });

  describe("validateUrlPattern", () => {
    //tslint:disable-next-line
    interface Material extends ValidatableMixin {
    }

    class Material implements ValidatableMixin {
      url: Stream<string>;

      constructor(url: string) {
        ValidatableMixin.call(this);
        this.url = Stream(url);
        this.validateUrlPattern("url");
      }
    }

    applyMixins(Material, ValidatableMixin);

    it("should validate url pattern", () => {
      const material = new Material("ftp://some.com");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("url")).toEqual(["Url must be a valid http(s) url"]);
    });

    it("should skip validation for empty attribute", () => {
      const material = new Material("");

      material.validate();

      expect(material.errors().hasErrors()).toBe(false);
    });
  });

  describe("validateFormatOf", () => {

    //tslint:disable-next-line
    interface Material extends ValidatableMixin {
    }

    class Material implements ValidatableMixin {
      url: Stream<string>;

      constructor(url: string) {
        ValidatableMixin.call(this);
        this.url = Stream(url);
        this.validateFormatOf("url", new RegExp("^http(s)?:\\/\\/.+"), {message: "Url format is invalid"});
      }
    }

    applyMixins(Material, ValidatableMixin);

    it("should validate the format of field", () => {
      const material = new Material("ftp://some.com");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("url")).toEqual(["Url format is invalid"]);
    });

    it("should skip validation for empty attribute", () => {
      const material = new Material("");

      material.validate();

      expect(material.errors().hasErrors()).toBe(false);
    });
  });

  describe("validateIdFormat", () => {

    it("should validate Id format", () => {
      //tslint:disable-next-line
      interface Material extends ValidatableMixin {
      }

      class Material implements ValidatableMixin {
        id: Stream<string>;

        constructor(id: string) {
          ValidatableMixin.call(this);
          this.id = Stream(id);
          this.validateIdFormat("id");
        }
      }

      applyMixins(Material, ValidatableMixin);

      const badStrings = ["shaky salamander", ".hello", _.repeat("a", 256)];
      badStrings.forEach((badString) => {
        const material = new Material(badString);
        material.validate();
        expect(material.errors().hasErrors()).toBe(true);
        expect(material.errors().errors("id"))
          .toEqual(["Invalid id. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."]);
      });

    });

    it("should validate Id format using provided message", () => {
      //tslint:disable-next-line
      interface Material extends ValidatableMixin {
      }

      class Material implements ValidatableMixin {
        id: Stream<string>;

        constructor(id: string) {
          ValidatableMixin.call(this);
          this.id = Stream(id);
          this.validateIdFormat("id", {message: "Id is invalid"});
        }
      }

      applyMixins(Material, ValidatableMixin);
      const material = new Material("shaky salamander");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("id")).toEqual(["Id is invalid"]);
    });
  });

  describe("validateWith", () => {
    class CustomUrlValidator extends Validator {
      constructor(options?: ValidatorOptions) {
        super(options);
      }

      protected doValidate(entity: any, attrName: string): void {
        if (_.isEmpty(entity.url())) {
          entity.errors().add("url", "Url cannot be blank");
        }
      }
    }

    class NameValidator extends Validator {
      constructor(options?: ValidatorOptions) {
        super(options);
      }

      protected doValidate(entity: any, attrName: string): void {
        if (_.isEmpty(entity.url())) {
          entity.errors().add("name", "Name cannot be blank");
        }
      }
    }

    //tslint:disable-next-line
    interface Material extends ValidatableMixin {
    }

    class Material implements ValidatableMixin {
      url: Stream<string>;
      name: Stream<string>;

      constructor(url: string, name: string) {
        ValidatableMixin.call(this);
        this.url  = Stream(url);
        this.name = Stream(name);
        this.validateWith<CustomUrlValidator>(new CustomUrlValidator(), "url");
        this.validateWith<NameValidator>(new NameValidator(), "name");
      }
    }

    applyMixins(Material, ValidatableMixin);

    it("should validate with custom validator", () => {
      const material = new Material("", "");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("url")).toEqual(["Url cannot be blank"]);
      expect(material.errors().errors("name")).toEqual(["Name cannot be blank"]);
    });
  });

  describe("validateAssociated", () => {
    //tslint:disable-next-line
    interface Material extends ValidatableMixin {
    }

    class Material implements ValidatableMixin {
      url: Stream<string>;

      constructor(url: string) {
        ValidatableMixin.call(this);
        this.url = Stream(url);
        this.validatePresenceOf("url");
      }
    }

    applyMixins(Material, ValidatableMixin);

    //tslint:disable-next-line
    interface Pipeline extends ValidatableMixin {
    }

    class Pipeline implements ValidatableMixin {
      material: Stream<Material>;

      constructor(material: Material) {
        ValidatableMixin.call(this);
        this.material = Stream(material);
        this.validateAssociated("material");
      }
    }

    applyMixins(Pipeline, ValidatableMixin);

    it("should validate associated attributes", () => {
      const pipeline = new Pipeline(new Material(""));

      pipeline.isValid();

      expect(pipeline.errors().hasErrors()).toBe(false);
      expect(pipeline.material().errors().hasErrors()).toBe(true);
      expect(pipeline.material().errors().errors("url")).toEqual(["URL must be present"]);
    });

  });

  describe("validate", () => {
    //tslint:disable-next-line
    interface Material extends ValidatableMixin {
    }

    class Material implements ValidatableMixin {
      username: Stream<string>;
      password: Stream<string>;

      constructor(username: string, password: string) {
        ValidatableMixin.call(this);
        this.username = Stream("");
        this.password = Stream("");
        this.validatePresenceOf("username");
        this.validatePresenceOf("password");
      }
    }

    applyMixins(Material, ValidatableMixin);

    it("should validate the model", () => {
      const material = new Material("", "");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("username")).toEqual(["Username must be present"]);
      expect(material.errors().errors("password")).toEqual(["Password must be present"]);
    });

    it("should be able validate a single attribute", () => {
      const material = new Material("", "");

      material.validate("username");

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("username")).toEqual(["Username must be present"]);
      expect(material.errors().hasErrors("password")).toBe(false);
    });

    it("should clear existing errors before validating", () => {
      const material = new Material("", "");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("username")).toEqual(["Username must be present"]);
      expect(material.errors().errors("password")).toEqual(["Password must be present"]);

      material.validate();

      expect(material.errors().errors("username")).toEqual(["Username must be present"]);
      expect(material.errors().errors("password")).toEqual(["Password must be present"]);
    });

    it("should clear existing errors for a given attribute before validating", () => {
      const material = new Material("", "");

      material.validate();

      expect(material.errors().hasErrors()).toBe(true);
      expect(material.errors().errors("username")).toEqual(["Username must be present"]);
      expect(material.errors().errors("password")).toEqual(["Password must be present"]);

      material.username("somename");
      material.validate("username");

      expect(material.errors().hasErrors("username")).toBe(false);
      expect(material.errors().errors("password")).toEqual(["Password must be present"]);
    });
  });

  describe("isValid", () => {
    //tslint:disable-next-line
    interface Variable extends ValidatableMixin {
    }

    class Variable implements ValidatableMixin {
      key: Stream<string>;
      parent: Stream<Variables>;
      name: Stream<string>;

      constructor(key: string) {
        ValidatableMixin.call(this);
        this.key    = Stream(key);
        this.name   = Stream("");
        this.parent = Stream();
        this.validatePresenceOf("name");
        this.validateUniquenessOf("key", () => this.parent().variables().filter((variable) => this !== variable));
      }
    }

    class Variables {
      variables: Stream<Variable[]>;

      constructor(variables: Variable[]) {
        this.variables = Stream(variables);
      }
    }

    applyMixins(Variable, ValidatableMixin);

    it("should validate the model", () => {
      const var1      = new Variable("foo");
      const var2      = new Variable("foo");
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var2.parent(variables);

      var1.isValid();

      expect(var1.errors().hasErrors()).toBe(true);
      expect(var1.errors().errors("key")).toEqual(["Key is a duplicate"]);
      expect(var1.errors().errors("name")).toEqual(["Name must be present"]);
    });

    it("should be invalid if associated attributes are invalid", () => {
      //tslint:disable-next-line
      interface Material extends ValidatableMixin {
      }

      class Material implements ValidatableMixin {
        url: Stream<string>;

        constructor(url: string) {
          ValidatableMixin.call(this);
          this.url = Stream(url);
          this.validatePresenceOf("url");
        }
      }

      applyMixins(Material, ValidatableMixin);

      //tslint:disable-next-line
      interface Task extends ValidatableMixin {
      }

      class Task implements ValidatableMixin {
        command: Stream<string>;

        constructor(command: string) {
          ValidatableMixin.call(this);
          this.command = Stream(command);
          this.validatePresenceOf("command");
        }
      }

      applyMixins(Task, ValidatableMixin);

      //tslint:disable-next-line
      interface Pipeline extends ValidatableMixin {
      }

      class Pipeline implements ValidatableMixin {
        material: Stream<Material>;
        task: Stream<Task>;

        constructor(material: Material, task: Task) {
          ValidatableMixin.call(this);
          this.material = Stream(material);
          this.task     = Stream(task);
          this.validateAssociated("material");
          this.validateAssociated("task");
        }
      }

      applyMixins(Pipeline, ValidatableMixin);

      const pipeline = new Pipeline(new Material(""), new Task("some command"));
      expect(pipeline.isValid()).toBe(false);
      expect(pipeline.errors().hasErrors()).toBe(false);
      expect(pipeline.material().errors().hasErrors()).toBe(true);
      expect(pipeline.task().errors().hasErrors()).toBe(false);
    });
  });

  describe("validateMaxLength", () => {

    //tslint:disable-next-line
    interface Variable extends ValidatableMixin {
    }

    class Variable implements ValidatableMixin {
      key: Stream<string>;

      constructor(key: string) {
        ValidatableMixin.call(this);
        this.key = Stream(key);
        this.validateMaxLength("key", 10);
      }
    }

    applyMixins(Variable, ValidatableMixin);

    it("should validate max length of a given field", () => {
      const var1 = new Variable("strGreaterThanTenChars");

      var1.validate();

      expect(var1.errors().hasErrors()).toBe(true);
      expect(var1.errors().errors("key")).toEqual(["Key must not exceed length 10"]);
    });

    it("should skip validation if attribute is empty", () => {
      const var1 = new Variable("");

      var1.validate();

      expect(var1.errors().hasErrors()).toBe(false);
    });

    it("should not give validation errors if field has valid length", () => {
      const var1 = new Variable("foobarbaz");

      var1.validate();

      expect(var1.errors().hasErrors()).toBe(false);
    });

  });

  describe("Error Message", () => {
    it("duplicate", () => {
      expect(ErrorMessages.duplicate("id")).toEqual("Id is a duplicate");
      expect(ErrorMessages.duplicate("PluginId")).toEqual("Plugin id is a duplicate");
      expect(ErrorMessages.duplicate("pluginId")).toEqual("Plugin id is a duplicate");
      expect(ErrorMessages.duplicate("plugin_id")).toEqual("Plugin id is a duplicate");
      expect(ErrorMessages.duplicate("plugin id")).toEqual("Plugin id is a duplicate");
    });

    it("mustBePresent", () => {
      expect(ErrorMessages.mustBePresent("id")).toEqual("Id must be present");
      expect(ErrorMessages.mustBePresent("PluginId")).toEqual("Plugin id must be present");
      expect(ErrorMessages.mustBePresent("pluginId")).toEqual("Plugin id must be present");
      expect(ErrorMessages.mustBePresent("plugin_id")).toEqual("Plugin id must be present");
      expect(ErrorMessages.mustBePresent("plugin id")).toEqual("Plugin id must be present");
    });

    it("mustBeAUrl", () => {
      expect(ErrorMessages.mustBeAUrl("url")).toEqual("Url must be a valid http(s) url");
      expect(ErrorMessages.mustBeAUrl("SomeUrl")).toEqual("Some url must be a valid http(s) url");
      expect(ErrorMessages.mustBeAUrl("someUrl")).toEqual("Some url must be a valid http(s) url");
      expect(ErrorMessages.mustBeAUrl("some_url")).toEqual("Some url must be a valid http(s) url");
      expect(ErrorMessages.mustBeAUrl("some url")).toEqual("Some url must be a valid http(s) url");
    });

    it("mustBePositiveNumber", () => {
      expect(ErrorMessages.mustBePositiveNumber("counter")).toEqual("Counter must be a positive integer");
      expect(ErrorMessages.mustBePositiveNumber("StageCounter")).toEqual("Stage counter must be a positive integer");
      expect(ErrorMessages.mustBePositiveNumber("stageCounter")).toEqual("Stage counter must be a positive integer");
      expect(ErrorMessages.mustBePositiveNumber("stage_counter")).toEqual("Stage counter must be a positive integer");
      expect(ErrorMessages.mustBePositiveNumber("stage counter")).toEqual("Stage counter must be a positive integer");
    });

    it("mustContainString", () => {
      expect(ErrorMessages.mustContainString("id", "foo")).toEqual("Id must contain the string 'foo'");
      expect(ErrorMessages.mustContainString("PluginId", "foo")).toEqual("Plugin id must contain the string 'foo'");
      expect(ErrorMessages.mustContainString("pluginId", "foo")).toEqual("Plugin id must contain the string 'foo'");
      expect(ErrorMessages.mustContainString("plugin_id", "foo")).toEqual("Plugin id must contain the string 'foo'");
      expect(ErrorMessages.mustContainString("plugin id", "foo")).toEqual("Plugin id must contain the string 'foo'");
    });

    it("mustNotExceedMaxLength", () => {
      expect(ErrorMessages.mustNotExceedMaxLength("id", 255)).toEqual("Id must not exceed length 255");
      expect(ErrorMessages.mustNotExceedMaxLength("PluginId", 255)).toEqual("Plugin id must not exceed length 255");
      expect(ErrorMessages.mustNotExceedMaxLength("pluginId", 255)).toEqual("Plugin id must not exceed length 255");
      expect(ErrorMessages.mustNotExceedMaxLength("plugin_id", 255)).toEqual("Plugin id must not exceed length 255");
      expect(ErrorMessages.mustNotExceedMaxLength("plugin id", 255)).toEqual("Plugin id must not exceed length 255");
    });
  });
});
