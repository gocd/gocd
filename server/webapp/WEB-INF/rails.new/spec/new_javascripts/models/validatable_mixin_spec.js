/*
 * Copyright 2016 ThoughtWorks, Inc.
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

define(['mithril', 'lodash', 'string-plus', 'models/validatable_mixin', 'models/model_mixins'], function (m, _, s, Validatable, Mixins) {
  describe('Validatable', function () {
    describe('errors', function () {
      var Material = function (data) {
        Validatable.call(this, data);
      };

      it('should map errors', function () {
        var material = new Material({errors: {url: ["Cannot be blank"]}});

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('url')).toEqual(["Cannot be blank"]);
      });
    });

    describe('validatePresenceOf', function () {
      var Material = function (data) {
        Validatable.call(this, data);
        this.name = m.prop('');

        this.validatePresenceOf('name');
      };

      it('should validate the presence of a given field', function () {
        var material = new Material({});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should be conditional', function () {
        var EnvVariable = function (data) {
          Validatable.call(this, data);
          this.name = m.prop('');
          this.value = m.prop('');

          this.validatePresenceOf('value', {condition: function (variable) {return !s.isBlank(variable.name());}});
        };

        var variable = new EnvVariable({});

        variable.validate();

        expect(variable.errors()._isEmpty()).toBe(true);
      });
    });

    describe('validateUniquenessOf', function () {
      var Variables = function (data) {
        Mixins.HasMany.call(this, {
          factory: Variables.Variable.create,
          as: 'Variable',
          collection: data,
          uniqueOn: 'name'
        });
      };

      Variables.Variable = function (data) {
        Validatable.call(this, data);

        this.key = m.prop(data.key);
        this.parent = Mixins.GetterSetter();

        this.validateUniquenessOf('key');
      };

      it('should validate uniqueness of a given field', function () {
        var var1 = new Variables.Variable({key: 'name'});
        var var2 = new Variables.Variable({key: 'name'});
        var variables = new Variables([var1, var2]);
        var1.parent(variables);
        var1.parent(variables);

        var1.validate();

        expect(var1.errors()._isEmpty()).toBe(false);
        expect(var1.errors().errors('key')).toEqual(["Key is a duplicate"]);
      });

      it('should skip validation in absence of parent', function () {
        var variable = new Variables.Variable({key: 'name'});

        variable.validate();

        expect(variable.errors()._isEmpty()).toBe(true);
      });

      it('should skip validation if attribute is empty', function () {
        var var1 = new Variables.Variable({key: ''});
        var var2 = new Variables.Variable({key: ''});
        var variables = new Variables([var1, var2]);
        var1.parent(variables);
        var1.parent(variables);

        var1.validate();

        expect(var1.errors()._isEmpty()).toBe(true);
      });
    });

    describe('validateUrlPattern', function () {
      var Material = function (data) {
        Validatable.call(this, data);
        this.url = m.prop(data.url);

        this.validateUrlPattern('url');
      };

      it('should validate url pattern', function () {
        var material = new Material({url: 'ftp://some.com'});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('url')).toEqual(['Url must be a valid http(s) url']);
      });

      it('should skip validation for empty attribute', function () {
        var material = new Material({url: ''});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(true);
      });
    });

    describe('validateFormatOf', function () {
      var Material = function (data) {
        Validatable.call(this, data);
        this.url = m.prop(data.url);

        this.validateFormatOf('url', {format: /^http(s)?:\/\/.+/, message: 'Url format is invalid'});
      };

      it('should validate the format of field', function () {
        var material = new Material({url: 'ftp://some.com'});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('url')).toEqual(['Url format is invalid']);
      });

      it('should skip validation for empty attribute', function () {
        var material = new Material({url: ''});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(true);
      });
    });

    describe('validateWith', function () {
      var Material = function (data) {
        Validatable.call(this, data);
        this.name = m.prop(data.name);
        this.url = m.prop(data.url);

        var UrlValidator = function () {
          this.validate = function (entity) {
            if (_.isEmpty(entity.url())) {
              entity.errors().add('url', 'Url cannot be blank');
            }
          };
        };

        var NameValidator = function () {
          this.validate = function (entity) {
            if (_.isEmpty(entity.url())) {
              entity.errors().add('name', 'Name cannot be blank');
            }
          };
        };

        this.validateWith('url', UrlValidator);
        this.validateWith('name', NameValidator);
      };

      it('should validate with custom validator', function () {
        var material = new Material({});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('url')).toEqual(['Url cannot be blank']);
        expect(material.errors().errors('name')).toEqual(['Name cannot be blank']);
      });
    });

    describe('validateAssociated', function () {
      var Material = function (data) {
        Validatable.call(this, data);
        this.url = m.prop(data.url);

        this.validatePresenceOf('url');
      };

      var Pipeline = function (data) {
        Validatable.call(this, data);
        this.material = m.prop(new Material(data.material));

        this.validateAssociated('material');
      };

      it('should validate associated attributes', function () {
        var pipeline = new Pipeline({material: {}});

        pipeline.isValid();

        expect(pipeline.errors()._isEmpty()).toBe(true);
        expect(pipeline.material().errors()._isEmpty()).toBe(false);
        expect(pipeline.material().errors().errors('url')).toEqual(['URL must be present']);
      });

    });

    describe('validate', function () {
      var Material = function (data) {
        Validatable.call(this, data);
        this.username = m.prop('');
        this.password = m.prop('');

        this.validatePresenceOf('username');
        this.validatePresenceOf('password');
      };

      it('should validate the model', function () {
        var material = new Material({});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('username')).toEqual(['Username must be present']);
        expect(material.errors().errors('password')).toEqual(['Password must be present']);
      });

      it('should be able validate a single attribute', function () {
        var material = new Material({});

        material.validate('username');

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('username')).toEqual(['Username must be present']);
        expect(material.errors().hasErrors('password')).toBe(false);
      });

      it('should clear existing errors before validating', function () {
        var material = new Material({});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('username')).toEqual(['Username must be present']);
        expect(material.errors().errors('password')).toEqual(['Password must be present']);

        material.validate();

        expect(material.errors().errors('username')).toEqual(['Username must be present']);
        expect(material.errors().errors('password')).toEqual(['Password must be present']);
      });

      it('should clear existing errors for a given attribute before validating', function () {
        var material = new Material({});

        material.validate();

        expect(material.errors()._isEmpty()).toBe(false);
        expect(material.errors().errors('username')).toEqual(['Username must be present']);
        expect(material.errors().errors('password')).toEqual(['Password must be present']);

        material.username('somename');
        material.validate('username');

        expect(material.errors().hasErrors('username')).toBe(false);
        expect(material.errors().errors('password')).toEqual(['Password must be present']);
      });
    });

    describe('isValid', function () {
      var Variables = function (data) {
        Mixins.HasMany.call(this, {
          factory: Variables.Variable.create,
          as: 'Variable',
          collection: data,
          uniqueOn: 'name'
        });
      };

      Variables.Variable = function (data) {
        Validatable.call(this, data);

        this.key  = m.prop(data.key);
        this.name = m.prop();
        this.parent = Mixins.GetterSetter();

        this.validatePresenceOf('name');
        this.validateUniquenessOf('key');
      };

      it('should validate the model', function () {
        var var1 = new Variables.Variable({key: 'name'});
        var var2 = new Variables.Variable({key: 'name'});
        var variables = new Variables([var1, var2]);
        var1.parent(variables);
        var1.parent(variables);

        var1.isValid();

        expect(var1.errors()._isEmpty()).toBe(false);
        expect(var1.errors().errors('key')).toEqual(['Key is a duplicate']);
        expect(var1.errors().errors('name')).toEqual(['Name must be present']);
      });

      it('should be invalid if associated attributes are invalid', function () {
        var Material = function (data) {
          Validatable.call(this, data);
          this.url = m.prop(data.url);

          this.validatePresenceOf('url');
        };

        var Task = function (data) {
          Validatable.call(this, data);
          this.command = m.prop(data.command);

          this.validatePresenceOf('command');
        };

        var Pipeline = function (data) {
          Validatable.call(this, data);
          this.material = m.prop(new Material(data.material));
          this.task     = m.prop(new Task(data.task));

          this.validateAssociated('material');
          this.validateAssociated('task');
        };

        var pipeline = new Pipeline({material: {}, task: {command: 'some_command'}});

        expect(pipeline.isValid()).toBe(false);
        expect(pipeline.errors()._isEmpty()).toBe(true);
        expect(pipeline.material().errors()._isEmpty()).toBe(false);
        expect(pipeline.task().errors()._isEmpty()).toBe(true);
      });
    });
  });
});