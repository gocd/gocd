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
const Tabs        = function (data) {
  Mixins.HasMany.call(this, {factory: Tabs.Tab.create, as: 'Tab', collection: data, uniqueOn: 'name'});
};

Tabs.Tab = function (data) {
  this.constructor.modelType = 'tab';
  Mixins.HasUUID.call(this);
  Validatable.call(this, data);

  this.parent = Mixins.GetterSetter();

  this.name = Stream(s.defaultToIfBlank(data.name, ''));
  this.path = Stream(s.defaultToIfBlank(data.path, ''));

  this.isBlank = function () {
    return s.isBlank(this.name()) && s.isBlank(this.path());
  };

  this.validatePresenceOf('name', {
    condition(property) {
      return (!s.isBlank(property.path()));
    }
  });
  this.validateUniquenessOf('name');
};

Tabs.Tab.create = (data) => new Tabs.Tab(data);

Mixins.fromJSONCollection({
  parentType: Tabs,
  childType:  Tabs.Tab,
  via:        'addTab'
});

Tabs.Tab.fromJSON = (data) => new Tabs.Tab(_.pick(data, ['name', 'path']));

module.exports = Tabs;
