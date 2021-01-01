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
import Stream from "mithril/stream";
import {EncryptedValue, plainOrCipherValue} from "views/components/forms/encrypted_value";
import {Filter} from "../maintenance_mode/material";
import {DependencyMaterialAttributesJSON, GitMaterialAttributesJSON, HgMaterialAttributesJSON, MaterialJSON, P4MaterialAttributesJSON, PackageMaterialAttributesJSON, PluggableScmMaterialAttributesJSON, SvnMaterialAttributesJSON, TfsMaterialAttributesJSON} from "./material_json";
import {stringOrNull} from "./pipeline_instance_json";

export class ChangeMaterial {
  type: Stream<string>;
  attributes: Stream<MaterialAttributes>;

  constructor(type: string, attributes: MaterialAttributes) {
    this.attributes = Stream(attributes);
    this.type       = Stream(type);
  }

  static fromJSON(json: MaterialJSON) {
    return new ChangeMaterial(json.type, MaterialAttributes.deserialize(json));
  }
}

type stringOrNullOrUndefined = stringOrNull | undefined;

abstract class MaterialAttributes {
  name: Stream<stringOrNullOrUndefined>;
  autoUpdate: Stream<boolean>;
  displayType: Stream<string>;
  description: Stream<string>;

  protected constructor(name: stringOrNullOrUndefined, autoUpdate: boolean, displayType: string, description: string) {
    this.name        = Stream(name);
    this.autoUpdate  = Stream(autoUpdate);
    this.displayType = Stream(displayType);
    this.description = Stream(description);
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
}

abstract class ScmMaterialAttributes extends MaterialAttributes {
  static readonly DESTINATION_REGEX = new RegExp(
    "^(?!\\/)((([\\.]\\/)?[\\.][^. ]+)|([^. ].+[^. ])|([^. ][^. ])|([^. ]))$");
  destination: Stream<string>       = Stream();
  username: Stream<string | undefined>;
  password: Stream<EncryptedValue>;

  constructor(name: stringOrNullOrUndefined, autoUpdate: boolean, displayType: string, description: string, username?: string, password?: string, encryptedPassword?: string) {
    super(name, autoUpdate, displayType, description);

    this.username = Stream(username);
    this.password = Stream(plainOrCipherValue({plainText: password, cipherText: encryptedPassword}));
  }
}

export class GitMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  branch: Stream<string | undefined>;

  constructor(name: stringOrNullOrUndefined, autoUpdate: boolean, displayType: string, description: string, url?: string, branch?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, displayType, description, username, password, encryptedPassword);
    this.url    = Stream(url);
    this.branch = Stream(branch);
  }

  static fromJSON(json: GitMaterialAttributesJSON) {
    const attrs = new GitMaterialAttributes(json.name, json.auto_update, json.display_type, json.description, json.url, json.branch, json.username, json.password, json.encrypted_password,);
    if (json.destination !== undefined) {
      attrs.destination(json.destination);
    }
    return attrs;
  }
}

export class SvnMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  checkExternals: Stream<boolean | undefined>;

  constructor(name: stringOrNullOrUndefined,
              autoUpdate: boolean,
              displayType: string,
              description: string,
              url?: string,
              checkExternals?: boolean,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, displayType, description, username, password, encryptedPassword);
    this.url            = Stream(url);
    this.checkExternals = Stream(checkExternals);
  }

  static fromJSON(json: SvnMaterialAttributesJSON) {
    const attrs = new SvnMaterialAttributes(json.name, json.auto_update, json.display_type, json.description, json.url, json.check_externals, json.username, json.password, json.encrypted_password);
    if (json.destination !== undefined) {
      attrs.destination(json.destination);
    }
    return attrs;
  }
}

export class HgMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  branch: Stream<string | undefined>;

  constructor(name: stringOrNullOrUndefined,
              autoUpdate: boolean,
              displayType: string,
              description: string,
              url?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string,
              branch?: string) {
    super(name, autoUpdate, displayType, description, username, password, encryptedPassword);
    this.url    = Stream(url);
    this.branch = Stream(branch);
  }

  static fromJSON(json: HgMaterialAttributesJSON) {
    const attrs = new HgMaterialAttributes(json.name, json.auto_update, json.display_type, json.description, json.url, json.username, json.password, json.encrypted_password, json.branch);
    if (json.destination !== undefined) {
      attrs.destination(json.destination);
    }
    return attrs;
  }
}

