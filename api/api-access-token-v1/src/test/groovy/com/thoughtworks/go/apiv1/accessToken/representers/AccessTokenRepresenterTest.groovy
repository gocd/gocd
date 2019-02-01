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

package com.thoughtworks.go.apiv1.accessToken.representers


import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.helper.AccessTokenMother.accessTokenWithName
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AccessTokenRepresenterTest {
  @Test
  void 'renders the access token hal representation with token value'() {
    def token = accessTokenWithName("token1")

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, token, true)
    })

    def expectedJSON = [
      "_links"        : [
        "self": [
          "href": "http://test.host/go/api/access_token/token1"
        ],
        "doc" : [
          "href": apiDocsUrl('#access_token')
        ],
        "find": [
          "href": "http://test.host/go/api/access_token/:token_name"
        ]
      ],
      "name"          : token.getName(),
      "description"   : token.getDescription(),
      "auth_config_id": token.authConfigId,
      "_meta"         : [
        "is_revoked"  : token.isRevoked(),
        "revoked_at"  : null,
        "created_at"  : jsonDate(token.getCreatedAt()),
        "last_used_at": null
      ],
      "token"         : token.getOriginalValue()
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders the access token metadata hal representation without token value'() {
    def token = accessTokenWithName("token1")

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, token, false)
    })

    def expectedJSON = [
      "_links"        : [
        "self": [
          "href": "http://test.host/go/api/access_token/token1"
        ],
        "doc" : [
          "href": apiDocsUrl('#access_token')
        ],
        "find": [
          "href": "http://test.host/go/api/access_token/:token_name"
        ]
      ],
      "name"          : token.getName(),
      "description"   : token.getDescription(),
      "auth_config_id": token.authConfigId,
      "_meta"         : [
        "is_revoked"  : token.isRevoked(),
        "revoked_at"  : null,
        "created_at"  : jsonDate(token.getCreatedAt()),
        "last_used_at": null
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }
}
