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

import _ from "lodash";
import Stream from "mithril/stream";
import {SuggestionProvider} from "views/components/forms/autocomplete";

export class StagesAutocompletionProvider extends SuggestionProvider {
  private readonly suggestions: Stream<any>;
  private readonly pipeline: Stream<string | undefined>;

  constructor(pipeline: Stream<string | undefined>, suggestions: Stream<any>) {
    super();
    this.pipeline    = pipeline;
    this.suggestions = suggestions;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      const stages = _.get(this.suggestions(), this.pipeline()!);
      resolve(stages ? Object.keys(stages) : []);
    });
  }
}
