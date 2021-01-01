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
package com.thoughtworks.go.apiv1.accessToken.representers

import com.thoughtworks.go.domain.AccessToken
import com.thoughtworks.go.spark.Routes
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
      AccessTokensRepresenter.toJSON(it, new Routes.CurrentUserAccessToken(), [token1, token2])
    })

    def expectedJSON = [
      "_links"   : [
        "self": [
          "href": "http://test.host/go/api/current_user/access_tokens"
        ],
        "doc" : [
          "href": apiDocsUrl('#access-tokens')
        ],
      ],
      "_embedded": [
        "access_tokens": [
          [
            "_links"      : [
              "self": [
                "href": "http://test.host/go/api/current_user/access_tokens/41"
              ],
              "doc" : [
                "href": apiDocsUrl('#access-tokens')
              ],
              "find": [
                "href": "http://test.host/go/api/current_user/access_tokens/:id"
              ]
            ],
            "id"          : token1.id,
            "description" : token1.description,
            "username"    : token1.username,
            "revoked"     : token1.revoked,
            "revoked_at"  : null,
            "revoke_cause": null,
            "revoked_by"  : null,
            "created_at"  : jsonDate(token1.createdAt),
            "last_used_at": null
          ],
          [
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
            "id"          : token2.id,
            "description" : token2.description,
            "username"    : token2.username,
            "revoked"     : token2.revoked,
            "revoked_at"  : null,
            "revoke_cause": null,
            "revoked_by"  : null,
            "created_at"  : jsonDate(token2.createdAt),
            "last_used_at": null
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }

  @Test
  void 'renders the access token for admin users without token value'() {
    AccessToken.AccessTokenWithDisplayValue token1 = randomAccessToken(41, true)
    def json = toObjectString({
      AccessTokensRepresenter.toJSON(it, new Routes.AdminUserAccessToken(), [token1])
    })

    def expectedJSON = [
      "_links"   : [
        "self": [
          "href": "http://test.host/go/api/admin/access_tokens"
        ],
        "doc" : [
          "href": apiDocsUrl('#access-tokens')
        ],
      ],
      "_embedded": [
        "access_tokens": [
          [
            "_links"                      : [
              "self": [
                "href": "http://test.host/go/api/admin/access_tokens/41"
              ],
              "doc" : [
                "href": apiDocsUrl('#access-tokens')
              ],
              "find": [
                "href": "http://test.host/go/api/admin/access_tokens/:id"
              ]
            ],
            "id"                          : token1.id,
            "description"                 : token1.description,
            "username"                    : token1.username,
            "revoked"                     : token1.revoked,
            "revoked_at"                  : null,
            "revoke_cause"                : null,
            "revoked_by"                  : null,
            "created_at"                  : jsonDate(token1.createdAt),
            "revoked_because_user_deleted": false,
            "last_used_at"                : null
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }
}
