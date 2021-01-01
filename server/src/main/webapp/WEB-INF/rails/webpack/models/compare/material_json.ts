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

import {FilterJSON} from "../maintenance_mode/material";
import {ErrorsJSON} from "../mixins/errors";

export interface MaterialJSON {
  type: string;
  attributes: MaterialAttributesJSON;
}

interface BaseAttributesJSON {
  name: string | null | undefined;
  auto_update: boolean;
  errors?: ErrorsJSON;
  display_type: string;
  description: string;
}

interface UsernamePasswordJSON {
  username?: string;
  password?: string;
  encrypted_password?: string;
}

interface ScmAttributesJSON extends BaseAttributesJSON, UsernamePasswordJSON {
  destination?: string;
}

export interface GitMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  branch: string;
}

export interface SvnMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  check_externals: boolean;
}

export interface HgMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  branch: string;
}

export interface P4MaterialAttributesJSON extends ScmAttributesJSON {
  port: string;
  use_tickets: boolean;
  view: string;
}

export interface TfsMaterialAttributesJSON extends ScmAttributesJSON {
  url: string;
  domain: string;
  project_path: string;
}

export interface DependencyMaterialAttributesJSON extends BaseAttributesJSON {
  pipeline: string;
  stage: string;
}

export interface PackageMaterialAttributesJSON extends BaseAttributesJSON {
  ref: string;
}

export interface PluggableScmMaterialAttributesJSON extends BaseAttributesJSON {
  ref: string;
  filter?: FilterJSON;
  destination: string;
}

export type MaterialAttributesJSON =
  GitMaterialAttributesJSON
  | SvnMaterialAttributesJSON
  | HgMaterialAttributesJSON
  | P4MaterialAttributesJSON
  | TfsMaterialAttributesJSON
  | DependencyMaterialAttributesJSON
  | PackageMaterialAttributesJSON
  | PluggableScmMaterialAttributesJSON;
