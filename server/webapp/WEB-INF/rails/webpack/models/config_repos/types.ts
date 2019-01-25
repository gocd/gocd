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
  ConfigReposJSON, GitMaterialAttributesJSON, HgMaterialAttributesJSON, MaterialAttributesJSON,
  MaterialJSON, MaterialModificationJSON,
  P4MaterialAttributesJSON, ParseInfoJSON, SvnMaterialAttributesJSON, TfsMaterialAttributesJSON,
} from "models/config_repos/serialization";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {ErrorMessages} from "models/mixins/validatable";
import {Configuration, PlainTextValue} from "models/shared/plugin_infos_new/plugin_settings/plugin_settings";
import {EncryptedValue} from "views/components/forms/encrypted_value";

const s = require("helpers/string-plus");

//tslint:disable-next-line
export interface ConfigRepo extends ValidatableMixin {
}

//tslint:disable-next-line
export interface LastParse extends ValidatableMixin {
}

//tslint:disable-next-line
export interface Material extends ValidatableMixin {
}

//tslint:disable-next-line
export interface MaterialAttributes extends ValidatableMixin {
}

//tslint:disable-next-line
export interface GitMaterialAttributes extends ValidatableMixin {
}

//tslint:disable-next-line
export interface SvnMaterialAttributes extends ValidatableMixin {
}

//tslint:disable-next-line
export interface HgMaterialAttributes extends ValidatableMixin {
}

//tslint:disable-next-line
export interface P4MaterialAttributes extends ValidatableMixin {
}

//tslint:disable-next-line
export interface TfsMaterialAttributes extends ValidatableMixin {
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

  constructor(id?: string,
              pluginId?: string,
              material?: Material,
              configuration?: Configuration[],
              lastParse?: ParseInfo | null) {
    this.id            = stream(id);
    this.pluginId      = stream(pluginId);
    this.material      = stream(material);
    this.configuration = stream(configuration);
    this.lastParse     = stream(lastParse);
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
                                      parseInfo);
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
}

class Materials {
  static fromJSON(material: MaterialJSON): Material {
    return new Material(material.type, MaterialAttributes.deserialize(material));
  }
}

export class Material implements ValidatableMixin {
  type: Stream<string>;
  attributes: Stream<MaterialAttributes>;

  constructor(type?: string, attributes?: MaterialAttributes) {
    this.attributes = stream(attributes);
    this.type       = stream(type);
    ValidatableMixin.call(this);
    this.validateAssociated("attributes");
  }

  typeProxy(value?: any): string {
    if (arguments.length > 0) {
      const newType = value;
      if (this.type() !== newType) {
        this.attributes(MaterialAttributes.deserialize({
                                                         type: newType,
                                                         attributes: ({} as MaterialAttributesJSON)
                                                       }));
      }
      this.type(newType);
    }
    return this.type();
  }
}

applyMixins(Material, ValidatableMixin);

export abstract class MaterialAttributes implements ValidatableMixin {

  autoUpdate: Stream<boolean>;

  protected constructor(name?: string, autoUpdate?: boolean) {
    this.autoUpdate = stream(autoUpdate);
    ValidatableMixin.call(this);
  }

  static deserialize(material: MaterialJSON) {
    switch (material.type) {
      case "git":
        return GitMaterialAttributes.fromJSON(material.attributes as GitMaterialAttributesJSON);
      case "svn":
        return SvnMaterialAttributes.fromJSON(material.attributes as SvnMaterialAttributesJSON);
      case "hg":
        return HgMaterialAttributes.fromJSON(material.attributes as HgMaterialAttributesJSON);
      case "p4":
        return P4MaterialAttributes.fromJSON(material.attributes as P4MaterialAttributesJSON);
      case "tfs":
        return TfsMaterialAttributes.fromJSON(material.attributes as TfsMaterialAttributesJSON);
      default:
        throw new Error(`Unknown material type ${material.type}`);
    }
  }

  toJSON() {
    const serialized                       = _.assign({}, this);
    const password: Stream<EncryptedValue> = _.get(serialized, "password");

    // remove the password field and setup the password serialization
    if (password) {
      // @ts-ignore
      delete serialized.password;

      if (password().isPlain() || password().isDirty()) {
        return _.assign({}, serialized, {password: password().value()});
      } else {
        return _.assign({}, serialized, {encrypted_password: password().value()});
      }
    }

    return serialized;
  }
}

applyMixins(MaterialAttributes, ValidatableMixin);

export class GitMaterialAttributes extends MaterialAttributes {
  url: Stream<string>;
  branch: Stream<string>;

