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
package com.thoughtworks.go.apiv1.accessToken.representers

import com.thoughtworks.go.domain.AccessToken
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.util.SecureRandom
import com.thoughtworks.go.util.TestingClock
import org.junit.jupiter.api.Test

import java.sql.Timestamp

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AccessTokenRepresenterTest {
  @Test
  void 'renders the access token hal representation with token value'() {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken(42, false)
    def revokedAt = new Timestamp(new Date().getTime())
    token.revoke("bob", "just because", revokedAt)

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, new Routes.CurrentUserAccessToken(), token)
    })

    def expectedJSON = [
      "_links"      : [
        "doc" : [
          "href": apiDocsUrl('#access-tokens')
        ],
        "find": [
          "href": "http://test.host/go/api/current_user/access_tokens/:id"
        ]
      ],
      "description" : token.description,
      "username"    : token.username,
      "revoked"     : token.revoked,
      "revoked_at"  : jsonDate(revokedAt),
      "revoke_cause": "just because",
      "revoked_by"  : "bob",
      "created_at"  : jsonDate(token.createdAt),
      "last_used_at": null
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders the access token metadata hal representation without token value'() {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken(42, true)

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, new Routes.CurrentUserAccessToken(), token)
    })

    def expectedJSON = [
      "_links"      : [
        "self": [
          "href": "http://test.host/go/api/current_user/access_tokens/42"
        ],
        "doc" : [
          "href": apiDocsUrl('#access-tokens')
        ],
        "find": [
          "href": "http://test.host/go/api/current_user/access_tokens/:id"
        ]
      ],
      "id"          : 42,
      "description" : token.description,
      "username"    : token.username,
      "revoked"     : false,
      "revoked_by"  : null,
      "revoke_cause": null,
      "revoked_at"  : null,
      "created_at"  : jsonDate(token.createdAt),
      "last_used_at": null,
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders error messages'() {
    def token = randomAccessToken()
    token.description = "\t"
    token.validate(null)

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, new Routes.CurrentUserAccessToken(), token)
    })

    assertThatJson(json).node("errors").isEqualTo([description: ['must not be blank']])
  }

  static AccessToken.AccessTokenWithDisplayValue randomAccessToken(long id = SecureRandom.longNumber(), persisted = true) {
    AccessToken.AccessTokenWithDisplayValue token = AccessToken.create(SecureRandom.hex(), SecureRandom.hex(), SecureRandom.hex(), new TestingClock())
    token.id = id
    if (persisted) {
      token.displayValue = null
    } else {
      token.id = -1;
    }
    return token
  }

  @Test
  void 'renders access token with msg without id or token value'() {
    AccessToken.AccessTokenWithDisplayValue token = randomAccessToken(42, false)

    def json = toObjectString({
      AccessTokenRepresenter.toJSON(it, new Routes.CurrentUserAccessToken(), token)
    })

    def expectedJSON = [
      "_links"        : [
        "doc" : [
          "href": apiDocsUrl('#access-tokens')
        ],
        "find": [
          "href": "http://test.host/go/api/current_user/access_tokens/:id"
        ]
      ],
      "description"   : token.description,
      "username"      : token.username,
      "revoked"       : false,
      "revoked_by"    : null,
      "revoke_cause"  : null,
      "revoked_at"    : null,
      "created_at"    : jsonDate(token.createdAt),
      "last_used_at"  : null,
    ]

    assertThatJson(json).isEqualTo(expectedJSON);
  }

}
