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

import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {
  ConfigRepoJSON,
  ConfigReposJSON, MaterialModificationJSON,
  ParseInfoJSON,
} from "models/config_repos/serialization";
import {Material, Materials} from "models/materials/types";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configuration, PlainTextValue} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";

//tslint:disable-next-line
export interface ConfigRepo extends ValidatableMixin {
}

//tslint:disable-next-line
export interface LastParse extends ValidatableMixin {
}

export class ConfigRepo implements ValidatableMixin {
  static readonly PIPELINE_PATTERN                             = "pipeline_pattern";
  static readonly ENVIRONMENT_PATTERN                          = "environment_pattern";
  static readonly FILE_PATTERN                                 = "file_pattern";
  static readonly JSON_PLUGIN_ID                               = "json.config.plugin";
  static readonly YAML_PLUGIN_ID                               = "yaml.config.plugin";
                  id: Stream<string>;
                  pluginId: Stream<string>;
                  material: Stream<Material>;
                  configuration: Stream<Configuration[]>;
                  lastParse: Stream<ParseInfo | null>;
                  __jsonPluginPipelinesPattern: Stream<string> = stream("");
                  __jsonPluginEnvPattern: Stream<string>       = stream("");
                  __yamlPluginPattern: Stream<string>          = stream("");
                  materialUpdateInProgress: Stream<boolean>;

  constructor(id?: string,
              pluginId?: string,
              material?: Material,
              configuration?: Configuration[],
              lastParse?: ParseInfo | null,
              materialUpdateInProgress?: boolean) {
    this.id                       = stream(id);
    this.pluginId                 = stream(pluginId);
    this.material                 = stream(material);
    this.configuration            = stream(configuration);
    this.lastParse                = stream(lastParse);
    this.materialUpdateInProgress = stream(materialUpdateInProgress || false);
    if (configuration) {
      this.__jsonPluginPipelinesPattern = stream(ConfigRepo.findConfigurationValue(configuration,
                                                                                   ConfigRepo.PIPELINE_PATTERN));
      this.__jsonPluginEnvPattern       = stream(ConfigRepo.findConfigurationValue(configuration,
                                                                                   ConfigRepo.ENVIRONMENT_PATTERN));
      this.__yamlPluginPattern          = stream(ConfigRepo.findConfigurationValue(configuration,
                                                                                   ConfigRepo.FILE_PATTERN));
    }
    ValidatableMixin.call(this);
    this.validatePresenceOf("id");
    this.validateIdFormat("id");
    this.validatePresenceOf("pluginId");
    this.validateAssociated("material");
  }

  static findConfigurationValue(configuration: Configuration[], key: string) {
    const config = configuration.find((config) => config.key === key);
    return config ? config.value : "";
  }

  static fromJSON(json: ConfigRepoJSON) {
    const configurations = json.configuration.map((config) => Configuration.fromJSON(config));
    const parseInfo      = ParseInfo.fromJSON(json.parse_info);

    const configRepo = new ConfigRepo(json.id,
                                      json.plugin_id,
                                      Materials.fromJSON(json.material),
                                      configurations,
                                      parseInfo,
                                      json.material_update_in_progress);
    configRepo.errors(new Errors(json.errors));
    return configRepo;
  }

  createConfigurationsFromText() {
    const configurations = [];
    if (this.pluginId() === ConfigRepo.YAML_PLUGIN_ID && this.__yamlPluginPattern().length > 0) {
      configurations.push(new Configuration(ConfigRepo.FILE_PATTERN, new PlainTextValue(this.__yamlPluginPattern())));
    }
    if (this.pluginId() === ConfigRepo.JSON_PLUGIN_ID) {
      if (this.__jsonPluginPipelinesPattern().length > 0) {
        configurations.push(new Configuration(ConfigRepo.PIPELINE_PATTERN,
                                              new PlainTextValue(this.__jsonPluginPipelinesPattern())));
      }
      if (this.__jsonPluginEnvPattern().length > 0) {
        configurations.push(new Configuration(ConfigRepo.ENVIRONMENT_PATTERN,
                                              new PlainTextValue(this.__jsonPluginEnvPattern())));
      }
    }
    return configurations;
  }

