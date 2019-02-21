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

export interface AccessTokenJSON {
  id: number;
  token?: string;
  description: string;
  username: string;
  revoked: boolean;
  revoke_cause: string;
  revoked_by: string;
  revoked_at: string | null;
  created_at: string;
  last_used_at: string | null;
  revoked_because_user_deleted?: boolean;
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
      return accessToken().createdAt().getTime();
    }, "desc")));
  }
}

export class AccessToken extends ValidatableMixin {
  id: Stream<number>;
  description: Stream<string>;
  username: Stream<string>;
  revoked: Stream<boolean>;
  revokedBy: Stream<string>;
  revokedAt: Stream<Date | null>;
  createdAt: Stream<Date>;
  lastUsedAt: Stream<Date | null>;
  revokedBecauseUserDeleted: Stream<boolean>;
  token: Stream<string> = stream();

  private constructor(id: number,
                      description: string,
                      username: string,
                      revoked: boolean,
                      revokedBy: string,
                      revokedAt: Date | null,
                      createdAt: Date,
                      lastUsedAt: Date | null,
                      revokedBecauseUserDeleted: boolean,
                      token: string  = "",
                      errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.id                        = stream(id);
    this.description               = stream(description);
    this.username                  = stream(username);
    this.revoked                   = stream(revoked);
    this.revokedBy                 = stream(revokedBy);
    this.revokedAt                 = stream(revokedAt);
    this.createdAt                 = stream(createdAt);
    this.lastUsedAt                = stream(lastUsedAt);
    this.revokedBecauseUserDeleted = stream(revokedBecauseUserDeleted || false);
    this.token                     = stream(token);
    this.errors(errors);
    this.validatePresenceOf("description");
  }

  static fromJSON(data: AccessTokenJSON): AccessToken {
    return new AccessToken(data.id,
                           data.description,
                           data.username,
                           data.revoked,
                           data.revoked_by,
                           AccessToken.parseDate(data.revoked_at),
                           AccessToken.parseDate(data.created_at),
                           AccessToken.parseDate(data.last_used_at),
                           data.revoked_because_user_deleted || false,
                           data.token,
                           new Errors(data.errors));
  }

  static new(): AccessToken {
    return new AccessToken(-1, "", "", false, "", null, new Date(), null, false);
  }

  private static parseDate(dateString: string | null) {
    if (dateString) {
      return TimeFormatter.toDate(dateString);
    }
    return null;
  }
}
