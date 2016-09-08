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

define(['mithril', 'lodash'], function (m, _) {
  var isPresentOnAll = function (setOfValues, value) {
    return _.every(setOfValues, function (values) {
      return _.includes(values, value);
    });
  };

  var isPresentOnAny = function (setOfValues, value) {
    return _.some(setOfValues, function (values) {
      return _.includes(values, value);
    });
  };

  var TriStateCheckbox = function (value, setOfValues) {
    var isChecked       = isPresentOnAll(setOfValues, value);
    var isIndeterminate = !isChecked && isPresentOnAny(setOfValues, value);

    this.name         = m.prop(value);
    var checked       = m.prop(isChecked);
    var indeterminate = m.prop(isIndeterminate);
    var self          = this;

    this.click = function () {
      if (isIndeterminate) {
        if (self.isChecked()) {
          self.becomeUnchecked();
        } else if (!self.isChecked() && !self.isIndeterminate()) {
          self.becomeIndeterminate();
        } else {
          self.becomeChecked();
        }
      } else {
        if (self.isChecked()) {
          self.becomeUnchecked();
        } else {
          self.becomeChecked();
        }
      }
    };

    this.isChecked = function () {
      return checked();
    };

    this.isIndeterminate = function () {
      return indeterminate();
    };

    this.becomeChecked = function () {
      checked(true);
      indeterminate(false);
    };

    this.becomeUnchecked = function () {
      checked(false);
      indeterminate(false);
    };

    this.becomeIndeterminate = function () {
      checked(false);
      indeterminate(true);
    };
  };

  return TriStateCheckbox;
});
