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

import {timeFormatter} from "helpers/time_formatter";
import {AccessTokenTestData} from "models/access_tokens/spec/access_token_test_data";
import {AccessToken, AccessTokens} from "models/access_tokens/types";

describe("AccessTokenTypesSpec", () => {
  describe("AccessTokens", () => {
    it("should deserialize json", () => {
      const accessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());

      expect(accessTokens).toHaveLength(2);
      const validAccessTokenJSON   = AccessTokenTestData.validAccessToken();
      const revokedAccessTokenJSON = AccessTokenTestData.revokedAccessToken();

      expect(accessTokens[0]().id()).toEqual(validAccessTokenJSON.id);
      expect(accessTokens[0]().description()).toEqual(validAccessTokenJSON.description);
      expect(accessTokens[0]().revokedBecauseUserDeleted()).toEqual(false);
      expect(accessTokens[0]().revoked()).toEqual(false);
      expect(accessTokens[0]().revokedAt()).toBeUndefined();
      expect(accessTokens[0]().createdAt())
        .toEqual(timeFormatter.toDate(validAccessTokenJSON.created_at));
      expect(accessTokens[0]().lastUsedAt())
        .toEqual(timeFormatter.toDate(validAccessTokenJSON.last_used_at));

      expect(accessTokens[1]().id()).toEqual(revokedAccessTokenJSON.id);
      expect(accessTokens[1]().description()).toEqual(revokedAccessTokenJSON.description);
      expect(accessTokens[1]().revokedBecauseUserDeleted()).toEqual(true);
      expect(accessTokens[1]().revoked()).toEqual(true);
      expect(accessTokens[1]().revokedAt())
        .toEqual(timeFormatter.toDate(revokedAccessTokenJSON.revoked_at));
      expect(accessTokens[1]().createdAt())
        .toEqual(timeFormatter.toDate(revokedAccessTokenJSON.created_at));
      expect(accessTokens[1]().lastUsedAt())
        .toEqual(timeFormatter.toDate(revokedAccessTokenJSON.last_used_at));
    });

    it("should filter access token with matching search text", () => {
      const accessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());

      const filteredTokens = accessTokens.filterBySearchText("ToKeN");

      expect(filteredTokens).toHaveLength(1);
      expect(filteredTokens[0]().id()).toBe(3);
    });

    it("should return active access tokens", () => {
      const accessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());

      const activeTokens = accessTokens.activeTokens();

      expect(activeTokens.length).toBe(1);
      expect(activeTokens[0]().id()).toBe(1);
    });

    it("should return revoked access tokens", () => {
      const accessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());

      const revokedTokens = accessTokens.revokedTokens();

      expect(revokedTokens.length).toBe(1);
      expect(revokedTokens[0]().id()).toBe(3);
    });

    it("should sort access tokens by created at date", () => {
      const accessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());

      const revokedTokens = accessTokens.sortByCreateDate();

      expect(revokedTokens[0]().id()).toBe(3);
      expect(revokedTokens[1]().id()).toBe(1);
    });
  });

  describe("AccessToken", () => {
    it("should deserialize token if provided", () => {
      const newAccessTokenWithTokenText = AccessTokenTestData.newAccessTokenWithTokenText();
      const accessToken                 = AccessToken.fromJSON(newAccessTokenWithTokenText);

      expect(accessToken.description()).toEqual(newAccessTokenWithTokenText.description);
      expect(accessToken.token()).toEqual(newAccessTokenWithTokenText.token as string);
    });

    it("should return true for `matches` if description matches with given search text", () => {
      const searchText                  = "DuMmY";
      const newAccessTokenWithTokenText = AccessTokenTestData.newAccessTokenWithTokenText();
      const accessToken                 = AccessToken.fromJSON(newAccessTokenWithTokenText);

      expect(accessToken.matches(searchText)).toEqual(true);
    });

    it("should return true for `matches` if username matches with given search text", () => {
      const searchText                  = "aDmIn";
      const newAccessTokenWithTokenText = AccessTokenTestData.newAccessTokenWithTokenText();
      const accessToken                 = AccessToken.fromJSON(newAccessTokenWithTokenText);

      expect(accessToken.matches(searchText)).toEqual(true);
    });

    it("should return false for `matches` if description matches with given search text", () => {
      const searchText                  = "some random text";
      const newAccessTokenWithTokenText = AccessTokenTestData.newAccessTokenWithTokenText();
      const accessToken                 = AccessToken.fromJSON(newAccessTokenWithTokenText);

      expect(accessToken.matches(searchText)).toEqual(false);
    });
  });

});
