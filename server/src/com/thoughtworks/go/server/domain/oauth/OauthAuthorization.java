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
 * @understands authorization of oauth client to access service on users behalf
 */
public class OauthAuthorization extends OauthDomainEntity<OauthDataSource.OauthAuthorizationDTO> {
    private String userId;
    private OauthClient oauthClient;
    private String code;
    private long expiresAt;

    public OauthAuthorization(String userId, OauthClient oauthClient, String code, long expiresAt) {
        this();
        this.userId = userId;
        this.oauthClient = oauthClient;
        this.code = code;
        this.expiresAt = expiresAt;
    }

    private OauthAuthorization() {
        //for hibernate
    }

    public OauthAuthorization(Map attributes, OauthClient oauthClient) {
        this((String) attributes.get("user_id"),  oauthClient, (String) attributes.get("code"), (Long) attributes.get("expires_at"));
        setIdIfAvailable(attributes);
    }

    public OauthDataSource.OauthAuthorizationDTO getDTO() {
        OauthDataSource.OauthAuthorizationDTO dto = new OauthDataSource.OauthAuthorizationDTO();
        dto.setId(getId());
        dto.setUserId(userId);
        dto.setOauthClientId(String.valueOf(oauthClient.getId()));
        dto.setCode(code);
        dto.setExpiresAt(expiresAt);
        return dto;
    }
}
