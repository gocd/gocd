/*
 * Copyright 2018 ThoughtWorks, Inc.
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

export interface ConfigReposJSON {
  _embedded: EmbeddedJSON;
}

export interface EmbeddedJSON {
  config_repos: ConfigRepoJSON[];
}

export interface ConfigRepoJSON {
  id: string;
  plugin_id: string;
  material: MaterialJSON;
  configuration: any[];
  parse_info: ParseInfoJSON;
  errors?: ErrorsJSON;
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

export interface MaterialJSON {
  type: string;
  attributes: MaterialAttributesJSON;
}

export interface ScmAttributesJSON {
  name: string;
  auto_update: boolean;
}

export interface GitMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  branch: string;
  errors?: ErrorsJSON;
}

export interface UsernamePasswordJSON {
  username?: string;
  password?: string;
  encrypted_password?: string;
  errors?: ErrorsJSON;
}

export interface SvnMaterialAttributesJSON extends ScmAttributesJSON, UsernamePasswordJSON {
  url: string;
  check_externals: boolean;
  errors: ErrorsJSON;
}

export interface HgMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  errors?: ErrorsJSON;
}

export interface P4MaterialAttributesJSON extends ScmAttributesJSON, UsernamePasswordJSON {
  port: string;
  use_tickets: boolean;
  view: string;
  errors?: ErrorsJSON;
}

export interface TfsMaterialAttributesJSON extends ScmAttributesJSON, UsernamePasswordJSON {
  url: string;
  domain: string;
  project_path: string;
  errors?: ErrorsJSON;
}

export type MaterialAttributesJSON =
  GitMaterialAttributesJSON
  | SvnMaterialAttributesJSON
  | HgMaterialAttributesJSON
  | P4MaterialAttributesJSON
  | TfsMaterialAttributesJSON;
