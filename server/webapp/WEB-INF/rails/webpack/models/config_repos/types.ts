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

export interface ConfigRepos {
  _embedded: Embedded;
}

export interface Embedded {
  config_repos: ConfigRepo[];
}

export interface ConfigRepo {
  id: string;
  plugin_id: string;
  material: Material;
  configuration: any[];
  last_parse: LastParse;
}

export interface LastParse {
  revision?: string;
  success?: boolean;
  error?: string;
}

export interface Material {
  type: string;
  attributes: MaterialAttributes;
}

export interface ScmAttributes {
  url: string;
  name: string;
  auto_update: boolean;
}

export interface GitMaterialAttributes extends ScmAttributes {
  branch: string;
}

export interface UsernamePassword {
  username: string;
  password: string;
  encrypted_password: string;
}

export interface SvnMaterialAttributes extends ScmAttributes, UsernamePassword {
  check_externals: boolean;
}

// tslint:disable:no-empty-interface
export interface HgMaterialAttributes extends ScmAttributes {
  // nothing special about hg
}

export interface P4MaterialAttributes extends ScmAttributes, UsernamePassword {
  port: string;
  use_tickets: boolean;
  view: string;
}

export interface TfsMaterialAttributes extends ScmAttributes, UsernamePassword {
  domain?: string;
  project_path?: string;
}

const HUMAN_NAMES_FOR_MATERIAL_ATTRIBUTES: { [index: string]: string } = {
  auto_update: "Auto update",
  branch: "Branch",
  check_externals: "Check Externals",
  domain: "Domain",
  encrypted_password: "Password",
  name: "Material Name",
  project_path: "Project Path",
  url: "URL",
  username: "Username",
  port: "Host and port",
  use_tickets: "Use tickets",
  view: "View"
};

export function humanizedMaterialAttributeName(key: string) {
  return HUMAN_NAMES_FOR_MATERIAL_ATTRIBUTES[key] || key;
}

export type MaterialAttributes =
  GitMaterialAttributes
  | SvnMaterialAttributes
  | HgMaterialAttributes
  | P4MaterialAttributes
  | TfsMaterialAttributes;
