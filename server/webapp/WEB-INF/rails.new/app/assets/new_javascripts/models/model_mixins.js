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

define(['lodash', 'string-plus', 'mithril'], function (_, s, m) {

  var Mixins = {};

  Mixins.HasUUID = function () {
    this.uuid = Mixins.GetterSetter(this.constructor.modelType + '-' + s.uuid());
  };

  Mixins.HasEncryptedAttribute = function (options) {
    var _value          = options.attribute,
      name            = options.name,
      capitalizedName = _.upperFirst(name);

    this[name] = function () {
      return _value().value.apply(_value(), arguments);
    };

    this['isSecure' + capitalizedName] = function () {
      return _value().isSecure();
    };

    this['isPlain' + capitalizedName] = function () {
      return _value().isPlain();
    };

    this['edit' + capitalizedName] = function () {
      _value().edit();
    };

    this['isDirty' + capitalizedName] = function () {
      return _value().isDirty();
    };

    this['isEditing' + capitalizedName] = function () {
      return _value().isEditing();
    };

    this['resetToOriginal' + capitalizedName] = function () {
      return _value().resetToOriginal();
    };

    this['becomeSecure' + capitalizedName] = function () {
      return _value().becomeSecure();
    };

    this['becomeUnSecure' + capitalizedName] = function () {
      return _value().becomeUnSecure();
    };
  };

  Mixins.HasMany = function (options) {
    Mixins.HasUUID.call(this);
    var factory               = options.factory;
    var associationName       = options.as;
    var associationNamePlural = s.defaultToIfBlank(options.plural, options.as + 's');
    var uniqueOn              = options.uniqueOn;
    var collection            = m.prop(s.defaultToIfBlank(options.collection, []));

    this.toJSON = function () {
      return _(collection()).map(function (item) {
        return item.isBlank && item.isBlank() ? null : item;
      }).compact().value();
    };

    this['add' + associationName] = function (instance) {
      collection().push(instance);
    };

    this['create' + associationName] = function (options) {
      var instance = factory(options || {});
      instance.parent(this);

      this['add' + associationName](instance);
      return instance;
    };

    this['remove' + associationName] = function (thing) {
      _.remove(collection(), thing);
    };

    this['first' + associationName] = function () {
      return _.first(collection());
    };

    this[_.camelCase(associationName) + 'AtIndex'] = function (index) {
      return collection()[index];
    };

    this['set' + associationNamePlural] = function (newItems) {
      return collection(newItems);
    };

    this['count' + associationName] = function () {
      return collection().length;
    };

    this['isEmpty' + associationName] = function () {
      return collection().length === 0;
    };

    this['indexOf' + associationName] = function (thing) {
      return _.indexOf(collection(), thing);
    };

    this['previous' + associationName] = function (thing) {
      return collection()[this['indexOf' + associationName](thing) - 1];
    };

    this['last' + associationName] = function () {
      return _.last(collection());
    };

    this['find' + associationName] = function (cb, thisArg) {
      return _.find(collection(), cb, thisArg);
    };

    this['filter' + associationName] = function (cb, thisArg) {
      return _.filter(collection(), cb, thisArg);
    };

    this['map' + associationNamePlural] = function (cb, thisArg) {
      return _.map(collection(), cb, thisArg);
    };

    this['each' + associationName] = function (cb, thisArg) {
      _.each(collection(), cb, thisArg);
    };

    this['sortBy' + associationNamePlural] = function(cb, thisArg) {
      return _.sortBy(collection(), cb, thisArg);
    };

    this['every' + associationName] = function(cb, thisArg) {
      return _.every(collection(), cb, thisArg);
    };

    this['collect' + associationName + 'Property'] = function (propName) {
      return this['map' + associationNamePlural](function (child) {
        return child[propName]();
      });
    };

    this.validate = function () {
      _.forEach(collection(), function (item) {
        return item.validate();
      });
    };

    this.isValid = function () {
      return _.every(collection() , function (item) {
        return item.isValid();
      });
    };

    this.isUnique = function (childModel, uniqueOn) {
      if(_.isNil(childModel[uniqueOn]()) || _.isEmpty(childModel[uniqueOn]())) {
        return true;
      }

      var occurences = _.countBy(this['collect' + associationName + 'Property'](uniqueOn));
      return (occurences[childModel[uniqueOn]()] <= 1);
    };

    if (uniqueOn) {
      this['validateUnique' + associationName + _.capitalize(uniqueOn)] = function (childModel, errors) {
        var occurences = _.countBy(this['collect' + associationName + 'Property'](uniqueOn));
        if (occurences[childModel[uniqueOn]()] > 1) {
          errors.add(uniqueOn, Mixins.ErrorMessages.duplicate(uniqueOn));
        }
      };
    }
  };

  Mixins.fromJSONCollection = function (options) {
    var parentType     = options.parentType;
    var childType      = options.childType;
    var addChildMethod = options.via;

    parentType.fromJSON = function (data) {
      var parentInstance = new parentType();
      if (!_.isEmpty(data)) {
        var assignParent = function (childInstance) {
          childInstance.parent(parentInstance);
          return childInstance;
        };
        _.map(data, _.flow(childType.fromJSON, assignParent, parentInstance[addChildMethod]));
      }
      return parentInstance;
    };
  };

  // copy of mithri's m.prop without the toJSON on the getterSetter.
  Mixins.GetterSetter = function (store) {
    return function () {
      if (arguments.length) {
        store = arguments[0];
      }
      return store;
    };
  };

  Mixins.TogglingGetterSetter = function (store) {
    return function () {
      if (arguments.length) {
        store(store() === arguments[0] ? undefined : arguments[0]);
      }
      return store();
    };
  };

  Mixins.Validations = {};

  Mixins.ErrorMessages = {
    duplicate:            function (attribute) {
      return s.humanize(attribute) + " is a duplicate";
    },
    mustBePresent:        function (attribute) {
      return s.humanize(attribute).replace(/\bxpath\b/i, 'XPath').replace(/\burl\b/i, 'URL') + " must be present";
    },
    mustBeAUrl:           function (attribute) {
      return s.humanize(attribute) + " must be a valid http(s) url";
    },
    mustBePositiveNumber: function (attribute) {
      return s.humanize(attribute) + " must be a positive integer";
    },
    mustContainString:    function (attribute, string) {
      return s.humanize(attribute) + " must contain the string '" + string + "'";
    }
  };

  return Mixins;
});
