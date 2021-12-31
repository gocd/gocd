/*
 * Copyright 2022 ThoughtWorks, Inc.
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

import {ErrorsJSON} from "models/mixins/errors";
import {AutoSuggestionJSON} from "models/roles/auto_suggestion";
import {RuleJSON} from "models/rules/rules";
import {PropertyJSON} from "models/shared/configuration";

interface EmbeddedJSON {
  secret_configs: SecretConfigJSON[];
}

export interface SecretConfigsJSON {
  _embedded: EmbeddedJSON;
}

export interface SecretConfigJSON {
  id: string;
  description: string;
  plugin_id: string;
  properties: PropertyJSON[];
  rules: RuleJSON[];
  errors?: ErrorsJSON;
}

export interface SecretConfigsWithSuggestionsJSON extends SecretConfigsJSON {
  auto_completion: AutoSuggestionJSON[];
}
