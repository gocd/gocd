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
  DependencyMaterialAttributesJSON,
  GitMaterialAttributesJSON,
  HgMaterialAttributesJSON,
  MaterialAttributesJSON,
  MaterialJSON,
  P4MaterialAttributesJSON,
  SvnMaterialAttributesJSON,
  TfsMaterialAttributesJSON,
} from "models/materials/serialization";
import {ErrorMessages} from "models/mixins/error_messages";
import {Errors} from "models/mixins/errors";
import {ErrorsConsumer} from "models/mixins/errors_consumer";
import {ValidatableMixin, Validator} from "models/mixins/new_validatable_mixin";
import {EncryptedValue} from "views/components/forms/encrypted_value";

const s        = require("helpers/string-plus");
const urlParse = require("url-parse");

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

//tslint:disable-next-line
export interface DependencyMaterialAttributes extends ValidatableMixin {
}

export class Materials {
  static fromJSON(material: MaterialJSON): Material {
    return new Material(material.type, MaterialAttributes.deserialize(material));
  }
}

export class Material extends ValidatableMixin {
  private static API_VERSION_HEADER = ApiVersion.v1;

  type: Stream<string>;
  attributes: Stream<MaterialAttributes>;

  constructor(type?: string, attributes?: MaterialAttributes) {
    super();
    this.attributes = stream(attributes);
    this.type       = stream(type);
    this.validateAssociated("attributes");
  }

  name(): string {
    return this.attributes().name() || "";
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

  errorContainerFor(subkey: string): ErrorsConsumer {
    return "type" === subkey ? this : this.attributes();
  }

  toApiPayload() {
    const raw = JsonUtils.toSnakeCasedObject(this);

    if (!raw.attributes.name) {
      delete raw.attributes.name; // collapse empty string as undefined to avoid blowing up
    }

    if (!raw.attributes.destination) {
      delete raw.attributes.destination; // collapse empty string as undefined to avoid blowing up
    }

    return raw;
  }

  checkConnection(pipelineGroup?: string) {
    const payload = this.toApiPayload();
    if (pipelineGroup) {
      payload.pipeline_group = pipelineGroup;
    }
    return ApiRequestBuilder.POST(
      SparkRoutes.materialConnectionCheck(),
      Material.API_VERSION_HEADER,
      {payload}
    );
  }
}

export abstract class MaterialAttributes extends ValidatableMixin {
  name: Stream<string>;
  autoUpdate: Stream<boolean>;

  protected constructor(name?: string, autoUpdate?: boolean) {
    super();
    this.name       = stream(name);
    this.autoUpdate = stream(autoUpdate);
    this.validateIdFormat("name");
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
      case "dependency":
        return DependencyMaterialAttributes.fromJSON(material.attributes as DependencyMaterialAttributesJSON);
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

export abstract class ScmMaterialAttributes extends MaterialAttributes {
  static readonly DESTINATION_REGEX = new RegExp(
    "^(?!\\/)((([\\.]\\/)?[\\.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ]))$");
  destination: Stream<string>       = stream();
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(name?: string, autoUpdate?: boolean, username?: string, password?: string, encryptedPassword?: string) {
    super(name, autoUpdate);
    this.validateFormatOf("destination",
                          ScmMaterialAttributes.DESTINATION_REGEX,
                          {message: "Must be a relative path within the pipeline's working directory"});

    this.username = stream(username);
    this.password = stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
  }
}

class AuthNotSetInUrlAndUserPassFieldsValidator extends Validator {
  protected doValidate(entity: any, attr: string): void {
    const url = this.get(entity, attr) as string;
    if (!!url) {
      const urlObj   = urlParse(url); // use url-parse instead of native URL() because MSEdge will not allow embedded credentials
      const username = this.get(entity, "username") as string | undefined;
      const password = this.get(entity, "password") as EncryptedValue | undefined;

      if ((!!username || !!(password && password.value())) && (!!urlObj.username || !!urlObj.password || url.indexOf("@") !== -1)) {
        entity.errors()
              .add(attr,
                   "URL credentials must be set in either the URL or the username+password fields, but not both.");
      }
    }
  }

  private get(entity: any, attr: string): any {
    const val = entity[attr];
    return ("function" === typeof val) ? val() : val;
  }
}

export class GitMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string>;
  branch: Stream<string>;

  constructor(name?: string, autoUpdate?: boolean, url?: string, branch?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url    = stream(url);
    this.branch = stream(branch);

    this.validatePresenceOf("url");
    this.validateWith(new AuthNotSetInUrlAndUserPassFieldsValidator(), "url");
  }

  static fromJSON(json: GitMaterialAttributesJSON) {
    const attrs = new GitMaterialAttributes(
      json.name,
      json.auto_update,
      json.url,
      json.branch,
      json.username,
      json.password,
      json.encrypted_password,
    );
    if (undefined !== json.destination) {
      attrs.destination(json.destination);
    }
    attrs.errors(new Errors(json.errors));
    return attrs;
  }
}

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

export class SvnMaterialAttributes extends ScmMaterialAttributes {
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
    const attrs = new SvnMaterialAttributes(
      json.name,
      json.auto_update,
      json.url,
      json.check_externals,
      json.username,
      json.password,
      json.encrypted_password,
    );
    if (undefined !== json.destination) {
      attrs.destination(json.destination);
    }
    attrs.errors(new Errors(json.errors));
    return attrs;
  }
}

export class HgMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string>;
  branch: Stream<string>;

