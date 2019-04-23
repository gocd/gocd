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

import * as _ from "lodash";

const s = require("string-plus");

export interface ErrorsJSON { [key: string]: string[]; }

export class Errors {
  private _errors: { [key: string]: string[] };

  constructor(errors: { [key: string]: string[] } = {}) {
    this._errors = errors;
  }

  add(attrName: string, message: string) {
    if (!this._errors[attrName]) {
      this._errors[attrName] = [];
    }
    this._errors[attrName].push(message);
  }

  clear(attrName?: string) {
    "undefined" !== typeof attrName ? delete this._errors[attrName] : this._errors = {};
  }

  errors(attrName?: string) {
    return attrName ? this._errors[attrName] : this._errors;
  }

  hasErrors(attrName?: string) {
    return attrName ? !_.isEmpty(this._errors[attrName]) : !this._isEmpty();
  }

  errorsForDisplay(attrName: string) {
    return _.map(this._errors[attrName] || [], s.terminateWithPeriod).join(" ");
  }

  count() {
    return _.size(this._errors);
  }

  keys() {
    return Object.keys(this._errors);
  }

  private _isEmpty() {
    return _.isEmpty(this._errors);
  }
}
