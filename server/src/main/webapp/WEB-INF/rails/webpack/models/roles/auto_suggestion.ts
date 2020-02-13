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

export interface AutoSuggestionJSON {
  key: string;
  value: string[];
}

class AutoSuggestion {
  key: string;
  value: string[];

  constructor(key: string, value: string[]) {
    this.key   = key;
    this.value = value;
  }

  static fromJSON(data: AutoSuggestionJSON): AutoSuggestion {
    return new AutoSuggestion(data.key, data.value);
  }
}

export class AutoSuggestions extends Array<AutoSuggestion> {
  constructor(...vals: AutoSuggestion[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(AutoSuggestions.prototype));
  }

  static fromJSON(data: AutoSuggestionJSON[]): AutoSuggestions {
    return new AutoSuggestions(...data.map((a) => AutoSuggestion.fromJSON(a)));
  }
}