  matches(textToMatch: string): boolean {
    if (!textToMatch) {
      return true;
    }

    const id             = this.id();
    const goodRevision   = this.lastParse() && this.lastParse()!.goodRevision();
    const latestRevision = this.lastParse() && this.lastParse()!.latestRevision();
    const materialUrl    = this.material().materialUrl();
    return [
      id,
      goodRevision,
      latestRevision,
      materialUrl
    ].some((value) => value ? value.toLowerCase().includes(textToMatch.toLowerCase()) : false);
  }
}

applyMixins(ConfigRepo, ValidatableMixin);

export abstract class ConfigRepos {
  static fromJSON(json?: ConfigReposJSON): ConfigRepo[] {
    return _.map(json!._embedded.config_repos, (json: ConfigRepoJSON) => {
      return ConfigRepo.fromJSON(json);
    });
  }
}

export class MaterialModification {
  readonly username: string;
  readonly emailAddress: string | null;
  readonly revision: string;
  readonly comment: string;
  readonly modifiedTime: string;

  constructor(username: string, emailAddress: string | null, revision: string, comment: string, modifiedTime: string) {
    this.username     = username;
    this.emailAddress = emailAddress;
    this.revision     = revision;
    this.comment      = comment;
    this.modifiedTime = modifiedTime;
  }

  static fromJSON(modification: MaterialModificationJSON): MaterialModification {
    return new MaterialModification(modification.username,
                                    modification.email_address,
                                    modification.revision,
                                    modification.comment,
                                    modification.modified_time);
  }
}

export class ParseInfo {
  error: Stream<string | null>;
  readonly latestParsedModification: MaterialModification | null;
  readonly goodModification: MaterialModification | null;

  constructor(latestParsedModification: MaterialModification | null,
              goodModification: MaterialModification | null,
              error: string | undefined | null) {
    this.latestParsedModification = latestParsedModification;
    this.goodModification         = goodModification;
    this.error                    = stream(error);
  }

  static fromJSON(json: ParseInfoJSON) {
    if (!_.isEmpty(json)) {
      const latestParsedModification = json.latest_parsed_modification ? MaterialModification.fromJSON(json.latest_parsed_modification) : null;
      const goodModification         = json.good_modification ? MaterialModification.fromJSON(json.good_modification) : null;

      return new ParseInfo(latestParsedModification, goodModification, json.error);
    }
  }

  goodRevision() {
    if (this.goodModification) {
      return this.goodModification.revision;
    }
  }

  latestRevision() {
    if (this.latestParsedModification) {
      return this.latestParsedModification.revision;
    }
  }
}

const HUMAN_NAMES_FOR_MATERIAL_ATTRIBUTES: { [index: string]: string } = {
  autoUpdate: "Auto update",
  branch: "Branch",
  checkExternals: "Check Externals",
  domain: "Domain",
  encryptedPassword: "Password",
  name: "Material Name",
  projectPath: "Project Path",
  url: "URL",
  username: "Username",
  password: "Password",
  port: "Host and port",
  useTickets: "Use tickets",
  view: "View",
  emailAddress: "Email",
  revision: "Revision",
  comment: "Comment",
  modifiedTime: "Modified Time"
};

const MATERIAL_TYPE_MAP: { [index: string]: string } = {
  git: "Git",
  hg: "Mercurial",
  tfs: "Team Foundation Server",
  svn: "Subversion",
  p4: "Perforce",
};

const CONFIG_ATTRIBUTE: { [index: string]: string } = {
  file_pattern: "File Pattern"
};

export function humanizedMaterialNameForMaterialType(materialType: string) {
  return MATERIAL_TYPE_MAP[materialType];
}

export function humanizedMaterialAttributeName(key: string) {
  return HUMAN_NAMES_FOR_MATERIAL_ATTRIBUTES[key] || CONFIG_ATTRIBUTE[key] || key;
}