  constructor(name?: string, autoUpdate?: boolean, url?: string, branch?: string) {
    super(name, autoUpdate);
    this.url    = stream(url);
    this.branch = stream(branch);
    this.validatePresenceOf("url");
  }

  static fromJSON(json: GitMaterialAttributesJSON) {
    const gitMaterialAttributes = new GitMaterialAttributes(json.name, json.auto_update, json.url, json.branch);
    gitMaterialAttributes.errors(new Errors(json.errors));
    return gitMaterialAttributes;
  }
}

applyMixins(GitMaterialAttributes, ValidatableMixin);

interface PasswordLike {
  cipherText?: string;
  plainText?: string;
}

function plainOrCipherValue(passwordLike: PasswordLike) {
  if (passwordLike.cipherText) {
    return new EncryptedValue({cipherText: s.defaultToIfBlank(passwordLike.cipherText, "")});
  } else {
    return new EncryptedValue({clearText: s.defaultToIfBlank(passwordLike.plainText, "")});
  }
}

export class SvnMaterialAttributes extends MaterialAttributes implements ValidatableMixin {
  url: Stream<string>;
  checkExternals: Stream<boolean>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              checkExternals?: boolean,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate);
    this.url            = stream(url);
    this.checkExternals = stream(checkExternals);
    this.username       = stream(username);
    this.password       = stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.validatePresenceOf("url");
  }

  static fromJSON(json: SvnMaterialAttributesJSON) {
    const svnMaterialAttributes = new SvnMaterialAttributes(json.name,
                                                            json.auto_update,
                                                            json.url,
                                                            json.check_externals,
                                                            json.username,
                                                            json.password,
                                                            json.encrypted_password);
    svnMaterialAttributes.errors(new Errors(json.errors));
    return svnMaterialAttributes;
  }
}

applyMixins(SvnMaterialAttributes, ValidatableMixin);

export class HgMaterialAttributes extends MaterialAttributes {
  url: Stream<string>;

  constructor(name?: string, autoUpdate?: boolean, url?: string) {
    super(name, autoUpdate);
    this.url = stream(url);
    this.validatePresenceOf("url");
  }

  static fromJSON(json: HgMaterialAttributesJSON) {
    const hgMaterialAttributes = new HgMaterialAttributes(json.name, json.auto_update, json.url);
    hgMaterialAttributes.errors(new Errors(json.errors));
    return hgMaterialAttributes;
  }
}

applyMixins(HgMaterialAttributes, ValidatableMixin);

export class P4MaterialAttributes extends MaterialAttributes {
  port: Stream<string>;
  useTickets: Stream<boolean>;
  view: Stream<string>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(name?: string,
              autoUpdate?: boolean,
              port?: string,
              useTickets?: boolean,
              view?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate);
    this.port       = stream(port);
    this.useTickets = stream(useTickets);
    this.view       = stream(view);
    this.username   = stream(username);
    this.password   = stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.validatePresenceOf("view");
    this.validatePresenceOf("port", {message: ErrorMessages.mustBePresent("Host and Port")});
  }

  static fromJSON(json: P4MaterialAttributesJSON) {
    const p4MaterialAttributes = new P4MaterialAttributes(json.name,
                                                          json.auto_update,
                                                          json.port,
                                                          json.use_tickets,
                                                          json.view,
                                                          json.username,
                                                          json.password,
                                                          json.encrypted_password);
    p4MaterialAttributes.errors(new Errors(json.errors));
    return p4MaterialAttributes;
  }
}

applyMixins(P4MaterialAttributes, ValidatableMixin);

export class TfsMaterialAttributes extends MaterialAttributes {
  url: Stream<string>;
  domain: Stream<string>;
  projectPath: Stream<string>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              domain?: string,
              projectPath?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate);
    this.url         = stream(url);
    this.domain      = stream(domain);
    this.projectPath = stream(projectPath);
    this.username    = stream(username);
    this.password    = stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.validatePresenceOf("url");
    this.validatePresenceOf("projectPath");
    this.validatePresenceOf("username");
    this.validatePresenceOfPassword("password");
  }

  static fromJSON(json: TfsMaterialAttributesJSON) {
    const tfsMaterialAttributes = new TfsMaterialAttributes(json.name,
                                                            json.auto_update,
                                                            json.url,
                                                            json.domain,
                                                            json.project_path,
                                                            json.username,
                                                            json.password,
                                                            json.encrypted_password);
    tfsMaterialAttributes.errors(new Errors(json.errors));
    return tfsMaterialAttributes;
  }
}

applyMixins(TfsMaterialAttributes, ValidatableMixin);

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
  view: "View"
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
