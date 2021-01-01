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

import s from "underscore.string";

export namespace ErrorMessages {
  export function idFormat(attr: string) {
    return `Invalid ${attr}. This must be alphanumeric and can contain hyphens, underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.`;
  }

  export function duplicate(attribute: string) {
    return `${humanize(attribute)} is a duplicate`;
  }

  export function mutuallyExclusive(attributeOne: string, attributeTwo: string) {
    return `${humanize(attributeOne)} and ${humanize(attributeTwo)} are mutuallly exclusive`;
  }

  export function mustBePresent(attribute: string) {
    return `${humanize(attribute).replace(/\bxpath\b/i, "XPath").replace(/\burl\b/i, "URL")} must be present`;
  }

  export function mustBePresentIf(attribute: string, condition: string) {
    return `${humanize(attribute).replace(/\bxpath\b/i, "XPath").replace(/\burl\b/i, "URL")} must be present if ${condition}`;
  }

  export function mustBeAUrl(attribute: string) {
    return `${humanize(attribute)} must be a valid http(s) url`;
  }

  export function mustBePositiveNumber(attribute: string) {
    return `${humanize(attribute)} must be a positive integer`;
  }

  export function mustContainString(attribute: string, requiredString: string) {
    return `${humanize(attribute)} must contain the string '${requiredString}'`;
  }

  export function mustNotExceedMaxLength(attribute: string, maxLength: number) {
    return `${humanize(attribute)} must not exceed length ${maxLength}`;
  }

  function humanize(str: string) {
    return s.capitalize(s.trim(s.underscored(str).replace(/_/g, " ")));
  }
}
