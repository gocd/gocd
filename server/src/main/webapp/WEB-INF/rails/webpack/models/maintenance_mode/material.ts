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

import {timeFormatter} from "helpers/time_formatter";
import _ from "lodash";
import Stream from "mithril/stream";
import {Errors, ErrorsJSON} from "models/mixins/errors";
import {ErrorMessages} from "models/mixins/error_messages";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {EncryptedValue, plainOrCipherValue} from "views/components/forms/encrypted_value";

export interface MaterialJSON {
  type: string;
  mdu_start_time: string;
  attributes: AnyMaterialAttributesJSON;
}

export interface FilterJSON {
  ignore?: string[] | null;
}

export interface MaterialAttributesJSON {
  name: string;
  auto_update: boolean;
  errors?: ErrorsJSON;
}

export interface ScmAttributesJSON extends MaterialAttributesJSON {
  url: string;
  destination: string;
  filter?: FilterJSON;
  invert_filter: boolean;
}

export interface DependencyMaterialAttributesJSON extends MaterialAttributesJSON {
  pipeline: string;
  stage: string;
}

export interface GitMaterialAttributesJSON extends ScmAttributesJSON {
  branch: string;
}

export interface UsernamePasswordJSON {
  username: string;
  password: string;
  encrypted_password: string;
}

export interface SvnMaterialAttributesJSON extends ScmAttributesJSON, UsernamePasswordJSON {
  check_externals: boolean;
}

type  HgMaterialAttributesJSON = ScmAttributesJSON;

export interface P4MaterialAttributesJSON extends ScmAttributesJSON, UsernamePasswordJSON {
  port: string;
  use_tickets: boolean;
  view: string;
}

export interface TfsMaterialAttributesJSON extends ScmAttributesJSON, UsernamePasswordJSON {
  domain: string;
  project_path: string;
}

export type AnyMaterialAttributesJSON =
  GitMaterialAttributesJSON
  | SvnMaterialAttributesJSON
  | HgMaterialAttributesJSON
  | P4MaterialAttributesJSON
  | TfsMaterialAttributesJSON
  | DependencyMaterialAttributesJSON;

export class Filter {
  ignore: Stream<string[]>;

  constructor(ignore: string[]) {
    this.ignore = Stream(ignore);
  }

  static fromJSON(filter?: FilterJSON) {
    const filterPattern = (filter && filter.ignore) ? filter.ignore : [];
    return new Filter(filterPattern);
  }

  toJSON() {
    if (_.isEmpty(this.ignore())) {
      return null;
    }
    return {
      ignore: this.ignore()
    };
  }
}

export abstract class MaterialAttributes extends ValidatableMixin {
  name: Stream<string>;
  autoUpdate: Stream<boolean>;

  protected constructor(name: string,
                        autoUpdate: boolean,
                        errors: { [key: string]: string[] } = {}) {
    super();
    this.name       = Stream(name);
    this.autoUpdate = Stream(autoUpdate);
    ValidatableMixin.call(this);
    this.errors(new Errors(errors));
  }

  static fromJSON(material: MaterialJSON): MaterialAttributes {
    switch (material.type) {
      case "git":
        return GitMaterialAttributes.deserialize(material.attributes as GitMaterialAttributesJSON);
      case "svn":
        return SvnMaterialAttributes.deserialize(material.attributes as SvnMaterialAttributesJSON);
      case "hg":
        return HgMaterialAttributes.deserialize(material.attributes as HgMaterialAttributesJSON);
      case "p4":
        return P4MaterialAttributes.deserialize(material.attributes as P4MaterialAttributesJSON);
      case "tfs":
        return TfsMaterialAttributes.deserialize(material.attributes as TfsMaterialAttributesJSON);
      case "dependency":
        return DependencyMaterialAttributes.deserialize(material.attributes as DependencyMaterialAttributesJSON);
      case "plugin":
        return new FakeMaterialAttributes();
      case "package":
        return new FakeMaterialAttributes();
      default:
        throw new Error(`Unknown material type ${material.type}`);
    }
  }
}