  constructor(name?: string, autoUpdate?: boolean, url?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string,
              branch?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url    = stream(url);
    this.branch = stream(branch);

    this.validatePresenceOf("url");
    this.validateWith(new AuthNotSetInUrlAndUserPassFieldsValidator(), "url");
  }

  static fromJSON(json: HgMaterialAttributesJSON) {
    const attrs = new HgMaterialAttributes(
      json.name,
      json.auto_update,
      json.url,
      json.username,
      json.password,
      json.encrypted_password,
      json.branch
    );
    if (undefined !== json.destination) {
      attrs.destination(json.destination);
    }
    attrs.errors(new Errors(json.errors));
    return attrs;
  }
}

export class P4MaterialAttributes extends ScmMaterialAttributes {
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

    this.validatePresenceOf("view");
    this.validatePresenceOf("port", {message: ErrorMessages.mustBePresent("Host and Port")});
  }

  static fromJSON(json: P4MaterialAttributesJSON) {
    const attrs = new P4MaterialAttributes(
      json.name,
      json.auto_update,
      json.port,
      json.use_tickets,
      json.view,
      json.username,
      json.password,
      json.encrypted_password,
    );

    if (undefined !== json.destination) {
      attrs.destination(json.destination);
    }
    attrs.errors(new Errors(json.errors));
    return attrs;
  }
}

export class TfsMaterialAttributes extends ScmMaterialAttributes {
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

    this.validatePresenceOf("url");
    this.validatePresenceOf("projectPath");
    this.validatePresenceOf("username");
    this.validatePresenceOfPassword("password");
  }

  static fromJSON(json: TfsMaterialAttributesJSON) {
    const attrs = new TfsMaterialAttributes(
      json.name,
      json.auto_update,
      json.url,
      json.domain,
      json.project_path,
      json.username,
      json.password,
      json.encrypted_password,
    );
    if (undefined !== json.destination) {
      attrs.destination(json.destination);
    }
    attrs.errors(new Errors(json.errors));
    return attrs;
  }
}

export class DependencyMaterialAttributes extends MaterialAttributes {
  pipeline: Stream<string>;
  stage: Stream<string>;

  constructor(name?: string, autoUpdate?: boolean, pipeline?: string, stage?: string) {
    super(name, autoUpdate);
    this.pipeline = stream(pipeline);
    this.stage    = stream(stage);

    this.validatePresenceOf("pipeline");
    this.validatePresenceOf("stage");
  }

  static fromJSON(json: DependencyMaterialAttributesJSON) {
    const attrs = new DependencyMaterialAttributes(
      json.name,
      json.auto_update,
      json.pipeline,
      json.stage,
    );
    attrs.errors(new Errors(json.errors));
    return attrs;
  }
}
