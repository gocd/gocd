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
import {Stream} from "mithril/stream";
import stream = require("mithril/stream");

const TimeFormatter = require("helpers/time_formatter");

interface MetaJSON {
  revoked: boolean;
  revoked_at: string;
  created_at: string;
  last_used_at: string;
}

export interface AccessTokenJSON {
  name: string;
  description: string;
  auth_config_id: string;
  _meta: MetaJSON;
}

interface EmbeddedJSON {
  access_tokens: AccessTokenJSON[];
}

export interface AccessTokensJSON {
  _embedded: EmbeddedJSON;
}

export class AccessTokens extends Array<AccessToken> {
  constructor(...access_tokens: AccessToken[]) {
    super(...access_tokens);
    Object.setPrototypeOf(this, Object.create(AccessTokens.prototype));
  }

  static fromJSON(json: AccessTokensJSON) {
    return new AccessTokens(...json._embedded.access_tokens.map((accessTokenJSON) => AccessToken.fromJSON(
      accessTokenJSON)));
  }
}

class Meta {
  revoked: Stream<boolean>;
  revokedAt: Stream<Date | null>;
  createdAt: Stream<Date>;
  lastUsedAt: Stream<Date | null>;

  constructor(revoked: boolean, revokedAt: Date | null, createdAt: Date, lastUsedAt: Date | null) {
    this.revoked    = stream(revoked);
    this.revokedAt  = stream(revokedAt);
    this.createdAt  = stream(createdAt);
    this.lastUsedAt = stream(lastUsedAt);

  }

  static fromJSON(json: MetaJSON): Meta {
    return new Meta(json.revoked,
                    Meta.parseDate(json.revoked_at),
                    Meta.parseDate(json.created_at),
                    Meta.parseDate(json.last_used_at));
  }

  private static parseDate(dateString?: string) {
    if (dateString) {
      return TimeFormatter.format(dateString);
    }
    return null;
  }
}

export class AccessToken {
  name: Stream<string>;
  description: Stream<string>;
  authConfigId: Stream<string>;
  meta: Stream<Meta>;

  constructor(name: string, description: string, authConfigId: string, meta: Meta) {
    this.name         = stream(name);
    this.description  = stream(description);
    this.authConfigId = stream(authConfigId);
    this.meta         = stream(meta);
  }

  static fromJSON(data: AccessTokenJSON): AccessToken {
    return new AccessToken(data.name, data.description, data.auth_config_id, Meta.fromJSON(data._meta));
  }
}