export class P4MaterialAttributes extends ScmMaterialAttributes {
  port: Stream<string | undefined>;
  useTickets: Stream<boolean | undefined>;
  view: Stream<string | undefined>;

  constructor(name: stringOrNullOrUndefined,
              autoUpdate: boolean,
              displayType: string,
              description: string,
              port?: string,
              useTickets?: boolean,
              view?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, displayType, description, username, password, encryptedPassword);
    this.port       = Stream(port);
    this.useTickets = Stream(useTickets);
    this.view       = Stream(view);
  }

  static fromJSON(json: P4MaterialAttributesJSON) {
    const attrs = new P4MaterialAttributes(json.name, json.auto_update, json.display_type, json.description, json.port, json.use_tickets, json.view, json.username, json.password, json.encrypted_password);

    if (json.destination !== undefined) {
      attrs.destination(json.destination);
    }
    return attrs;
  }
}

export class TfsMaterialAttributes extends ScmMaterialAttributes {
  url: Stream<string | undefined>;
  domain: Stream<string | undefined>;
  projectPath: Stream<string | undefined>;

  constructor(name: stringOrNullOrUndefined,
              autoUpdate: boolean,
              displayType: string,
              description: string,
              url?: string,
              domain?: string,
              projectPath?: string,
              username?: string,
              password?: string,
              encryptedPassword?: string) {
    super(name, autoUpdate, displayType, description, username, password, encryptedPassword);
    this.url         = Stream(url);
    this.domain      = Stream(domain);
    this.projectPath = Stream(projectPath);
  }

  static fromJSON(json: TfsMaterialAttributesJSON) {
    const attrs = new TfsMaterialAttributes(json.name, json.auto_update, json.display_type, json.description, json.url, json.domain, json.project_path, json.username, json.password, json.encrypted_password);
    if (json.destination !== undefined) {
      attrs.destination(json.destination);
    }
    return attrs;
  }
}

export class DependencyMaterialAttributes extends MaterialAttributes {
  pipeline: Stream<string | undefined>;
  stage: Stream<string | undefined>;

  constructor(name: stringOrNullOrUndefined, autoUpdate: boolean, displayType: string, description: string, pipeline: string | undefined, stage: string | undefined) {
    super(name, autoUpdate, displayType, description);
    this.pipeline = Stream(pipeline);
    this.stage    = Stream(stage);
  }

  static fromJSON(json: DependencyMaterialAttributesJSON) {
    return new DependencyMaterialAttributes(json.name, json.auto_update, json.display_type, json.description, json.pipeline, json.stage);
  }
}

export class PackageMaterialAttributes extends MaterialAttributes {
  ref: Stream<string | undefined>;

  constructor(name: stringOrNullOrUndefined, autoUpdate: boolean, displayType: string, description: string, ref?: string) {
    super(name, autoUpdate, displayType, description);
    this.ref = Stream(ref);
  }

  static fromJSON(data: PackageMaterialAttributesJSON): PackageMaterialAttributes {
    return new PackageMaterialAttributes(data.name, data.auto_update, data.display_type, data.description, data.ref);
  }
}

export class PluggableScmMaterialAttributes extends MaterialAttributes {
  ref: Stream<string>;
  filter: Stream<Filter>;
  destination: Stream<string>;

  constructor(name: stringOrNullOrUndefined, autoUpdate: boolean, displayType: string, description: string, ref: string, destination: string, filter: Filter) {
    super(name, autoUpdate, displayType, description);
    this.ref         = Stream(ref);
    this.filter      = Stream(filter);
    this.destination = Stream(destination);
  }

  static fromJSON(data: PluggableScmMaterialAttributesJSON): PluggableScmMaterialAttributes {
    return new PluggableScmMaterialAttributes(data.name, data.auto_update, data.display_type, data.description, data.ref, data.destination, Filter.fromJSON(data.filter));
  }
}
