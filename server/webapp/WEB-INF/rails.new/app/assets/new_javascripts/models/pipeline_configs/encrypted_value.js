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

define(['mithril', 'lodash', 'string-plus', 'models/model_mixins'], function (m, _, s, Mixins) {
  var EncryptedValue = function (data) {
    this.constructor.modelType = 'encryptedValue';
    Mixins.HasUUID.call(this);

    if (_.has(data, 'clearText') && _.has(data, 'cipherText')) {
      throw "You cannot initialize an encrypted value with both clear text and cipher text!";
    }

    var _originalValue = m.prop(_.has(data, 'cipherText') ? data.cipherText : data.clearText);
    var _value         = m.prop(_.has(data, 'cipherText') ? data.cipherText : data.clearText);
    var _isEncrypted   = m.prop(_.has(data, 'cipherText'));
    var _canEdit        = m.prop(!_isEncrypted());

    this.value = function () {
      if (arguments.length) {
        if (this.isPlain()) {
          return _value(arguments[0]);
        } else {
          if (_canEdit()) {
            return _value(arguments[0]);
          } else {
            throw "You cannot edit a cipher text value!";
          }
        }
      }
      return _value();
    };

    this.edit = function () {
      _canEdit(true);
      _value('');
    };

    this.isEditing = function () {
      return _canEdit();
    };

    this.isDirty = function () {
      return this.value() !== _originalValue();
    };

    this.isPlain = function () {
      return !this.isSecure();
    };

    this.isSecure = function () {
      return _isEncrypted();
    };

    this.becomeSecure = function () {
      _isEncrypted(true);
      _canEdit(false);
    };

    this.becomeUnSecure = function () {
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

  return EncryptedValue;
});
