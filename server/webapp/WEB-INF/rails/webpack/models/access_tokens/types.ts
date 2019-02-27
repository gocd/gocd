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

  activeTokens() {
    return new AccessTokens(..._.filter(this, (accessToken) => !accessToken().revoked()));
  }

  revokedTokens() {
    return new RevokedTokens(_.filter(this, (accessToken) => accessToken().revoked()));
  }

  filterBySearchText(searchText: string) {
    return new AccessTokens(..._.filter(this, (accessToken) => accessToken().matches(searchText)));
  }
}

class RevokedTokens extends AccessTokens {
  constructor(access_tokens: Array<Stream<AccessToken>>) {
    super(..._.filter(access_tokens, (accessToken) => accessToken().revoked()));
    Object.setPrototypeOf(this, Object.create(RevokedTokens.prototype));
  }

  sortByRevokeTime() {
    return new AccessTokens(...(_.orderBy(this, (accessToken) => {
      return accessToken().revokedAt()!.getTime();
    }, "desc")));
  }
}

export class AccessToken extends ValidatableMixin {
  id: Stream<number>;
  description: Stream<string>;
  username: Stream<string>;
  createdAt: Stream<Date>;
  lastUsedAt: Stream<Date>;
  token: Stream<string> = stream();
  revoked: Stream<boolean>;
  revokedAt: Stream<Date>;
  revokedBy: Stream<string>;
  revokeCause: Stream<string>;
  revokedBecauseUserDeleted: Stream<boolean>;

  private constructor(id: number,
                      description: string,
                      username: string,
                      revoked: boolean,
                      revokedBy: string,
                      revokedAt: Date | undefined,
                      createdAt: Date,
                      lastUsedAt: Date | undefined,
                      revokeCause?: string,
                      revokedBecauseUserDeleted?: boolean,
                      token: string  = "",
                      errors: Errors = new Errors()) {
    super();
    ValidatableMixin.call(this);
    this.id                        = stream(id);
    this.description               = stream(description);
    this.username                  = stream(username);
    this.createdAt                 = stream(createdAt);
    this.lastUsedAt                = stream(lastUsedAt);
    this.token                     = stream(token);
    this.revoked                   = stream(revoked);
    this.revokedAt                 = stream(revokedAt);
    this.revokedBy                 = stream(revokedBy);
    this.revokeCause               = stream(revokeCause);
    this.revokedBecauseUserDeleted = stream(revokedBecauseUserDeleted || false);
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
                           data.revoke_cause,
                           data.revoked_because_user_deleted || false,
                           data.token,
                           new Errors(data.errors));
  }

  static new(): AccessToken {
    return new AccessToken(-1, "", "", false, "", undefined, new Date(), undefined, undefined, false);
  }

  matches(searchText: string) {
    if (searchText) {
      searchText               = searchText.toLowerCase();
      const matchesDescription = this.description().toLowerCase().includes(searchText);
      const matchesUsername    = this.username().toLowerCase().includes(searchText);
      return matchesDescription || matchesUsername;
    }
    return true;
  }

  private static parseDate(dateString: string | null) {
    if (dateString) {
      return TimeFormatter.toDate(dateString);
    }
    return undefined;
  }
}
