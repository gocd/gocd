/*
 * Copyright 2020 ThoughtWorks, Inc.
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
import {ConfigRepoJSON, ConfigReposJSON, MaterialModificationJSON, ParseInfoJSON,} from "models/config_repos/serialization";
import {Material, Materials} from "models/materials/types";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Rules} from "models/rules/rules";
import {Configuration} from "models/shared/configuration";
import {PlainTextValue} from "models/shared/config_value";
import {AutoSuggestions} from "../roles/auto_suggestion";

//tslint:disable-next-line
export interface ConfigRepo extends ValidatableMixin {
}

//tslint:disable-next-line
export interface LastParse extends ValidatableMixin {
}

export class ConfigRepo implements ValidatableMixin {
  static readonly PIPELINE_PATTERN             = "pipeline_pattern";
  static readonly ENVIRONMENT_PATTERN          = "environment_pattern";
  static readonly FILE_PATTERN                 = "file_pattern";
  static readonly JSON_PLUGIN_ID               = "json.config.plugin";
  static readonly YAML_PLUGIN_ID               = "yaml.config.plugin";
  static readonly GROOVY_PLUGIN_ID             = "cd.go.contrib.plugins.configrepo.groovy";
  id: Stream<string | undefined>;
  pluginId: Stream<string | undefined>;
  material: Stream<Material | undefined>;
  canAdminister: Stream<boolean>;
  configuration: Stream<Configuration[] | undefined>;
  lastParse: Stream<ParseInfo | null | undefined>;
  __jsonPluginPipelinesPattern: Stream<string> = Stream("");
  __jsonPluginEnvPattern: Stream<string>       = Stream("");
  __yamlPluginPattern: Stream<string>          = Stream("");
  materialUpdateInProgress: Stream<boolean>;
  rules: Stream<Rules>;

  constructor(id?: string,
              pluginId?: string,
              material?: Material,
              canAdminister?: boolean,
              configuration?: Configuration[],
              lastParse?: ParseInfo | null,
              materialUpdateInProgress?: boolean,
              rules?: Rules) {
    this.id                       = Stream(id);
    this.pluginId                 = Stream(pluginId);
    this.material                 = Stream(material);
    this.canAdminister            = Stream(canAdminister || false);
    this.configuration            = Stream(configuration);
    this.lastParse                = Stream(lastParse);
    this.materialUpdateInProgress = Stream(materialUpdateInProgress || false);
    this.rules                    = Stream(rules || []);
    if (configuration) {
      this.__jsonPluginPipelinesPattern = Stream(ConfigRepo.findConfigurationValue(configuration,
                                                                                   ConfigRepo.PIPELINE_PATTERN));
      this.__jsonPluginEnvPattern       = Stream(ConfigRepo.findConfigurationValue(configuration,
                                                                                   ConfigRepo.ENVIRONMENT_PATTERN));
      this.__yamlPluginPattern          = Stream(ConfigRepo.findConfigurationValue(configuration,
                                                                                   ConfigRepo.FILE_PATTERN));
    }
    ValidatableMixin.call(this);
    this.validatePresenceOf("id", {message: "Please provide a name for this repository"});
    this.validateIdFormat("id", {message: "Only letters, numbers, hyphens, underscores, and periods. Must not start with a period. Max 255 chars."});
    this.validatePresenceOf("pluginId");
    this.validateAssociated("material");
    this.validateEach("rules");
  }

  static findConfigurationValue(configuration: Configuration[], key: string) {
    const config = configuration.find((config) => config.key === key);
    return config && config.value ? config.value : "";
  }

  static fromJSON(json: ConfigRepoJSON) {
    const configurations = json.configuration.map((config) => Configuration.fromJSON(config));
    const parseInfo      = ParseInfo.fromJSON(json.parse_info);

    const configRepo = new ConfigRepo(json.id,
                                      json.plugin_id,
                                      Materials.fromJSON(json.material),
                                      json.can_administer,
                                      configurations,
                                      parseInfo,
                                      json.material_update_in_progress,
                                      Rules.fromJSON(json.rules));
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
    const materialUrl    = this.material()!.materialUrl();
    return [
      id,
      goodRevision,
      latestRevision,
      materialUrl
    ].some((value) => value ? value.toLowerCase().includes(textToMatch.toLowerCase()) : false);
  }
}

applyMixins(ConfigRepo, ValidatableMixin);

export class ConfigRepos {
  configRepos: ConfigRepo[];
  autoCompletion: AutoSuggestions;

  constructor(configRepos: ConfigRepo[], autoCompletion: AutoSuggestions) {
    this.configRepos    = configRepos;
    this.autoCompletion = autoCompletion;
  }

  static fromJSON(json?: ConfigReposJSON): ConfigRepos {
    const configRepos = _.map(json!._embedded.config_repos, (json: ConfigRepoJSON) => {
      return ConfigRepo.fromJSON(json);
    });
    return new ConfigRepos(configRepos, AutoSuggestions.fromJSON(json!.auto_completion));
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
  error: Stream<string | null | undefined>;
  readonly latestParsedModification: MaterialModification | null;
  readonly goodModification: MaterialModification | null;

  constructor(latestParsedModification: MaterialModification | null,
              goodModification: MaterialModification | null,
              error: string | undefined | null) {
    this.latestParsedModification = latestParsedModification;
    this.goodModification         = goodModification;
    this.error                    = Stream(error);
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
  autoUpdate:        "Auto-update",
  branch:            "Branch",
  checkExternals:    "Check Externals",
  domain:            "Domain",
  encryptedPassword: "Password",
  name:              "Material Name",
  destination:       "Alternate Checkout Path",
  projectPath:       "Project Path",
  url:               "URL",
  username:          "Username",
  password:          "Password",
  port:              "Host and Port",
  useTickets:        "Use Tickets",
  view:              "View",
  emailAddress:      "Email",
  revision:          "Revision",
  comment:           "Comment",
  modifiedTime:      "Modified Time"
};

const MATERIAL_TYPE_MAP: { [index: string]: string } = {
  git: "Git",
  hg:  "Mercurial",
  tfs: "Team Foundation Server",
  svn: "Subversion",
  p4:  "Perforce",
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
