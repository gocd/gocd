/*
 * Copyright 2018 ThoughtWorks, Inc.
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

const s = require('string-plus');

namespace ErrorMessages {
  export function duplicate(attribute: string) {
    return `${s.humanize(attribute)} is a duplicate`;
  }

  export function mustBePresent(attribute: string) {
    return `${s.humanize(attribute).replace(/\bxpath\b/i, 'XPath').replace(/\burl\b/i, 'URL')} must be present`;
  }

  export function mustBeAUrl(attribute: string) {
    return `${s.humanize(attribute)} must be a valid http(s) url`;
  }

  export function mustBePositiveNumber(attribute: string) {
    return `${s.humanize(attribute)} must be a positive integer`;
  }

  export function mustContainString(attribute: string, requiredString: string) {
    return `${s.humanize(attribute)} must contain the string '${requiredString}'`;
  }
}

export class Validatable {
  errors: string[];

  constructor() {
    this.errors = [];
  }

  validatePresenceOf(attribute: string, message?: string) {
    if (s.isBlank(attribute)) {
      this.errors.push(attribute, message || ErrorMessages.mustBePresent(attribute));
    }
  }

}
