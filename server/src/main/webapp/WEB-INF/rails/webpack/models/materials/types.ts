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

import {ApiRequestBuilder, ApiVersion} from "helpers/api_request_builder";
import {JsonUtils} from "helpers/json_utils";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import Stream from "mithril/stream";
import {
  DependencyMaterialAttributesJSON,
  GitMaterialAttributesJSON,
  HgMaterialAttributesJSON,
  MaterialAttributesJSON,
  MaterialJSON,
  P4MaterialAttributesJSON,
  PackageMaterialAttributesJSON,
  PluggableScmMaterialAttributesJSON,
  SvnMaterialAttributesJSON,
  TfsMaterialAttributesJSON,
} from "models/materials/serialization";
import {Errors} from "models/mixins/errors";
import {ErrorsConsumer} from "models/mixins/errors_consumer";
import {ErrorMessages} from "models/mixins/error_messages";
import {ValidatableMixin, Validator} from "models/mixins/new_validatable_mixin";
import urlParse from "url-parse";
import {EncryptedValue, plainOrCipherValue} from "views/components/forms/encrypted_value";
import {Filter} from "../maintenance_mode/material";

const mapTypeToDisplayType: { [key: string]: string; } = {
  git:        "Git",
  svn:        "Subversion",
  hg:         "Mercurial",
  p4:         "Perforce",
  tfs:        "Tfs",
  dependency: "Pipeline",
  package:    "Package",
  plugin:     "Plugin"
};

export class Materials {
  static fromJSON(material: MaterialJSON): Material {
    return new Material(material.type, MaterialAttributes.deserialize(material));
  }

  static fromJSONArray(materials: MaterialJSON[]): Material[] {
    return materials.map(this.fromJSON);
  }
}

export class Material extends ValidatableMixin {
  private static API_VERSION_HEADER = ApiVersion.v1;

  type: Stream<string | undefined>;
  attributes: Stream<MaterialAttributes | undefined>;

  constructor(type?: string, attributes?: MaterialAttributes) {
    super();
    this.attributes = Stream(attributes);
    this.type       = Stream(type);
    this.validateAssociated("attributes");
  }

  name(): string {
    return this.attributes()!.name() || "";
  }

  typeProxy(value?: any): string {
    if (arguments.length > 0) {
      const newType = value;
      if (this.type() !== newType) {
        this.attributes(MaterialAttributes.deserialize({
                                                         type:       newType,
                                                         attributes: ({} as MaterialAttributesJSON)
                                                       }));
      }
      this.type(newType);
    }
    return this.type()!;
  }

  materialUrl(): string {
    switch (this.type()) {
      case "p4":
        return (this.attributes() as P4MaterialAttributes).port()!;
      case "dependency":
        const attrs = (this.attributes() as DependencyMaterialAttributes);
        return `${attrs.pipeline()} / ${attrs.stage()}`;
      case "package":
      case "plugin":
        return "";
      default:
        // @ts-ignore
        return this.attributes()!.url();
    }
  }

  errorContainerFor(subkey: string): ErrorsConsumer {
    return "type" === subkey ? this : this.attributes()!;
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

  pacConfigFiles(pluginId: string) {
    const payload = this.toApiPayload();
    return ApiRequestBuilder.POST(
      SparkRoutes.pacListConfigFiles(pluginId),
      Material.API_VERSION_HEADER,
      {payload}
    );
  }

  checkConnection(pipelineName?: string, pipelineGroup?: string) {
    const payload = this.toApiPayload();
    if (pipelineName) {
      payload.pipeline_name = pipelineName;
    }
    if (pipelineGroup) {
      payload.pipeline_group = pipelineGroup;
    }
    return ApiRequestBuilder.POST(
      SparkRoutes.materialConnectionCheck(),
      Material.API_VERSION_HEADER,
      {payload}
    );
  }

  typeForDisplay() {
    return mapTypeToDisplayType[this.type()!];
  }

  displayName() {
    const name = this.name();
    if (name.length > 0) {
      return name;
    }
    if (this.type() === "dependency") {
      return (this.attributes() as DependencyMaterialAttributes).pipeline();
    }
    if (this.type() === "package" || this.type() === "plugin") {
      return "";
    }
    if (this.type() === "p4") {
      return (this.attributes() as P4MaterialAttributes).port();
    }
    // @ts-ignore
    return this.attributes()!.url();
  }

  allErrors(): string[] {
    const errors = this.errors().allErrorsForDisplay();
    errors.push(...this.attributes()!.errors().allErrorsForDisplay());
    return errors;
  }

  clone() {
    if (this.attributes() === undefined) {
      return new Material(this.type());
    } else {
      const attrs = this.attributes()!.clone();
      return new Material(this.type(), attrs);
    }
  }

  resetPasswordIfAny() {
    const serialized                       = _.assign({}, this.attributes());
    const password: Stream<EncryptedValue> = _.get(serialized, "password");

    if (password && (password().isPlain() || password().isDirty())) {
      password().resetToOriginal();
    }
  }
}

export abstract class MaterialAttributes extends ValidatableMixin {
  name: Stream<string | undefined>;
  autoUpdate: Stream<boolean | undefined>;

