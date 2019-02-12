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

import com.thoughtworks.go.domain.AccessToken
import com.thoughtworks.go.spark.util.SecureRandom
import com.thoughtworks.go.util.TestingClock
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AccessTokenRepresenterTest {
  @Test
  void 'renders the access token hal representation with token value'() {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken(42, false)

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, token)
    })

    def expectedJSON = [
      "_links"        : [
        "self": [
          "href": "http://test.host/go/api/access_tokens/42"
        ],
        "doc" : [
          "href": apiDocsUrl('#access-token')
        ],
        "find": [
          "href": "http://test.host/go/api/access_tokens/:id"
        ]
      ],
      "id"            : 42,
      "description"   : token.description,
      "auth_config_id": token.authConfigId,
      "_meta"         : [
        "is_revoked"  : token.revoked,
        "revoked_at"  : null,
        "created_at"  : jsonDate(token.createdAt),
        "last_used_at": null
      ],
      "token"         : token.displayValue
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders the access token metadata hal representation without token value'() {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken(42)
    token.displayValue = null

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, token)
    })

    def expectedJSON = [
      "_links"        : [
        "self": [
          "href": "http://test.host/go/api/access_tokens/42"
        ],
        "doc" : [
          "href": apiDocsUrl('#access-token')
        ],
        "find": [
          "href": "http://test.host/go/api/access_tokens/:id"
        ]
      ],
      "id"            : 42,
      "description"   : token.description,
      "auth_config_id": token.authConfigId,
      "_meta"         : [
        "is_revoked"  : token.revoked,
        "revoked_at"  : null,
        "created_at"  : jsonDate(token.createdAt),
        "last_used_at": null
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  static AccessToken.AccessTokenWithDisplayValue randomAccessToken(long id = SecureRandom.longNumber(), persisted = true) {
    AccessToken.AccessTokenWithDisplayValue token = AccessToken.create(SecureRandom.hex(), SecureRandom.hex(), SecureRandom.hex(), new TestingClock())
    token.id = id
    if (persisted) {
      token.displayValue = null
    }
    return token
  }

}
