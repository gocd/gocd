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
import {MaterialJSON} from "models/materials/serialization";
import {ErrorsJSON} from "models/mixins/errors";
import {AutoSuggestionJSON} from "models/roles/auto_suggestion";
import {RuleJSON} from "models/rules/rules";

export interface ConfigReposJSON {
  _embedded: EmbeddedJSON;
  auto_completion: AutoSuggestionJSON[];
}

export interface EmbeddedJSON {
  config_repos: ConfigRepoJSON[];
}

export interface ConfigRepoJSON {
  id: string;
  plugin_id: string;
  material: MaterialJSON;
  can_administer: boolean;
  configuration: any[];
  parse_info: ParseInfoJSON;
  errors?: ErrorsJSON;
  material_update_in_progress: boolean;
  rules: RuleJSON[];
}

export interface MaterialModificationJSON {
  username: string;
  email_address: string | null;
  revision: string;
  comment: string;
  modified_time: string;
}

export interface ParseInfoJSON {
  latest_parsed_modification?: MaterialModificationJSON;
  good_modification?: MaterialModificationJSON;
  error?: string;
}
