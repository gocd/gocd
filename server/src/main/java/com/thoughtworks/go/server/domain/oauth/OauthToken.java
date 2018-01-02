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

import com.thoughtworks.go.server.oauth.OauthDataSource;

/**
 * @understands understands token that a client will use in header to allow service to map request back to user
 */
public class OauthToken extends OauthDomainEntity<OauthDataSource.OauthTokenDTO> {
    private String userId;
    private OauthClient oauthClient;
    private String accessToken;
    private String refreshToken;
    private long expiresAt;

    public OauthToken(String userId, OauthClient oauthClient, String accessToken, String refreshToken, long expiresAt) {
        this();
        this.userId = userId;
        this.oauthClient = oauthClient;
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.expiresAt = expiresAt;
    }

    private OauthToken() {
        //for hibernate
    }

    public OauthToken(Map attributes, OauthClient oauthClient) {
        this((String) attributes.get("user_id"),  oauthClient, (String) attributes.get("access_token"),(String) attributes.get("refresh_token"), (Long) attributes.get("expires_at"));
        setIdIfAvailable(attributes);
    }

    public OauthDataSource.OauthTokenDTO getDTO() {
        OauthDataSource.OauthTokenDTO dto = new OauthDataSource.OauthTokenDTO();
        dto.setId(getId());
        dto.setUserId(userId);
        dto.setOauthClientId(String.valueOf(oauthClient.getId()));
        dto.setAccessToken(accessToken);
        dto.setRefreshToken(refreshToken);
        dto.setExpiresAt(expiresAt);
        return dto;
    }
}
