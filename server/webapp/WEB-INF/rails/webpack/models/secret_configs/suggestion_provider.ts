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
import {DefaultCache, PipelineGroupCache} from "models/pipeline_configs/pipeline_groups_cache";
import {SuggestionProvider} from "views/components/forms/autocomplete";
import {Option} from "views/components/forms/input_fields";

export class DynamicSuggestionProvider extends SuggestionProvider {
  private type: string;
  private pipelineGroupCache: PipelineGroupCache<Option> = new DefaultCache();

  constructor(type: string) {
    super();
    this.type = type;
  }

  setType(value: string) {
    this.type = value;
  }

  getData(): Promise<Awesomplete.Suggestion[]> {
    const self = this;
    if (this.type === "pipeline_group") {
      return new Promise<Awesomplete.Suggestion[]>((resolve, reject) => {
        if (!self.pipelineGroupCache.ready()) {
          self.pipelineGroupCache.prime(() => {
            resolve(self.pipelineGroupCache.pipelineGroups().map((group) => group.text));
          }, () => {
            reject(self.pipelineGroupCache.failureReason());
          });
        } else {
          resolve(self.pipelineGroupCache.pipelineGroups().map((group) => group.text));
        }
      });
    }

    return new Promise<Awesomplete.Suggestion[]>((resolve) => {
      resolve([]);
    });
  }

}
