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
import * as _ from "lodash";
import {Stream} from "mithril/stream";
import * as stream from "mithril/stream";
import {Errors} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

const TimeFormatter = require("helpers/time_formatter");

interface MetaJSON {
  revoked: boolean;
  is_revoked?: boolean;
  revoked_at: string | null;
  created_at: string;
  last_used_at: string | null;
}

export interface AccessTokenJSON {
  id: number;
  token?: string;
  description: string;
  auth_config_id: string;
  _meta: MetaJSON;
  errors?: { [key: string]: string[] };
}

interface EmbeddedJSON {
  access_tokens: AccessTokenJSON[];
}

export interface AccessTokensJSON {
  _embedded: EmbeddedJSON;
}

export class AccessTokens extends Array<Stream<AccessToken>> {
  constructor(...access_tokens: Array<Stream<AccessToken>>) {
    super(...access_tokens);
    Object.setPrototypeOf(this, Object.create(AccessTokens.prototype));
  }

  static fromJSON(json: AccessTokensJSON) {
    return new AccessTokens(...json._embedded.access_tokens.map((accessTokenJSON) => stream(AccessToken.fromJSON(
      accessTokenJSON))));
  }

  sortByCreateDate() {
    return new AccessTokens(...(_.orderBy(this, (accessToken) => {
      return accessToken().meta().createdAt().getTime();
    }, "desc")));
  }
}

class Meta {
  revoked: Stream<boolean>;
  revokedAt: Stream<Date | null>;
  createdAt: Stream<Date>;
  lastUsedAt: Stream<Date | null>;

  private constructor(revoked: boolean, revokedAt: Date | null, createdAt: Date, lastUsedAt: Date | null) {
    this.revoked    = stream(revoked);
    this.revokedAt  = stream(revokedAt);
    this.createdAt  = stream(createdAt);
    this.lastUsedAt = stream(lastUsedAt);

  }

  static fromJSON(json: MetaJSON): Meta {
    const isRevoked = json.revoked || json.is_revoked || false;
    return new Meta(isRevoked,
                    Meta.parseDate(json.revoked_at),
                    Meta.parseDate(json.created_at),
                    Meta.parseDate(json.last_used_at));
  }

  private static parseDate(dateString: string | null) {
    if (dateString) {
      return TimeFormatter.toDate(dateString);
    }
    return null;
  }
}

export class AccessToken extends ValidatableMixin {
  id: Stream<number>;
  description: Stream<string>;
  authConfigId: Stream<string>;
  token: Stream<string> = stream();
  meta: Stream<Meta>;

  private constructor(id: number,
                      description: string,
                      token?: string,
                      authConfigId?: string,
                      meta?: Meta,
                      errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.id           = stream(id);
    this.description  = stream(description);
    this.token        = stream(token);
    this.authConfigId = stream(authConfigId);
    this.meta         = stream(meta);
    this.errors(errors);
    this.validatePresenceOf("description");
  }

  static fromJSON(data: AccessTokenJSON): AccessToken {
    return new AccessToken(data.id,
                           data.description,
                           data.token,
                           data.auth_config_id,
                           Meta.fromJSON(data._meta),
                           new Errors(data.errors));
  }

  static new(): AccessToken {
    return new AccessToken(-1, "", undefined, undefined, undefined);
  }
}
