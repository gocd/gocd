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

import {ApiRequestBuilder, ApiResult, ApiVersion} from "helpers/api_request_builder";
import {SparkRoutes} from "helpers/spark_routes";
import _ from "lodash";
import Stream from "mithril/stream";
import {MaterialModificationJSON} from "models/config_repos/serialization";
import {humanizedMaterialAttributeName, MaterialModification} from "models/config_repos/types";
import {Filter} from "models/maintenance_mode/material";
import {DependencyMaterialAttributes, mapTypeToDisplayType} from "./types";

interface BaseAttributesJSON {
  name: string;
  auto_update: boolean;
}

interface GitMaterialAttributesJSON extends BaseAttributesJSON {
  url: string;
  branch: string;
}

interface SvnMaterialAttributesJSON extends BaseAttributesJSON {
  url: string;
  check_externals: boolean;
}

interface HgMaterialAttributesJSON extends BaseAttributesJSON {
  url: string;
  branch: string;
}

interface P4MaterialAttributesJSON extends BaseAttributesJSON {
  port: string;
  use_tickets: boolean;
  view: string;
}

interface TfsMaterialAttributesJSON extends BaseAttributesJSON {
  url: string;
  domain: string;
  project_path: string;
}

interface PackageMaterialAttributesJSON extends BaseAttributesJSON {
  ref: string;
  package_name: string;
  package_repo_name: string;
}

interface PluggableScmMaterialAttributesJSON extends BaseAttributesJSON {
  ref: string;
  scm_name: string;
}

type MaterialAttributesJSON =
  GitMaterialAttributesJSON
  | SvnMaterialAttributesJSON
  | HgMaterialAttributesJSON
  | P4MaterialAttributesJSON
  | TfsMaterialAttributesJSON
  | PackageMaterialAttributesJSON
  | PluggableScmMaterialAttributesJSON;

export interface MaterialWithFingerprintJSON {
  type: string;
  fingerprint: string;
  attributes: MaterialAttributesJSON;
}

interface MaterialWithModificationJSON {
  config: MaterialWithFingerprintJSON;
  modification: MaterialModificationJSON;
}

interface MaterialsJSON {
  materials: MaterialWithModificationJSON[];
}

abstract class MaterialAttributes {
  name: Stream<string | undefined>;
  autoUpdate: Stream<boolean>;

  protected constructor(name?: string, autoUpdate: boolean = false) {
    this.name       = Stream(name);
    this.autoUpdate = Stream(autoUpdate);
  }

  static deserialize(material: MaterialWithFingerprintJSON) {
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
      case "package":
        return PackageMaterialAttributes.fromJSON(material.attributes as PackageMaterialAttributesJSON);
      case "plugin":
        return PluggableScmMaterialAttributes.fromJSON(material.attributes as PluggableScmMaterialAttributesJSON);
      default:
        throw new Error(`Unknown material type ${material.type}`);
    }
  }
}

export class GitMaterialAttributes extends MaterialAttributes {
  url: Stream<string | undefined>;
  branch: Stream<string | undefined>;

  constructor(name?: string, autoUpdate?: boolean, url?: string, branch?: string) {
    super(name, autoUpdate);
    this.url    = Stream(url);
    this.branch = Stream(branch);
  }

  static fromJSON(json: GitMaterialAttributesJSON) {
    return new GitMaterialAttributes(json.name, json.auto_update, json.url, json.branch);
  }
}

export class SvnMaterialAttributes extends MaterialAttributes {
  url: Stream<string | undefined>;
  checkExternals: Stream<boolean | undefined>;

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              checkExternals?: boolean) {
    super(name, autoUpdate);
    this.url            = Stream(url);
    this.checkExternals = Stream(checkExternals);
  }

  static fromJSON(json: SvnMaterialAttributesJSON) {
    return new SvnMaterialAttributes(json.name, json.auto_update, json.url, json.check_externals);
  }
}

