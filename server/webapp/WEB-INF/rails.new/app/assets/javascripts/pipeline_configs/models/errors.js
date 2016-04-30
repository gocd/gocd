/*
 *   Copyright 2016 ThoughtWorks, Inc.
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */

define(['lodash', 'string-plus', 'mithril'], function (_, s, m) {

  var Errors = function (data) {
    var errors = {};

    this.add = function (attrName, message) {
      if (_.isArray(message)) {
        message = _.join(message, ',');
      }
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

  }

  Errors.fromJson = function (data) {
    var errors = new Errors();
    if (_.has(data, 'errors')) {
      var data = data.errors;
      _.forOwn(data, function (value, key) {
        errors.add(_.camelCase(key), value);
      });
    }
    return errors;
  };

  return Errors;
});
