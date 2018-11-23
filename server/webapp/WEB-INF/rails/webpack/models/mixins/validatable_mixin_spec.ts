/*
 * Copyright 2018 ThoughtWorks, Inc.
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

import * as _ from "lodash";
import * as stream from "mithril/stream";
import {Stream} from "mithril/stream";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin, Validator, ValidatorOptions} from "models/mixins/new_validatable_mixin";
import * as s from "underscore.string";

describe("Validatable", () => {

  describe("validatePresenceOf", () => {

    it("should validate the presence of a given field", () => {
      //tslint:disable-next-line
      interface Material extends ValidatableMixin {
      }

      class Material implements ValidatableMixin {
        readonly name: string;

        constructor(name: string) {
          ValidatableMixin.call(this);
          this.name = name;
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
          this.name  = stream("");
          this.value = stream("");
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
        this.key    = stream(key);
        this.parent = stream();
        this.validateUniquenessOf("key", () => this.parent().variables);
      }
    }

    class Variables {
      variables: Stream<Variable[]>;

      constructor(variables: Variable[]) {
        this.variables = stream(variables);
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
  });

  describe("validateUrlPattern", () => {
    //tslint:disable-next-line
    interface Material extends ValidatableMixin {
    }

    class Material implements ValidatableMixin {
      url: Stream<string>;

      constructor(url: string) {
        ValidatableMixin.call(this);
        this.url = stream(url);
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
        this.url = stream(url);
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
        this.url  = stream(url);
        this.name = stream(name);
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
        this.url = stream(url);
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
        this.material = stream(material);
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
        this.username = stream("");
        this.password = stream("");
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
        this.key    = stream(key);
        this.name   = stream("");
        this.parent = stream();
        this.validatePresenceOf("name");
        this.validateUniquenessOf("key", () => this.parent().variables);
      }
    }

    class Variables {
      variables: Stream<Variable[]>;

      constructor(variables: Variable[]) {
        this.variables = stream(variables);
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
          this.url = stream(url);
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
          this.command = stream(command);
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
          this.material = stream(material);
          this.task     = stream(task);
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
});