  protected constructor(name?: string, autoUpdate?: boolean) {
    super();
    this.name       = Stream(name);
    this.autoUpdate = Stream(autoUpdate);
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
      case "package":
        return PackageMaterialAttributes.fromJSON(material.attributes as PackageMaterialAttributesJSON);
      case "plugin":
        return PluggableScmMaterialAttributes.fromJSON(material.attributes as PluggableScmMaterialAttributesJSON);
      default:
        throw new Error(`Unknown material type ${material.type}`);
    }
  }

  toJSON() {
    const serialized = _.assign({}, this);

    const destination = _.get(serialized, "destination");
    if (_.isEmpty(destination) || _.isEmpty(destination())) {
      // @ts-ignore
      delete serialized.destination; // collapse empty string as undefined to avoid blowing up
    }

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

  abstract clone(): MaterialAttributes;
}

export abstract class ScmMaterialAttributes extends MaterialAttributes {
  static readonly DESTINATION_REGEX = new RegExp(
    "^(?!\\/)((([\\.]\\/)?[\\.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ]))$");
  destination: Stream<string>       = Stream();
  username: Stream<string | undefined>;
  password: Stream<EncryptedValue>;
  filter: Stream<Filter | undefined>;
  invertFilter: Stream<boolean | undefined>;

  constructor(name?: string, autoUpdate?: boolean, username?: string, password?: string, encryptedPassword?: string, filter?: Filter, invertFilter?: boolean) {
    super(name, autoUpdate);
    this.validateFormatOf("destination",
                          ScmMaterialAttributes.DESTINATION_REGEX,
                          {message: "Must be a relative path within the pipeline's working directory"});

    this.username     = Stream(username);
    this.password     = Stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.filter       = Stream(filter);
    this.invertFilter = Stream(invertFilter);
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
              .add(attr, "URL credentials must be set in either the URL or the username+password fields, but not both.");
      }
    }
  }

  private get(entity: any, attr: string): any {
    const val = entity[attr];
    return ("function" === typeof val) ? val() : val;
  }
}

export class GitMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  branch: Stream<string | undefined>;

  constructor(name?: string, autoUpdate?: boolean, url?: string, branch?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url    = Stream(url);
    this.branch = Stream(branch);

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
    if (json.filter !== undefined) {
      attrs.filter(Filter.fromJSON(json.filter));
    }
    attrs.invertFilter(json.invert_filter);
    return attrs;
  }

  clone(): MaterialAttributes {
    const gitAttrs = new GitMaterialAttributes(this.name(), this.autoUpdate(), this.url(), this.branch(), this.username());
    gitAttrs.password(this.password());
    if (this.filter() !== undefined) {
      gitAttrs.filter(new Filter(this.filter()!.ignore()));
    }
    gitAttrs.invertFilter(this.invertFilter());
    return gitAttrs;
  }
}

export class SvnMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  checkExternals: Stream<boolean | undefined>;

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              checkExternals?: boolean,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url            = Stream(url);
    this.checkExternals = Stream(checkExternals);

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
    if (json.filter !== undefined) {
      attrs.filter(Filter.fromJSON(json.filter));
    }
    attrs.invertFilter(json.invert_filter);
    return attrs;
  }

  clone(): MaterialAttributes {
    const svnAttrs = new SvnMaterialAttributes(this.name(), this.autoUpdate(), this.url(), this.checkExternals(), this.username());
    svnAttrs.password(this.password());
    if (this.filter() !== undefined) {
      svnAttrs.filter(new Filter(this.filter()!.ignore()));
    }
    svnAttrs.invertFilter(this.invertFilter());
    return svnAttrs;
  }
}

export class HgMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  branch: Stream<string | undefined>;

  constructor(name?: string, autoUpdate?: boolean, url?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string,
              branch?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url    = Stream(url);
    this.branch = Stream(branch);

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
    if (json.filter !== undefined) {
      attrs.filter(Filter.fromJSON(json.filter));
    }
    attrs.invertFilter(json.invert_filter);
    return attrs;
  }

  clone(): MaterialAttributes {
    const hgAttrs = new HgMaterialAttributes(this.name(), this.autoUpdate(), this.url(), this.username());
    hgAttrs.password(this.password());
    hgAttrs.branch(this.branch());
    if (this.filter() !== undefined) {
      hgAttrs.filter(new Filter(this.filter()!.ignore()));
    }
    hgAttrs.invertFilter(this.invertFilter());
    return hgAttrs;
  }
}

