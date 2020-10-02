/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import _ from "lodash";
import {Validatable} from "models/mixins/validatable_mixin";

const PADDED_SPACES = /^(?! ).*[\S]$/; // deny leading/trailing spaces
const ASCII_PR_CHAR = /^[\x20-\x7E]+$/; // ASCII printable characters only (codes 32 - 126)

const MSG_PAD_SPACE = "View name must not have leading or trailing whitespace";
const MSG_BAD_CHARS = "View name is only allowed to contain letters, numbers, spaces, and punctuation marks";
const MSG_DUPE_NAME = "Another view with this name already exists";

const MSG_NO_SELECT = "At least one pipeline must be selected";

/** Mixin to provide validations on personalization modal */
export function FilterValidations(opts) {
  Validatable.call(this, {});
  this.validatePresenceOf("name");
  this.validateFormatOf("name", {format: PADDED_SPACES, message: MSG_PAD_SPACE});
  this.validateFormatOf("name", {format: ASCII_PR_CHAR, message: MSG_BAD_CHARS});
  this.validateWith("name", uniquenessValidator(opts, MSG_DUPE_NAME));
  this.validateWith("hasPipelinesSelected", truthValidator(MSG_NO_SELECT));

  this.firstError = () => {
    return this.errors().errorsForDisplay("name") || this.errors().errorsForDisplay("hasPipelinesSelected");
  };

  this.invalid = () => {
    this.validate("hasPipelinesSelected");
    this.validate("name");
    return this.errors().hasErrors();
  };
}

function truthValidator(message) {
  return function validator() {
    this.validate = (model, attr) => {
      if (!model[attr]()) { model.errors().add(attr, message); }
    };
  };
}

function uniquenessValidator({names, name}, message) {
  return function validator() {
    this.validate = (model, attr) => {
      if ("string" === typeof name && name === model[attr]()) { return; }
      if (contains(names(), model[attr]())) {
        model.errors().add(attr, message);
      }
    };
  };
}

function contains(arr, el) { return !!el && _.includes(_.map(arr, (a) => a.toLowerCase()), el.toLowerCase()); }

