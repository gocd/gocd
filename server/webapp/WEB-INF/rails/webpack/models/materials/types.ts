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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import JsonUtils from "helpers/json_utils";
import SparkRoutes from "helpers/spark_routes";
import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {
  GitMaterialAttributesJSON,
  HgMaterialAttributesJSON,
  MaterialAttributesJSON,
  MaterialJSON,
  P4MaterialAttributesJSON,
  SvnMaterialAttributesJSON,
  TfsMaterialAttributesJSON,
} from "models/materials/serialization";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {ErrorMessages} from "models/mixins/validatable";
import {EncryptedValue} from "views/components/forms/encrypted_value";
const s = require("helpers/string-plus");

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

export class Materials {
  static fromJSON(material: MaterialJSON): Material {
    return new Material(material.type, MaterialAttributes.deserialize(material));
  }
}

export class Material implements ValidatableMixin {
  private static API_VERSION_HEADER = ApiVersion.v1;

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

  materialUrl(): string {
    // @ts-ignore
    return this.type() === "p4" ? this.attributes().port() : this.attributes().url();
  }

  toPayload() {
    return JsonUtils.toSnakeCasedObject(this);
  }

  checkConnection() {
    return ApiRequestBuilder.POST(
      SparkRoutes.materialConnectionCheck(),
      Material.API_VERSION_HEADER,
      {payload: this.toPayload()}
    );
  }
}

applyMixins(Material, ValidatableMixin);

export abstract class MaterialAttributes implements ValidatableMixin {
  username: Stream<string>;
  password: Stream<EncryptedValue>;
  autoUpdate: Stream<boolean>;

  protected constructor(name?: string,
                        autoUpdate?: boolean,
                        username?: string,
                        password?: string,
                        encryptedPassword?: string) {
    this.autoUpdate = stream(autoUpdate);
    this.username   = stream(username);
    this.password   = stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
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

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              branch?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url    = stream(url);
    this.branch = stream(branch);
    this.validatePresenceOf("url");
  }

  static fromJSON(json: GitMaterialAttributesJSON) {
    const gitMaterialAttributes = new GitMaterialAttributes(json.name,
                                                            json.auto_update,
                                                            json.url,
                                                            json.branch,
                                                            json.username,
                                                            json.password,
                                                            json.encrypted_password);
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

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              checkExternals?: boolean,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url            = stream(url);
    this.checkExternals = stream(checkExternals);
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

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url = stream(url);
    this.validatePresenceOf("url");
  }

  static fromJSON(json: HgMaterialAttributesJSON) {
    const hgMaterialAttributes = new HgMaterialAttributes(json.name,
                                                          json.auto_update,
                                                          json.url,
                                                          json.username,
                                                          json.password,
                                                          json.encrypted_password);
    hgMaterialAttributes.errors(new Errors(json.errors));
    return hgMaterialAttributes;
  }
}

applyMixins(HgMaterialAttributes, ValidatableMixin);

export class P4MaterialAttributes extends MaterialAttributes {
  port: Stream<string>;
  useTickets: Stream<boolean>;
  view: Stream<string>;

  constructor(name?: string,
              autoUpdate?: boolean,
              port?: string,
              useTickets?: boolean,
              view?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
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

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              domain?: string,
              projectPath?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
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