export class P4MaterialAttributes extends ScmMaterialAttributes {
  port: Stream<string | undefined>;
  useTickets: Stream<boolean | undefined>;
  view: Stream<string | undefined>;

  constructor(name?: string,
              autoUpdate?: boolean,
              port?: string,
              useTickets?: boolean,
              view?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.port       = Stream(port);
    this.useTickets = Stream(useTickets);
    this.view       = Stream(view);

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
    if (json.filter !== undefined) {
      attrs.filter(Filter.fromJSON(json.filter));
    }
    attrs.invertFilter(json.invert_filter);
    return attrs;
  }

  clone(): MaterialAttributes {
    const p4Attrs = new P4MaterialAttributes(this.name(), this.autoUpdate(), this.port(), this.useTickets(), this.view(), this.username());
    p4Attrs.password(this.password());
    if (this.filter() !== undefined) {
      p4Attrs.filter(new Filter(this.filter()!.ignore()));
    }
    p4Attrs.invertFilter(this.invertFilter());
    return p4Attrs;
  }
}

export class TfsMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  domain: Stream<string | undefined>;
  projectPath: Stream<string | undefined>;

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              domain?: string,
              projectPath?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, username, password, encryptedPassword);
    this.url         = Stream(url);
    this.domain      = Stream(domain);
    this.projectPath = Stream(projectPath);

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
    if (json.filter !== undefined) {
      attrs.filter(Filter.fromJSON(json.filter));
    }
    attrs.invertFilter(json.invert_filter);
    return attrs;
  }

  clone(): MaterialAttributes {
    const tfsAttrs = new TfsMaterialAttributes(this.name(), this.autoUpdate(), this.url(), this.domain(), this.projectPath(), this.username());
    tfsAttrs.password(this.password());
    if (this.filter() !== undefined) {
      tfsAttrs.filter(new Filter(this.filter()!.ignore()));
    }
    tfsAttrs.invertFilter(this.invertFilter());
    return tfsAttrs;
  }
}

export class DependencyMaterialAttributes extends MaterialAttributes {
  pipeline: Stream<string | undefined>;
  stage: Stream<string | undefined>;
  ignoreForScheduling: Stream<boolean | undefined>;

  constructor(name?: string, autoUpdate?: boolean, pipeline?: string, stage?: string, ignoreForScheduling?: boolean) {
    super(name, autoUpdate);
    this.pipeline            = Stream(pipeline);
    this.stage               = Stream(stage);
    this.ignoreForScheduling = Stream(ignoreForScheduling);

    this.validatePresenceOf("pipeline");
    this.validatePresenceOf("stage");
  }

  static fromJSON(json: DependencyMaterialAttributesJSON) {
    const attrs = new DependencyMaterialAttributes(
      json.name,
      json.auto_update,
      json.pipeline,
      json.stage,
      json.ignore_for_scheduling
    );
    attrs.errors(new Errors(json.errors));
    return attrs;
  }

  clone(): MaterialAttributes {
    return new DependencyMaterialAttributes(this.name(), this.autoUpdate(), this.pipeline(), this.stage(), this.ignoreForScheduling());
  }
}

export class PackageMaterialAttributes extends MaterialAttributes {
  ref: Stream<string | undefined>;

  constructor(name?: string, autoUpdate?: boolean, ref?: string) {
    super(name, autoUpdate);
    this.ref = Stream(ref);
  }

  static fromJSON(data: PackageMaterialAttributesJSON): PackageMaterialAttributes {
    const attrs = new PackageMaterialAttributes(data.name, data.auto_update, data.ref);
    attrs.errors(new Errors(data.errors));
    return attrs;
  }

  clone(): MaterialAttributes {
    return new PackageMaterialAttributes(this.name(), this.autoUpdate(), this.ref());
  }
}

export class PluggableScmMaterialAttributes extends MaterialAttributes {
  ref: Stream<string>;
  filter: Stream<Filter>;
  destination: Stream<string>;

  constructor(name: string | undefined, autoUpdate: boolean | undefined, ref: string, destination: string, filter: Filter) {
    super(name, autoUpdate);
    this.ref         = Stream(ref);
    this.filter      = Stream(filter);
    this.destination = Stream(destination);
  }

  static fromJSON(data: PluggableScmMaterialAttributesJSON): PluggableScmMaterialAttributes {
    const attrs = new PluggableScmMaterialAttributes(data.name, data.auto_update, data.ref, data.destination, Filter.fromJSON(data.filter));
    attrs.errors(new Errors(data.errors));
    return attrs;
  }

  clone(): MaterialAttributes {
    return new PluggableScmMaterialAttributes(this.name(), this.autoUpdate(), this.ref(), this.destination(), this.filter());
  }
}
