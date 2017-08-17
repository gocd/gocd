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

const _      = require('lodash');
const s      = require('string-plus');
const Stream = require('mithril/stream');

const Mixins = {};

Mixins.HasUUID = function () {
  this.uuid = Mixins.GetterSetter(`${this.constructor.modelType}-${s.uuid()}`);
};

Mixins.HasEncryptedAttribute = function (options) {
  const _value          = options.attribute, name            = options.name, capitalizedName = _.upperFirst(name);

  this[name] = function(...args) {
    return _value().value(...args);
  };

  this[`isSecure${capitalizedName}`] = () => _value().isSecure();

  this[`isPlain${capitalizedName}`] = () => _value().isPlain();

  this[`edit${capitalizedName}`] = () => {
    _value().edit();
  };

  this[`isDirty${capitalizedName}`] = () => _value().isDirty();

  this[`isEditing${capitalizedName}`] = () => _value().isEditing();

  this[`resetToOriginal${capitalizedName}`] = () => _value().resetToOriginal();

  this[`becomeSecure${capitalizedName}`] = () => _value().becomeSecure();

  this[`becomeUnSecure${capitalizedName}`] = () => _value().becomeUnSecure();
};

Mixins.HasMany = function (options) {
  Mixins.HasUUID.call(this);
  const factory               = options.factory;
  const associationName       = options.as;
  const associationNamePlural = s.defaultToIfBlank(options.plural, `${options.as}s`);
  const uniqueOn              = options.uniqueOn;
  const collection            = Stream(s.defaultToIfBlank(options.collection, []));

  this.toJSON = () => _(collection()).map((item) => item.isBlank && item.isBlank() ? null : item).compact().value();

  this[`add${associationName}`] = (instance) => {
    collection().push(instance);
  };

  this[`create${associationName}`] = function (options) {
    const instance = factory(options || {});
    instance.parent(this);

    this[`add${associationName}`](instance);
    return instance;
  };

  this[`remove${associationName}`] = (thing) => {
    _.remove(collection(), thing);
  };

  this[`first${associationName}`] = () => _.first(collection());

  this[`${_.camelCase(associationName)}AtIndex`] = (index) => collection()[index];

  this[`set${associationNamePlural}`] = (newItems) => collection(newItems);

  this[`count${associationName}`] = () => collection().length;

  this[`isEmpty${associationName}`] = () => collection().length === 0;

  this[`indexOf${associationName}`] = (thing) => _.indexOf(collection(), thing);

  this[`previous${associationName}`] = function (thing) {
    return collection()[this[`indexOf${associationName}`](thing) - 1];
  };

  this[`last${associationName}`] = () => _.last(collection());

  this[`find${associationName}`] = (cb, thisArg) => _.find(collection(), cb, thisArg);

  this[`filter${associationName}`] = (cb, thisArg) => _.filter(collection(), cb, thisArg);

  this[`map${associationNamePlural}`] = (cb, thisArg) => _.map(collection(), cb, thisArg);

  this[`each${associationName}`] = (cb, thisArg) => {
    _.each(collection(), cb, thisArg);
  };

  this[`sortBy${associationNamePlural}`] = (cb, thisArg) => _.sortBy(collection(), cb, thisArg);

  this[`groupBy${associationName}Property`] = (propName) => _.groupBy(collection(), propName);

  this[`every${associationName}`] = (cb, thisArg) => _.every(collection(), cb, thisArg);

  this[`collect${associationName}Property`] = function (propName) {
    return this[`map${associationNamePlural}`]((child) => child[propName]());
  };

  this.validate = () => {
    _.forEach(collection(), (item) => item.validate());
  };

  this.isValid = () => _.every(collection(), (item) => item.isValid());

  this.isUnique = function (childModel, uniqueOn) {
    if (_.isNil(childModel[uniqueOn]()) || _.isEmpty(childModel[uniqueOn]())) {
      return true;
    }

    const occurences = _.countBy(this[`collect${associationName}Property`](uniqueOn));
    return (occurences[childModel[uniqueOn]()] <= 1);
  };

  if (uniqueOn) {
    this[`validateUnique${associationName}${_.capitalize(uniqueOn)}`] = function (childModel, errors) {
      const occurences = _.countBy(this[`collect${associationName}Property`](uniqueOn));
      if (occurences[childModel[uniqueOn]()] > 1) {
        errors.add(uniqueOn, Mixins.ErrorMessages.duplicate(uniqueOn));
      }
    };
  }
};

Mixins.fromJSONCollection = (options) => {
  const parentType     = options.parentType;
  const childType      = options.childType;
  const addChildMethod = options.via;

  parentType.fromJSON = (data) => {
    const parentInstance = new parentType();
    if (!_.isEmpty(data)) {
      const assignParent = (childInstance) => {
        childInstance.parent(parentInstance);
        return childInstance;
      };
      _.map(data, _.flow(childType.fromJSON, assignParent, parentInstance[addChildMethod]));
    }
    return parentInstance;
  };
};

// copy of mithri's Stream without the toJSON on the getterSetter.
Mixins.GetterSetter = (store) => function(...args) {
  if (args.length) {
    store = args[0];
  }
  return store;
};

Mixins.TogglingGetterSetter = (store) => function(...args) {
  if (args.length) {
    store(store() === args[0] ? undefined : args[0]);
  }
  return store();
};

Mixins.Validations = {};

Mixins.ErrorMessages = {
  duplicate(attribute) {
    return `${s.humanize(attribute)} is a duplicate`;
  },
  mustBePresent(attribute) {
    return `${s.humanize(attribute).replace(/\bxpath\b/i, 'XPath').replace(/\burl\b/i, 'URL')} must be present`;
  },
  mustBeAUrl(attribute) {
    return `${s.humanize(attribute)} must be a valid http(s) url`;
  },
  mustBePositiveNumber(attribute) {
    return `${s.humanize(attribute)} must be a positive integer`;
  },
  mustContainString(attribute, string) {
    return `${s.humanize(attribute)} must contain the string '${string}'`;
  }
};

module.exports = Mixins;
