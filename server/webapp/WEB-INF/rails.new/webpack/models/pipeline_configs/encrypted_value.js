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

var Stream         = require('mithril/stream');
var _              = require('lodash');
var s              = require('string-plus');
var Mixins         = require('models/mixins/model_mixins');
var EncryptedValue = function (data) {
  this.constructor.modelType = 'encryptedValue';
  Mixins.HasUUID.call(this);

  if (_.has(data, 'clearText') && _.has(data, 'cipherText')) {
    throw "You cannot initialize an encrypted value with both clear text and cipher text!";
  }

  var _originalValue = Stream(_.has(data, 'cipherText') ? data.cipherText : data.clearText);
  var _value         = Stream(_.has(data, 'cipherText') ? data.cipherText : data.clearText);
  var _isEncrypted   = Stream(_.has(data, 'cipherText'));
  var _canEdit       = Stream(!_isEncrypted());

  this.value = function(...args) {
    if (args.length) {
      if (this.isPlain()) {
        return _value(args[0]);
      } else {
        if (_canEdit()) {
          return _value(args[0]);
        } else {
          throw "You cannot edit a cipher text value!";
        }
      }
    }
    return _value();
  };

  this.edit = () => {
    _canEdit(true);
    _value('');
  };

  this.isEditing = () => _canEdit();

  this.isDirty = function () {
    return this.value() !== _originalValue();
  };

  this.isPlain = function () {
    return !this.isSecure();
  };

  this.isSecure = () => _isEncrypted();

  this.becomeSecure = () => {
    _isEncrypted(true);
    _canEdit(false);
  };

  this.becomeUnSecure = () => {
    _isEncrypted(false);
    _canEdit(true);
  };

  this.resetToOriginal = function () {
    if (s.isBlank(_originalValue())) {
      this.edit();
    } else {
      _value(_originalValue());
      _canEdit(false);
    }
  };
};

module.exports = EncryptedValue;
