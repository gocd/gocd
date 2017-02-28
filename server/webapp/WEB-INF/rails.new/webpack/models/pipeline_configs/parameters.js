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

var Stream      = require('mithril/stream');
var _           = require('lodash');
var s           = require('string-plus');
var Mixins      = require('models/mixins/model_mixins');
var Validatable = require('models/mixins/validatable_mixin');

var Parameters = function (data) {
  Mixins.HasMany.call(this, {
    factory:    Parameters.Parameter.create,
    as:         'Parameter',
    collection: data,
    uniqueOn:   'name'
  });
};

Parameters.Parameter = function (data) {
  this.constructor.modelType = 'parameter';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.name  = Stream(s.defaultToIfBlank(data.name, ''));
  this.value = Stream(s.defaultToIfBlank(data.value, ''));

  this.isBlank = function () {
    return s.isBlank(this.name()) && s.isBlank(this.value());
  };

  this.validatePresenceOf('name', {
    condition: function (property) {
      return (!s.isBlank(property.value()));
    }
  });
  this.validateUniquenessOf('name');
};

Parameters.Parameter.create = function (data) {
  return new Parameters.Parameter(data);
};

Mixins.fromJSONCollection({
  parentType: Parameters,
  childType:  Parameters.Parameter,
  via:        'addParameter'
});

Parameters.Parameter.fromJSON = function (data) {
  return new Parameters.Parameter(_.pick(data, ['name', 'value']));
};

module.exports = Parameters;
