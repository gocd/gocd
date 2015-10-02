/*
 * Copyright 2015 ThoughtWorks, Inc.
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

define(['lodash', 'string-plus'], function (_, s) {

  var Mixins = {};

  Mixins.Errors = function () {
    var errors = {};

    this.add = function (attrName, message) {
      errors[attrName] = errors[attrName] || [];
      errors[attrName].push(message);
    };

    this.clear = function () {
      errors = {};
    };

    this.errors = function (optionalAttribute) {
      if (this._isEmpty()) {
        return;
      }

      if (optionalAttribute) {
        return errors[optionalAttribute];
      }

      return errors;
    };

    this._isEmpty = function () {
      return _.isEmpty(errors);
    };

    this.errorsForDisplay = function (attrName) {
      return _.map(errors[attrName] || [], function (message) {
        return message + ".";
      }).join(" ");
    };
  };

  Mixins.HasUUID = function () {
    this.uuid = Mixins.GetterSetter(this.constructor.modelType + '-' + s.uuid());
  };

  Mixins.HasMany = function (options) {
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

    this['remove' + associationName] = function (variable) {
      _.remove(collection(), variable);
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

    this['map' + associationNamePlural] = function (cb, thisArg) {
      return _.map(collection(), cb, thisArg);
    };

    this['collect' + associationName + 'Property'] = function (propName) {
      return this['map' + associationNamePlural](function (child) {
        return child[propName]();
      });
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
    duplicate:     function (attribute) {
      return s.humanize(attribute) + " is a duplicate";
    },
    mustBePresent: function (attribute) {
      return s.humanize(attribute).replace(/\bxpath\b/i, 'XPath').replace(/\burl\b/i, 'URL') + " must be present";
    },
    mustBeAUrl:    function (attribute) {
      return s.humanize(attribute) + " must be a valid http(s) url";
    },

    mustContainString: function(attribute, string){
      return s.humanize(attribute) + " must contain the string '" + string + "'";
    }
  };

  return Mixins;
});