export class FakeMaterialAttributes extends MaterialAttributes {
  constructor() {
    super("", false, {});
  }
}

export abstract class ScmMaterialAttributes extends MaterialAttributes {
  url: Stream<string>;
  destination: Stream<string>;
  filter: Stream<Filter>;
  invertFilter: Stream<boolean>;

  protected constructor(url: string,
                        autoUpdate: boolean,
                        name: string,
                        destination: string,
                        filter: Filter,
                        invertFilter: boolean,
                        errors: { [key: string]: string[] } = {}) {
    super(name, autoUpdate, errors);
    this.url          = Stream(url);
    this.destination  = Stream(destination);
    this.filter       = Stream(filter);
    this.invertFilter = Stream(invertFilter);
    ValidatableMixin.call(this);
    this.validatePresenceOf("url");
  }
}

applyMixins(ScmMaterialAttributes, ValidatableMixin);

export class GitMaterialAttributes extends ScmMaterialAttributes {
  branch: Stream<string>;

  constructor(url: string,
              autoUpdate: boolean,
              name: string,
              branch: string,
              destination: string,
              filter: Filter,
              invertFilter: boolean,
              errors: { [key: string]: string[] } = {}) {
    super(url, autoUpdate, name, destination, filter, invertFilter, errors);
    this.branch = Stream(branch);
  }

  static deserialize(json: GitMaterialAttributesJSON) {
    return new GitMaterialAttributes(
      json.url,
      json.auto_update,
      json.name,
      json.branch,
      json.destination,
      Filter.fromJSON(json.filter),
      json.invert_filter,
      json.errors
    );
  }
}

applyMixins(GitMaterialAttributes, ValidatableMixin);

export class SvnMaterialAttributes extends ScmMaterialAttributes {
  checkExternals: Stream<boolean>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(url: string,
              autoUpdate: boolean,
              name: string,
              destination: string,
              checkExternals: boolean,
              username: string,
              password: string,
              encryptedPassword: string,
              filter: Filter,
              invertFilter: boolean,
              errors: { [key: string]: string[] } = {}) {
    super(url, autoUpdate, name, destination, filter, invertFilter, errors);
    this.checkExternals = Stream(checkExternals);
    this.username       = Stream(username);
    this.password       = Stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
  }

  static deserialize(json: SvnMaterialAttributesJSON) {
    return new SvnMaterialAttributes(
      json.url,
      json.auto_update,
      json.name,
      json.destination,
      json.check_externals,
      json.username,
      json.password,
      json.encrypted_password,
      Filter.fromJSON(json.filter),
      json.invert_filter,
      json.errors);
  }
}

applyMixins(SvnMaterialAttributes, ValidatableMixin);

export class HgMaterialAttributes extends ScmMaterialAttributes {
  constructor(url: string,
              autoUpdate: boolean,
              name: string,
              destination: string,
              filter: Filter,
              invertFilter: boolean,
              errors: { [key: string]: string[] } = {}) {
    super(url, autoUpdate, name, destination, filter, invertFilter, errors);
  }

  static deserialize(json: HgMaterialAttributesJSON) {
    return new HgMaterialAttributes(
      json.url,
      json.auto_update,
      json.name,
      json.destination,
      Filter.fromJSON(json.filter),
      json.invert_filter,
      json.errors
    );
  }
}

applyMixins(HgMaterialAttributes, ValidatableMixin);

export class DependencyMaterialAttributes extends MaterialAttributes {
  constructor(name: string,
              autoUpdate: boolean,
              errors: { [key: string]: string[] } = {}) {
    super(name, autoUpdate, errors);
  }

  static deserialize(json: DependencyMaterialAttributesJSON) {
    return new DependencyMaterialAttributes(
      json.name,
      json.auto_update,
      json.errors
    );
  }
}

applyMixins(DependencyMaterialAttributes, ValidatableMixin);

