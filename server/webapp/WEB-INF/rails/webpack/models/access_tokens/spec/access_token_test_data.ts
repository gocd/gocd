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

import {AccessTokenJSON, AccessTokensJSON} from "models/access_tokens/types";

export class AccessTokenTestData {
  static all() {
    return {
      _embedded: {
        access_tokens: [AccessTokenTestData.validAccessToken(), AccessTokenTestData.revokedAccessToken()]

      }
    } as AccessTokensJSON;
  }

  static validAccessToken() {
    return {
      name: "ValidToken",
      description: "This is dummy description",
      auth_config_id: "auth_config_id0",
      _meta: {
        revoked: false,
        revoked_at: null,
        created_at: "2019-02-05T06:41:58Z",
        last_used_at: "2019-02-05T07:41:58Z"
      }
    } as AccessTokenJSON;
  }

  static newAccessTokenWithTokenText() {
    return {
      name: "ValidToken",
      description: "This is dummy description",
      auth_config_id: "auth_config_id0",
      token: "05724e1de425eeeaf750b7cb2ee98636abdf",
      _meta: {
        revoked: false,
        revoked_at: null,
        created_at: "2019-02-05T06:41:58Z",
        last_used_at: "2019-02-05T07:41:58Z"
      }
    } as AccessTokenJSON;
  }

  static revokedAccessToken() {
    return {
      name: "RevokedToken",
      description: "This is token is revoked",
      auth_config_id: "auth_config_id0",
      _meta: {
        revoked: true,
        revoked_at: "2019-02-05T06:41:58Z",
        created_at: "2019-02-04T06:41:58Z",
        last_used_at: "2019-02-04T07:41:58Z"
      }
    } as AccessTokenJSON;
  }
}
