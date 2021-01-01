/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import {mixins as s} from "helpers/string-plus";
import _ from "lodash";
import {EventAware} from "models/mixins/event_aware";

export interface ErrorsJSON {
  [key: string]: string[];
}

export class Errors {
  private _errors: ErrorsJSON;

  constructor(errors: ErrorsJSON = {}) {
    this._errors = errors;
    EventAware.call(this);
  }

  clone() {
    return new Errors(_.clone(this._errors));
  }

  add(attrName: string, message: string) {
    if (!this._errors[attrName]) {
      this._errors[attrName] = [];
    }
    this._errors[attrName].push(message);
    this.notify("error:change");
  }

  addIfDoesNotExists(attrName: string, message: string) {
    if (!this.hasError(attrName, message)) {
      this.add(attrName, message);
    }
  }

  clear(attrName?: string) {
    "undefined" !== typeof attrName ? delete this._errors[attrName] : this._errors = {};
    this.notify("error:change");
  }

  clearError(attrName: string, message: string) {
    const errors = this._errors[attrName];
    _.remove(errors, (err) => err === message);
    this._errors[attrName] = errors;
  }

  errors(attrName?: string) {
    return attrName ? this._errors[attrName] : this._errors;
  }

  hasErrors(attrName?: string) {
    return attrName ? !_.isEmpty(this._errors[attrName]) : !this._isEmpty();
  }

  hasError(attrName: string, message: string) {
    const errors = this._errors[attrName] || [];
    return !!_.find(errors, (err) => err === message);
  }

  errorsForDisplay(attrName: string) {
    return _.map(this._errors[attrName] || [], s.terminateWithPeriod).join(" ");
  }

  allErrorsForDisplay(): string[] {
    return _.map(this.keys(), (key) => this.errorsForDisplay(key));
  }

  count() {
    return _.size(this._errors);
  }

  keys() {
    return Object.keys(this._errors);
  }

  toJSON() {
    return JSON.parse(JSON.stringify(this._errors)); // deep copy of internal state
  }

  private _isEmpty() {
    return _.every(this._errors, (errs, attr) => _.isEmpty(errs));
  }
}

// tslint:disable-next-line no-empty-interface
export interface Errors extends EventAware {}
