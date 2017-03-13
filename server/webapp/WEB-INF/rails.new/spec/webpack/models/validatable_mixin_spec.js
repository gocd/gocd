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
describe('Validatable', () => {

  const Stream      = require('mithril/stream');
  const _           = require('lodash');
  const s           = require('string-plus');
  const Validatable = require('models/mixins/validatable_mixin');
  const Mixins      = require('models/mixins/model_mixins');

  describe('errors', () => {
    const Material = function (data) {
      Validatable.call(this, data);
    };

    it('should map errors', () => {
      const material = new Material({errors: {url: ["Cannot be blank"]}});

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('url')).toEqual(["Cannot be blank"]);
    });
  });

  describe('validatePresenceOf', () => {
    const Material = function (data) {
      Validatable.call(this, data);
      this.name = Stream('');

      this.validatePresenceOf('name');
    };

    it('should validate the presence of a given field', () => {
      const material = new Material({});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('name')).toEqual(['Name must be present']);
    });

    it('should be conditional', () => {
      const EnvVariable = function (data) {
        Validatable.call(this, data);
        this.name  = Stream('');
        this.value = Stream('');

        this.validatePresenceOf('value', {
          condition(variable) {
            return !s.isBlank(variable.name());
          }
        });
      };

      const variable = new EnvVariable({});

      variable.validate();

      expect(variable.errors()._isEmpty()).toBe(true);
    });
  });

  describe('validateUniquenessOf', () => {
    const Variables = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Variables.Variable.create,
        as:         'Variable',
        collection: data,
        uniqueOn:   'name'
      });
    };

    Variables.Variable = function (data) {
      Validatable.call(this, data);

      this.key    = Stream(data.key);
      this.parent = Mixins.GetterSetter();

      this.validateUniquenessOf('key');
    };

    it('should validate uniqueness of a given field', () => {
      const var1      = new Variables.Variable({key: 'name'});
      const var2      = new Variables.Variable({key: 'name'});
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var1.parent(variables);

      var1.validate();

      expect(var1.errors()._isEmpty()).toBe(false);
      expect(var1.errors().errors('key')).toEqual(["Key is a duplicate"]);
    });

    it('should skip validation in absence of parent', () => {
      const variable = new Variables.Variable({key: 'name'});

      variable.validate();

      expect(variable.errors()._isEmpty()).toBe(true);
    });

    it('should skip validation if attribute is empty', () => {
      const var1      = new Variables.Variable({key: ''});
      const var2      = new Variables.Variable({key: ''});
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var1.parent(variables);

      var1.validate();

      expect(var1.errors()._isEmpty()).toBe(true);
    });
  });

  describe('validateUrlPattern', () => {
    const Material = function (data) {
      Validatable.call(this, data);
      this.url = Stream(data.url);

      this.validateUrlPattern('url');
    };

    it('should validate url pattern', () => {
      const material = new Material({url: 'ftp://some.com'});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('url')).toEqual(['Url must be a valid http(s) url']);
    });

    it('should skip validation for empty attribute', () => {
      const material = new Material({url: ''});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(true);
    });
  });

  describe('validateFormatOf', () => {
    const Material = function (data) {
      Validatable.call(this, data);
      this.url = Stream(data.url);

      this.validateFormatOf('url', {format: /^http(s)?:\/\/.+/, message: 'Url format is invalid'});
    };

    it('should validate the format of field', () => {
      const material = new Material({url: 'ftp://some.com'});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('url')).toEqual(['Url format is invalid']);
    });

    it('should skip validation for empty attribute', () => {
      const material = new Material({url: ''});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(true);
    });
  });

  describe('validateWith', () => {
    const Material = function (data) {
      Validatable.call(this, data);
      this.name = Stream(data.name);
      this.url  = Stream(data.url);

      const UrlValidator = function () {
        this.validate = (entity) => {
          if (_.isEmpty(entity.url())) {
            entity.errors().add('url', 'Url cannot be blank');
          }
        };
      };

      const NameValidator = function () {
        this.validate = (entity) => {
          if (_.isEmpty(entity.url())) {
            entity.errors().add('name', 'Name cannot be blank');
          }
        };
      };

      this.validateWith('url', UrlValidator);
      this.validateWith('name', NameValidator);
    };

    it('should validate with custom validator', () => {
      const material = new Material({});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('url')).toEqual(['Url cannot be blank']);
      expect(material.errors().errors('name')).toEqual(['Name cannot be blank']);
    });
  });

  describe('validateAssociated', () => {
    const Material = function (data) {
      Validatable.call(this, data);
      this.url = Stream(data.url);

      this.validatePresenceOf('url');
    };

    const Pipeline = function (data) {
      Validatable.call(this, data);
      this.material = Stream(new Material(data.material));

      this.validateAssociated('material');
    };

    it('should validate associated attributes', () => {
      const pipeline = new Pipeline({material: {}});

      pipeline.isValid();

      expect(pipeline.errors()._isEmpty()).toBe(true);
      expect(pipeline.material().errors()._isEmpty()).toBe(false);
      expect(pipeline.material().errors().errors('url')).toEqual(['URL must be present']);
    });

  });

  describe('validate', () => {
    const Material = function (data) {
      Validatable.call(this, data);
      this.username = Stream('');
      this.password = Stream('');

      this.validatePresenceOf('username');
      this.validatePresenceOf('password');
    };

    it('should validate the model', () => {
      const material = new Material({});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('username')).toEqual(['Username must be present']);
      expect(material.errors().errors('password')).toEqual(['Password must be present']);
    });

    it('should be able validate a single attribute', () => {
      const material = new Material({});

      material.validate('username');

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('username')).toEqual(['Username must be present']);
      expect(material.errors().hasErrors('password')).toBe(false);
    });

    it('should clear existing errors before validating', () => {
      const material = new Material({});

      material.validate();

      expect(material.errors()._isEmpty()).toBe(false);
      expect(material.errors().errors('username')).toEqual(['Username must be present']);
      expect(material.errors().errors('password')).toEqual(['Password must be present']);

      material.validate();

      expect(material.errors().errors('username')).toEqual(['Username must be present']);
      expect(material.errors().errors('password')).toEqual(['Password must be present']);
    });

    it('should clear existing errors for a given attribute before validating', () => {
      const material = new Material({});

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

  describe('isValid', () => {
    const Variables = function (data) {
      Mixins.HasMany.call(this, {
        factory:    Variables.Variable.create,
        as:         'Variable',
        collection: data,
        uniqueOn:   'name'
      });
    };

    Variables.Variable = function (data) {
      Validatable.call(this, data);

      this.key    = Stream(data.key);
      this.name   = Stream();
      this.parent = Mixins.GetterSetter();

      this.validatePresenceOf('name');
      this.validateUniquenessOf('key');
    };

    it('should validate the model', () => {
      const var1      = new Variables.Variable({key: 'name'});
      const var2      = new Variables.Variable({key: 'name'});
      const variables = new Variables([var1, var2]);
      var1.parent(variables);
      var1.parent(variables);

      var1.isValid();

      expect(var1.errors()._isEmpty()).toBe(false);
      expect(var1.errors().errors('key')).toEqual(['Key is a duplicate']);
      expect(var1.errors().errors('name')).toEqual(['Name must be present']);
    });

    it('should be invalid if associated attributes are invalid', () => {
      const Material = function (data) {
        Validatable.call(this, data);
        this.url = Stream(data.url);

        this.validatePresenceOf('url');
      };

      const Task = function (data) {
        Validatable.call(this, data);
        this.command = Stream(data.command);

        this.validatePresenceOf('command');
      };

      const Pipeline = function (data) {
        Validatable.call(this, data);
        this.material = Stream(new Material(data.material));
        this.task     = Stream(new Task(data.task));

        this.validateAssociated('material');
        this.validateAssociated('task');
      };

      const pipeline = new Pipeline({material: {}, task: {command: 'some_command'}});

      expect(pipeline.isValid()).toBe(false);
      expect(pipeline.errors()._isEmpty()).toBe(true);
      expect(pipeline.material().errors()._isEmpty()).toBe(false);
      expect(pipeline.task().errors()._isEmpty()).toBe(true);
    });
  });
});
