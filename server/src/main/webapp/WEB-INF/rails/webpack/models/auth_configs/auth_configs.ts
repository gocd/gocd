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

import Stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {applyMixins} from "models/mixins/mixins";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";
import {Configurations, PropertyJSON} from "models/shared/configuration";

export interface AuthConfigJSON {
  id: string;
  plugin_id: string;
  allow_only_known_users_to_login: boolean;
  properties: PropertyJSON[];
  errors?: { [key: string]: string[] };
}

interface EmbeddedJSON {
  auth_configs: AuthConfigJSON[];
}

export interface AuthConfigsJSON {
  _embedded: EmbeddedJSON;
}

export class AuthConfig extends ValidatableMixin {
  id: Stream<string | undefined>;
  pluginId: Stream<string | undefined>;
  allowOnlyKnownUsersToLogin: Stream<boolean>;
  properties: Stream<Configurations | undefined>;

  constructor(id?: string,
              pluginId?: string,
              allowOnlyKnownUsersToLogin?: boolean,
              properties?: Configurations,
              errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.id                         = Stream(id);
    this.pluginId                   = Stream(pluginId);
    this.allowOnlyKnownUsersToLogin = Stream(allowOnlyKnownUsersToLogin || false);
    this.properties                 = Stream(properties);
    this.errors(errors);

    this.validatePresenceOf("pluginId");
    this.validatePresenceOf("id");
    this.validateFormatOf("id",
                          new RegExp("^[-a-zA-Z0-9_][-a-zA-Z0-9_.]*$"),
                          {message: "Invalid Id. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period)."});
    this.validateMaxLength("id", 255, {message: "The maximum allowed length is 255 characters."});
  }

  static fromJSON(data: AuthConfigJSON) {
    const configurations = Configurations.fromJSON(data.properties);
    const errors         = new Errors(data.errors);
    return new AuthConfig(data.id, data.plugin_id, data.allow_only_known_users_to_login, configurations, errors);
  }

  toJSON(): object {
    return {
      id: this.id,
      plugin_id: this.pluginId,
      allow_only_known_users_to_login: this.allowOnlyKnownUsersToLogin,
      properties: this.properties
    };
  }
}

applyMixins(AuthConfig, ValidatableMixin);

export class AuthConfigs extends Array<AuthConfig> {
  constructor(...authConfigs: AuthConfig[]) {
    super(...authConfigs);
    Object.setPrototypeOf(this, Object.create(AuthConfigs.prototype));
  }

  static fromJSON(data: AuthConfigsJSON) {
    return new AuthConfigs(...data._embedded.auth_configs.map(AuthConfig.fromJSON));
  }

  findById(authConfigId: string) {
    return this.find((authConfig) => authConfig.id() === authConfigId) as AuthConfig;
  }
}