export class HgMaterialAttributes extends MaterialAttributes {
  url: Stream<string | undefined>;
  branch: Stream<string | undefined>;

  constructor(name?: string, autoUpdate?: boolean, url?: string, branch?: string) {
    super(name, autoUpdate);
    this.url    = Stream(url);
    this.branch = Stream(branch);
  }

  static fromJSON(json: HgMaterialAttributesJSON) {
    return new HgMaterialAttributes(json.name, json.auto_update, json.url, json.branch);
  }
}

export class P4MaterialAttributes extends MaterialAttributes {
  port: Stream<string | undefined>;
  useTickets: Stream<boolean | undefined>;
  view: Stream<string | undefined>;

  constructor(name?: string,
              autoUpdate?: boolean,
              port?: string,
              useTickets?: boolean,
              view?: string) {
    super(name, autoUpdate);
    this.port       = Stream(port);
    this.useTickets = Stream(useTickets);
    this.view       = Stream(view);
  }

  static fromJSON(json: P4MaterialAttributesJSON) {
    return new P4MaterialAttributes(json.name, json.auto_update, json.port, json.use_tickets, json.view);
  }
}

export class TfsMaterialAttributes extends MaterialAttributes {
  url: Stream<string | undefined>;
  domain: Stream<string | undefined>;
  projectPath: Stream<string | undefined>;

  constructor(name?: string,
              autoUpdate?: boolean,
              url?: string,
              domain?: string,
              projectPath?: string) {
    super(name, autoUpdate);
    this.url         = Stream(url);
    this.domain      = Stream(domain);
    this.projectPath = Stream(projectPath);
  }

  static fromJSON(json: TfsMaterialAttributesJSON) {
    return new TfsMaterialAttributes(json.name, json.auto_update, json.url, json.domain, json.project_path);
  }
}

export class PackageMaterialAttributes extends MaterialAttributes {
  ref: Stream<string | undefined>;
  packageName: Stream<string | undefined>;
  packageRepoName: Stream<string | undefined>;

  constructor(name?: string, autoUpdate?: boolean, ref?: string, packageName?: string, packageRepoName?: string) {
    super(name, autoUpdate);
    this.ref             = Stream(ref);
    this.packageName     = Stream(packageName);
    this.packageRepoName = Stream(packageRepoName);
  }

  static fromJSON(data: PackageMaterialAttributesJSON): PackageMaterialAttributes {
    return new PackageMaterialAttributes(data.name, data.auto_update, data.ref, data.package_name, data.package_repo_name);
  }
}

export class PluggableScmMaterialAttributes extends MaterialAttributes {
  ref: Stream<string>;
  scmName: Stream<string>;

  constructor(name: string | undefined, autoUpdate: boolean | undefined, ref: string, scmName: string) {
    super(name, autoUpdate);
    this.ref     = Stream(ref);
    this.scmName = Stream(scmName);
  }

  static fromJSON(data: PluggableScmMaterialAttributesJSON): PluggableScmMaterialAttributes {
    return new PluggableScmMaterialAttributes(data.name, data.auto_update, data.ref, data.scm_name);
  }
}

export class MaterialWithFingerprint {
  type: Stream<string>;
  fingerprint: Stream<string>;
  attributes: Stream<MaterialAttributes>;

  constructor(type: string, fingerprint: string, attributes: MaterialAttributes) {
    this.type        = Stream(type);
    this.fingerprint = Stream(fingerprint);
    this.attributes  = Stream(attributes);
  }

  static fromJSON(data: MaterialWithFingerprintJSON): MaterialWithFingerprint {
    return new MaterialWithFingerprint(data.type, data.fingerprint, MaterialAttributes.deserialize(data));
  }

  name(): string {
    return this.attributes()!.name() || "";
  }

  typeForDisplay() {
    return mapTypeToDisplayType[this.type()!];
  }

