/*
 * Copyright 2019 ThoughtWorks, Inc.
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

import * as Awesomplete from "awesomplete";
import {SuggestionProvider} from "views/components/forms/autocomplete";

export class DynamicSuggestionProvider extends SuggestionProvider {
  private type: string;
  private autoCompleteHelper: Map<string, string[]>;

  constructor(type: string, autoCompleteHelper: Map<string, string[]>) {
    super();
    this.type               = type;
    this.autoCompleteHelper = autoCompleteHelper;
  }

  setType(value: string) {
    this.type = value;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    if (this.autoCompleteHelper.has(this.type)) {
      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve(this.autoCompleteHelper.get(this.type));
      });
    }

    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve([]);
    });
  }

}
