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

class AccessTokensRepresenterTest {
  @Test
  void 'renders the access tokens hal representation without token value'() {
    def token1 = accessTokenWithName("token1")
    def token2 = accessTokenWithName("token2")

    def json = toObjectString({
      AccessTokensRepresenter.toJSON(it, [token1, token2])
    })

    def expectedJSON = [
      "_links"   : [
        "self": [
          "href": "http://test.host/go/api/access_token"
        ],
        "doc" : [
          "href": apiDocsUrl('#access_token')
        ],
      ],
      "_embedded": [
        "access_tokens": [
          [
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
            "name"          : token1.getName(),
            "description"   : token1.getDescription(),
            "auth_config_id": token1.authConfigId,
            "_meta"         : [
              "is_revoked"  : token1.isRevoked(),
              "revoked_at"  : null,
              "created_at"  : jsonDate(token1.getCreatedAt()),
              "last_used_at": null
            ]
          ],
          [
            "_links"        : [
              "self": [
                "href": "http://test.host/go/api/access_token/token2"
              ],
              "doc" : [
                "href": apiDocsUrl('#access_token')
              ],
              "find": [
                "href": "http://test.host/go/api/access_token/:token_name"
              ]
            ],
            "name"          : token2.getName(),
            "description"   : token2.getDescription(),
            "auth_config_id": token2.authConfigId,
            "_meta"         : [
              "is_revoked"  : token2.isRevoked(),
              "revoked_at"  : null,
              "created_at"  : jsonDate(token2.getCreatedAt()),
              "last_used_at": null
            ]
          ]
        ]
      ]
    ]

    assertThatJson(json).isEqualTo(expectedJSON)
  }
}