  displayName() {
    const name = this.name();
    if (name.length > 0) {
      return name;
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

  attributesAsMap(): Map<string, any> {
    const map: Map<string, string> = new Map();
    let keys: string[]             = [];
    switch (this.type()) {
      case "git":
      case "hg":
        keys = ["url", "branch"];
        break;
      case "p4":
        keys = ["port", "view"];
        break;
      case "tfs":
        keys = ["url", "domain", "projectPath"];
        break;
      case "svn":
        keys = ["url"];
        break;
    }
    const reducer = (map: Map<any, any>, value: any, key: string) => {
      if (keys.includes(key)) {
        MaterialWithFingerprint.resolveKeyValueForAttribute(map, value, key);
      }
      return map;
    };
    _.reduce(this.attributes(), reducer, map);
    return map;
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
      case "git":
        // @ts-ignore
        return `${this.attributes()!.url()} [ ${this.attributes()!.branch()} ]`;
      default:
        // @ts-ignore
        return this.attributes()!.url();
    }
  }

  private static resolveKeyValueForAttribute(accumulator: Map<string, string>, value: any, key: string) {
    if (key.startsWith("__") || ["name"].includes(key)) {
      return accumulator;
    }

    let renderedValue = value;
    const renderedKey = humanizedMaterialAttributeName(key);

    // test for value being a stream
    if (_.isFunction(value)) {
      value = value();
    }

    // test for value being an EncryptedPassword
    if (value && value.valueForDisplay) {
      renderedValue = value.valueForDisplay();
    }

    renderedValue = _.isFunction(renderedValue) ? renderedValue() : renderedValue;
    if (key === "filter" && renderedValue) {
      renderedValue = (renderedValue as Filter).ignore();
    }
    accumulator.set(renderedKey, renderedValue);
    return accumulator;
  }
}

export class MaterialWithModification {
  config: MaterialWithFingerprint;
  modification: MaterialModification | null;

  constructor(config: MaterialWithFingerprint, modification: MaterialModification | null) {
    this.config       = config;
    this.modification = modification;
  }

  static fromJSON(data: MaterialWithModificationJSON): MaterialWithModification {
    const mod = data.modification === null ? null : MaterialModification.fromJSON(data.modification);
    return new MaterialWithModification(MaterialWithFingerprint.fromJSON(data.config), mod);
  }
}

export class Materials extends Array<MaterialWithModification> {
  constructor(...vals: MaterialWithModification[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(Materials.prototype));
  }

  static fromJSON(data: MaterialWithModificationJSON[]): Materials {
    return new Materials(...data.map((a) => MaterialWithModification.fromJSON(a)));
  }

  sortOnType() {
    this.sort((m1, m2) => m1.config.type()!.localeCompare(m2.config.type()!));
  }
}

interface ModificationsJSON {
  modifications: MaterialModificationJSON[];
}

export class MaterialModifications extends Array<MaterialModification> {
  constructor(...vals: MaterialModification[]) {
    super(...vals);
    Object.setPrototypeOf(this, Object.create(MaterialModifications.prototype));
  }

  static fromJSON(data: MaterialModificationJSON[]): MaterialModifications {
    return new MaterialModifications(...data.map((a) => MaterialModification.fromJSON(a)));
  }
}

export class MaterialAPIs {
  private static API_VERSION_HEADER = ApiVersion.latest;

  static all() {
    return ApiRequestBuilder.GET(SparkRoutes.getAllMaterials(), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const data = JSON.parse(body) as MaterialsJSON;
                              return Materials.fromJSON(data.materials);
                            }));
  }

  static modifications(fingerprint: string) {
    return ApiRequestBuilder.GET(SparkRoutes.getModifications(fingerprint), this.API_VERSION_HEADER)
                            .then((result: ApiResult<string>) => result.map((body) => {
                              const parse = JSON.parse(body) as ModificationsJSON;
                              return MaterialModifications.fromJSON(parse.modifications);
                            }));
  }
}
