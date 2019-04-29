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

import {ErrorsJSON} from "models/mixins/errors";

export interface MaterialJSON {
  type: string;
  attributes: MaterialAttributesJSON;
}

export interface ScmAttributesJSON {
  name: string;
  auto_update: boolean;

  username?: string;
  password?: string;
  encrypted_password?: string;
}

export interface GitMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  branch: string;
  errors?: ErrorsJSON;
}

export interface SvnMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  check_externals: boolean;
  errors: ErrorsJSON;
}

export interface HgMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  errors?: ErrorsJSON;
}

export interface P4MaterialAttributesJSON extends ScmAttributesJSON {
  port: string;
  use_tickets: boolean;
  view: string;
  errors?: ErrorsJSON;
}

export interface TfsMaterialAttributesJSON extends ScmAttributesJSON {
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
