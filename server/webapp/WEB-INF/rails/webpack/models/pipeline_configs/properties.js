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

const Stream      = require('mithril/stream');
const _           = require('lodash');
const s           = require('string-plus');
const Mixins      = require('models/mixins/model_mixins');
const Validatable = require('models/mixins/validatable_mixin');

const Properties = function (data) {
  Mixins.HasMany.call(this, {
    factory:    Properties.Property.create,
    as:         'Property',
    plural:     'Properties',
    collection: data,
    uniqueOn:   'name'
  });
};

Properties.Property = function (data) {
  this.constructor.modelType = 'property';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.name   = Stream(s.defaultToIfBlank(data.name, ''));
  this.source = Stream(s.defaultToIfBlank(data.source, ''));
  this.xpath  = Stream(s.defaultToIfBlank(data.xpath, ''));

  this.isBlank = function () {
    return s.isBlank(this.name()) && s.isBlank(this.source()) && s.isBlank(this.xpath());
  };

  this.validatePresenceOf('name', {
    condition(property) {
      return (!s.isBlank(property.source()) || !s.isBlank(property.xpath()));
    }
  });
  this.validateUniquenessOf('name');
  this.validatePresenceOf('source', {
    condition(property) {
      return !property.isBlank();
    }
  });
  this.validatePresenceOf('xpath', {
    condition(property) {
      return !property.isBlank();
    }
  });
};

Properties.Property.create = (data) => new Properties.Property(data);

Mixins.fromJSONCollection({
  parentType: Properties,
  childType:  Properties.Property,
  via:        'addProperty'
});


Properties.Property.fromJSON = (data) => new Properties.Property(_.pick(data, ['name', 'source', 'xpath']));

module.exports = Properties;
