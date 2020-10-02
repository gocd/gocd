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

/// <reference types="jasmine"/>

declare namespace jasmine {
  interface Matchers<T> {
    toBeChecked(): boolean;
    toBeDisabled(): boolean;
    toBeEmpty(): boolean;
    toBeHidden(): boolean;
    toBeInDOM(): boolean;
    toBeVisible(): boolean;
    toContainElement(selector: string): boolean;
    toContainHtml(html: string): boolean;
    toContainText(text: string): boolean;
    toExist(): boolean;
    toHaveAttr(attributeName: string, expectedAttributeValue? : any): boolean;
    toHaveHtml(html: string): boolean;
    toHaveLength(length: number): boolean;
    toHaveProp(propertyName: string, expectedPropertyValue? : any): boolean;
    toHaveText(text: string): boolean;
    toHaveValue(value : string): boolean;
  }
}
