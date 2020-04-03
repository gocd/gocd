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
import {Errors} from "models/mixins/errors";
import {ValidatableMixin} from "models/mixins/new_validatable_mixin";

export interface AccessTokenJSON {
  id: number;
  token?: string;
  description: string;
  username: string;
  revoked: boolean;
  revoke_cause: string;
  revoked_by: string;
  revoked_at?: string;
  created_at: string;
  last_used_at?: string;
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
    return new AccessTokens(...json._embedded.access_tokens.map((accessTokenJSON) => Stream(AccessToken.fromJSON(
      accessTokenJSON))));
  }

  sortByCreateDate() {
    return new AccessTokens(...(_.orderBy(this, (accessToken: Stream<AccessToken>) => {
      return accessToken().createdAt().getTime();
    }, "desc")));
  }

  activeTokens() {
    return new AccessTokens(..._.filter(this, (accessToken: Stream<AccessToken>) => !accessToken().revoked()));
  }

  revokedTokens() {
    return new RevokedTokens(_.filter(this, (accessToken: Stream<AccessToken>) => accessToken().revoked()));
  }

  filterBySearchText(searchText: string) {
    return new AccessTokens(..._.filter(this, (accessToken: Stream<AccessToken>) => accessToken().matches(searchText)));
  }
}

export class RevokedTokens extends AccessTokens {
  constructor(access_tokens: Array<Stream<AccessToken>>) {
    super(..._.filter(access_tokens, (accessToken: Stream<AccessToken>) => accessToken().revoked()));
    Object.setPrototypeOf(this, Object.create(RevokedTokens.prototype));
  }

  sortByRevokeTime() {
    return new AccessTokens(...(_.orderBy(this, (accessToken: Stream<AccessToken>) => {
      return accessToken().revokedAt()!.getTime();
    }, "desc")));
  }
}

export class AccessToken extends ValidatableMixin {
  id: Stream<number>;
  description: Stream<string>;
  username: Stream<string>;
  createdAt: Stream<Date>;
  lastUsedAt: Stream<Date | undefined>;
  token: Stream<string> = Stream();
  revoked: Stream<boolean>;
  revokedAt: Stream<Date | undefined>;
  revokedBy: Stream<string>;
  revokeCause: Stream<string | undefined>;
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

    this.id                        = Stream(id);
    this.description               = Stream(description);
    this.username                  = Stream(username);
    this.createdAt                 = Stream(createdAt);
    this.lastUsedAt                = Stream(lastUsedAt);
    this.token                     = Stream(token);
    this.revoked                   = Stream(revoked);
    this.revokedAt                 = Stream(revokedAt);
    this.revokedBy                 = Stream(revokedBy);
    this.revokeCause               = Stream(revokeCause);
    this.revokedBecauseUserDeleted = Stream(revokedBecauseUserDeleted || false);
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
                           AccessToken.parseDate(data.created_at) as Date,
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

  private static parseDate(dateString?: string) {
    if (dateString) {
      return timeFormatter.toDate(dateString);
    }
    return undefined;
  }
}
