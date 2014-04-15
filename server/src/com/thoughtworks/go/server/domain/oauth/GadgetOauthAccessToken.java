/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.domain.oauth;

import java.util.Map;

import com.thoughtworks.go.server.gadget.GadgetDataSource;

/**
 * @understands
 */
public class GadgetOauthAccessToken extends OauthDomainEntity<GadgetDataSource.GadgetOauthAccessTokenDTO> {
    private String userId;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
    private long gadgetsOauthClientId;

    public GadgetOauthAccessToken() {
    }

    public GadgetOauthAccessToken(String userId, GadgetOauthClient client, String accessToken, String refreshToken, Long expiresIn) {
        this(userId, client.getId(), accessToken, refreshToken, expiresIn);
    }

    private GadgetOauthAccessToken(String userId, long gadgetsOauthClientId, String accessToken, String refreshToken, Long expiresIn) {
        this.userId = userId;
        this.gadgetsOauthClientId = gadgetsOauthClientId;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresIn = expiresIn;
    }

    public GadgetOauthAccessToken(Map attributes) {
        this((String)attributes.get("user_id"), (Long)attributes.get("gadgets_oauth_client_id"), (String)attributes.get("access_token"), (String)attributes.get("refresh_token"),(Long) attributes.get("expires_in"));
        setIdIfAvailable(attributes);
    }

    @Override public GadgetDataSource.GadgetOauthAccessTokenDTO getDTO() {
        return new GadgetDataSource.GadgetOauthAccessTokenDTO(getId(), userId, gadgetsOauthClientId, accessToken, refreshToken, expiresIn);
    }
}
