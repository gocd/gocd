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
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static com.thoughtworks.go.apiv1.accessToken.representers.AccessTokenRepresenterTest.randomAccessToken
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class AccessTokensRepresenterTest {
  @Test
  void 'renders the access tokens hal representation without token value'() {
    AccessToken.AccessTokenWithDisplayValue token1 = randomAccessToken(41, true)
    AccessToken.AccessTokenWithDisplayValue token2 = randomAccessToken(42, true)

    def json = toObjectString({
      AccessTokensRepresenter.toJSON(it, [token1, token2])
    })

    def expectedJSON = [
      "_links"   : [
        "self": [
          "href": "http://test.host/go/api/access_tokens"
        ],
        "doc" : [
          "href": apiDocsUrl('#access-token')
        ],
      ],
      "_embedded": [
        "access_tokens": [
          [
            "_links"        : [
              "self": [
                "href": "http://test.host/go/api/access_tokens/41"
              ],
              "doc" : [
                "href": apiDocsUrl('#access-token')
              ],
              "find": [
                "href": "http://test.host/go/api/access_tokens/:id"
              ]
            ],
            "id"          : token1.id,
            "description"   : token1.description,
            "auth_config_id": token1.authConfigId,
            "_meta"         : [
              "revoked"  : token1.revoked,
              "revoked_at"  : null,
              "created_at"  : jsonDate(token1.createdAt),
              "last_used_at": null
            ]
          ],
          [
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
            "id"          : token2.id,
            "description"   : token2.description,
            "auth_config_id": token2.authConfigId,
            "_meta"         : [
              "revoked"  : token2.revoked,
              "revoked_at"  : null,
              "created_at"  : jsonDate(token2.createdAt),
              "last_used_at": null
            ]
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }
}
