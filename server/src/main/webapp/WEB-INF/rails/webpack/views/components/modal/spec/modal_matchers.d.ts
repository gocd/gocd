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
    toContainSpinner(): boolean;

    toContainError(msg: string): boolean;

    toContainTitle(title: string): boolean;

    toContainBody(body: string): boolean;

    toContainInBody(body: string): boolean;

    toContainSelectorInBody(selector: string): boolean;

    toContainElementWithDataTestId(selector: string): boolean;

    toContainButtons(buttons: string[]): boolean;
  }
}
