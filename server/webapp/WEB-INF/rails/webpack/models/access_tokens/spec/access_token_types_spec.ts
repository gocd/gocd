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

import {AccessTokenTestData} from "models/access_tokens/spec/access_token_test_data";
import {AccessToken, AccessTokens} from "models/access_tokens/types";

const TimeFormatter = require("helpers/time_formatter");

describe("AccessTokenTypesSpec", () => {

  it("should deserialize json", () => {
    const accessTokens = AccessTokens.fromJSON(AccessTokenTestData.all());

    expect(accessTokens).toHaveLength(2);
    const validAccessTokenJSON   = AccessTokenTestData.validAccessToken();
    const revokedAccessTokenJSON = AccessTokenTestData.revokedAccessToken();

    expect(accessTokens[0]().id()).toEqual(validAccessTokenJSON.id);
    expect(accessTokens[0]().description()).toEqual(validAccessTokenJSON.description);
    expect(accessTokens[0]().authConfigId()).toEqual(validAccessTokenJSON.auth_config_id);
    expect(accessTokens[0]().revoked()).toEqual(false);
    expect(accessTokens[0]().revokedAt()).toEqual(null);
    expect(accessTokens[0]().createdAt())
      .toEqual(TimeFormatter.toDate(validAccessTokenJSON.created_at));
    expect(accessTokens[0]().lastUsedAt())
      .toEqual(TimeFormatter.toDate(validAccessTokenJSON.last_used_at));

    expect(accessTokens[1]().id()).toEqual(revokedAccessTokenJSON.id);
    expect(accessTokens[1]().description()).toEqual(revokedAccessTokenJSON.description);
    expect(accessTokens[1]().authConfigId()).toEqual(revokedAccessTokenJSON.auth_config_id);
    expect(accessTokens[1]().revoked()).toEqual(true);
    expect(accessTokens[1]().revokedAt())
      .toEqual(TimeFormatter.toDate(revokedAccessTokenJSON.revoked_at));
    expect(accessTokens[1]().createdAt())
      .toEqual(TimeFormatter.toDate(revokedAccessTokenJSON.created_at));
    expect(accessTokens[1]().lastUsedAt())
      .toEqual(TimeFormatter.toDate(revokedAccessTokenJSON.last_used_at));
  });

  it("should deserialize token if provided", () => {
    const newAccessTokenWithTokenText = AccessTokenTestData.newAccessTokenWithTokenText();
    const accessToken                 = AccessToken.fromJSON(newAccessTokenWithTokenText);

    expect(accessToken.description()).toEqual(newAccessTokenWithTokenText.description);
    expect(accessToken.token()).toEqual(newAccessTokenWithTokenText.token as string);
  });

});
