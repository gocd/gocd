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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins', 'models/validatable_mixin'], function (m, _, s, Mixins, Validatable) {

  var Properties = function (data) {
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

    this.name   = m.prop(s.defaultToIfBlank(data.name, ''));
    this.source = m.prop(s.defaultToIfBlank(data.source, ''));
    this.xpath  = m.prop(s.defaultToIfBlank(data.xpath, ''));

    this.isBlank = function () {
      return s.isBlank(this.name()) && s.isBlank(this.source()) && s.isBlank(this.xpath());
    };

    this.validatePresenceOf('name', {condition: function(property) {return (!s.isBlank(property.source()) || !s.isBlank(property.xpath()));}});
    this.validateUniquenessOf('name');
    this.validatePresenceOf('source', {condition: function(property) {return !property.isBlank();}});
    this.validatePresenceOf('xpath',  {condition: function(property) {return !property.isBlank();}});
  };

  Properties.Property.create = function (data) {
    return new Properties.Property(data);
  };

  Mixins.fromJSONCollection({
    parentType: Properties,
    childType:  Properties.Property,
    via:        'addProperty'
  });


  Properties.Property.fromJSON = function (data) {
    return new Properties.Property(_.pick(data, ['name', 'source', 'xpath']));
  };

  return Properties;
});
