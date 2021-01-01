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

import Stream from "mithril/stream";
import {Rule} from "models/rules/rules";
import {SuggestionProvider} from "views/components/forms/autocomplete";

export class ResourceSuggestionProvider extends SuggestionProvider {
  private rule: Stream<Rule>;
  private suggestion: Map<string, string[]>;

  constructor(rule: Stream<Rule>, suggestion: Map<string, string[]>) {
    super();
    this.rule       = rule;
    this.suggestion = suggestion;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    if (this.rule().type() === "*") {
      const allSuggestions: Set<string> = new Set();
      this.suggestion.forEach((value, key) => {
        value.forEach((val) => allSuggestions.add(val));
      });

      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve(Array.from(allSuggestions.values()));
      });
    }
    if (this.suggestion.has(this.rule().type())) {
      return new Promise<Awesomplete.Suggestion[]>((resolve) => {
        resolve(this.suggestion.get(this.rule().type())!);
      });
    }

    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve([]);
    });
  }

}
