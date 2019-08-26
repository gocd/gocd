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

import Stream from "mithril/stream";
import {Errors, ErrorsJSON} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configurations, PropertyJSON} from "models/shared/configuration";

export enum RoleType {
  gocd, plugin
}

export interface GoCDAttributesJSON {
  users: string[];
}

export interface PluginAttributesJSON {
  auth_config_id: string;
  properties: PropertyJSON[];
}

export type AttributeType = GoCDAttributesJSON | PluginAttributesJSON;

export interface RoleJSON {
  name: string;
  type: string;
  attributes: AttributeType;
  errors?: ErrorsJSON;
}

interface EmbeddedJSON {
  roles: RoleJSON[];
}

export interface RolesJSON {
  _embedded: EmbeddedJSON;
}

export interface UserRoleUpdateJSON {
  role: string;
  users: {
    add?: string[];
    remove?: string[];
  };
}

export interface BulkUserRoleUpdateJSON {
  operations: UserRoleUpdateJSON[];
}

export class GoCDAttributes {
  users: string[];

  constructor(users: string[]) {
    this.users = users;
  }

  static deserialize(data: GoCDAttributesJSON) {
    return new GoCDAttributes(data.users);
  }

  addUser(username: string) {
    if (this.hasUser(username)) {
      return;
    }
    this.users.push(username);
  }

  hasUser(username: string) {
    if (username && username.length > 0) {
      const index = this.users.indexOf(username, 0);
      return index > -1;
    }
    return false;
  }

  deleteUser(username: string) {
    const index = this.users.indexOf(username, 0);
    if (index > -1) {
      this.users.splice(index, 1);
    }
  }

  toJSON() {
    return {
      users: this.users
    };
  }
}

export class PluginAttributes {
  authConfigId: string;
  private __properties: Configurations;

  constructor(authConfigId: string, properties: Configurations) {
    this.authConfigId = authConfigId;
    this.__properties = properties;
  }

  static deserialize(data: PluginAttributesJSON) {
    return new PluginAttributes(data.auth_config_id, Configurations.fromJSON(data.properties));
  }

  properties() {
    return this.__properties;
  }

  toJSON() {
    return {
      auth_config_id: this.authConfigId,
      properties: this.properties().allConfigurations().map((config) => {
        return {
          key: config.key,
          value: config.displayValue()
        };
      })
    };
  }
}

export abstract class Role<T> extends ValidatableMixin {
  name: Stream<string>;
  type: Stream<RoleType>;
  attributes: Stream<T>;

  protected constructor(name: string, type: RoleType, attributes: T, errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);

    this.type       = Stream(type);
    this.name       = Stream(name);
    this.attributes = Stream(attributes);
    this.errors(errors);

    this.validatePresenceOf("name");
    this.validateFormatOf("name",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("name", 255, {message: "The maximum allowed length is 255 characters."});
  }

  static fromJSON(data: RoleJSON) {
    const errors = new Errors(data.errors);
    switch (data.type) {
      case "gocd":
        return new GoCDRole(data.name,
                            GoCDAttributes.deserialize(data.attributes as GoCDAttributesJSON),
                            errors);
      case "plugin":
        return new PluginRole(data.name,
                              PluginAttributes.deserialize(data.attributes as PluginAttributesJSON), errors);
      default:
        throw new Error(`Unknown role type ${data.type}`);
    }
  }

  isPluginRole() {
    return this.type() === RoleType.plugin;
  }

  toJSON() {
    return {
      name: this.name(),
      type: RoleType[this.type()],
      attributes: this.attributes.toJSON()
    };
  }
}

applyMixins(Role, ValidatableMixin);

export class GoCDRole extends Role<GoCDAttributes> {

  constructor(name: string, attributes: GoCDAttributes, errors?: Errors) {
    super(name, RoleType.gocd, attributes, errors);
  }
}

export class PluginRole extends Role<PluginAttributes> {

  constructor(name: string, attributes: PluginAttributes, errors?: Errors) {
    super(name, RoleType.plugin, attributes, errors);
  }
}

export class Roles extends Array<GoCDRole | PluginRole> {

  constructor(...roles: Array<GoCDRole | PluginRole>) {
    super(...roles);
  }

  static fromJSON(data: RolesJSON) {
    const roles = data._embedded.roles.map((value: RoleJSON) => {
      return Role.fromJSON(value);
    });
    return new Roles(...roles);
  }
}