export class P4MaterialAttributes extends ScmMaterialAttributes {
  port: Stream<string>;
  useTickets: Stream<boolean>;
  view: Stream<string>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(url: string,
              autoUpdate: boolean,
              name: string,
              destination: string,
              port: string,
              useTickets: boolean,
              view: string,
              username: string,
              password: string,
              encryptedPassword: string,
              filter: Filter,
              invertFilter: boolean,
              errors: { [key: string]: string[] } = {}) {
    super(url, autoUpdate, name, destination, filter, invertFilter, errors);
    this.port       = Stream(port);
    this.useTickets = Stream(useTickets);
    this.view       = Stream(view);
    this.username   = Stream(username);
    this.password   = Stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.validatePresenceOf("view");
    this.validatePresenceOf("port", {message: ErrorMessages.mustBePresent("Host and Port")});
  }

  static deserialize(json: P4MaterialAttributesJSON) {
    return new P4MaterialAttributes(
      json.url,
      json.auto_update,
      json.name,
      json.destination,
      json.port,
      json.use_tickets,
      json.view,
      json.username,
      json.password,
      json.encrypted_password,
      Filter.fromJSON(json.filter),
      json.invert_filter,
      json.errors
    );
  }
}

applyMixins(P4MaterialAttributes, ValidatableMixin);

export class TfsMaterialAttributes extends ScmMaterialAttributes {
  domain: Stream<string>;
  projectPath: Stream<string>;
  username: Stream<string>;
  password: Stream<EncryptedValue>;

  constructor(url: string,
              autoUpdate: boolean,
              name: string,
              destination: string,
              domain: string,
              projectPath: string,
              username: string,
              password: string,
              encryptedPassword: string,
              filter: Filter,
              invertFilter: boolean,
              errors: { [key: string]: string[] } = {}) {
    super(url, autoUpdate, name, destination, filter, invertFilter, errors);
    this.domain      = Stream(domain);
    this.projectPath = Stream(projectPath);
    this.username    = Stream(username);
    this.password    = Stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
    this.validatePresenceOf("projectPath");
    this.validatePresenceOf("username");
    this.validatePresenceOfPassword("password");
  }

  static deserialize(json: TfsMaterialAttributesJSON) {
    return new TfsMaterialAttributes(
      json.url,
      json.auto_update,
      json.name,
      json.destination,
      json.domain,
      json.project_path,
      json.username,
      json.password,
      json.encrypted_password,
      Filter.fromJSON(json.filter),
      json.invert_filter,
      json.errors
    );
  }
}

applyMixins(TfsMaterialAttributes, ValidatableMixin);

export class Material {
  type: Stream<string>;
  mduStartTime: Stream<Date>;
  attributes: Stream<MaterialAttributes>;

  constructor(type: string, attributes: MaterialAttributes, mduStartTime: Date) {
    this.type         = Stream(type);
    this.attributes   = Stream(attributes);
    this.mduStartTime = Stream(mduStartTime);
  }

  static fromJSON(material: MaterialJSON): Material {
    return new Material(material.type,
                        MaterialAttributes.fromJSON(material),
                        timeFormatter.toDate(material.mdu_start_time));
  }

  attributesAsMap() {
    const map = new Map();
    _.forEach(this.attributes(), (value, key) => {
      if (!key.startsWith("__")) {
        if (value instanceof EncryptedValue) {
          map.set(key, value.valueForDisplay());
        } else if (_.isObject(value)) {
          map.set(key, JSON.stringify(value));
        } else {
          map.set(key, value ? value!.toString() : null);
        }
      }
    });
    return map;
  }
}

export class Materials {
  private materials: Material[];

  constructor(materials: Material[]) {
    this.materials = materials;
  }

  static fromJSON(materials: MaterialJSON[]): Materials {
    return new Materials(materials.map(Material.fromJSON));
  }

  allScmMaterials() {
    return this.materials.filter(Materials.scmMaterialPredicate);
  }

  count() {
    return this.materials ? this.materials.length : 0;
  }

  private static scmMaterialPredicate(material: Material) {
    return ["git", "hg", "svn", "p4", "tfs"].indexOf(material.type()) > -1;
  }

}
