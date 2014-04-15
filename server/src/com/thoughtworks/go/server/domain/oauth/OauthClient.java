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
import java.util.Set;

import com.thoughtworks.go.server.oauth.OauthDataSource;

/**
 * @understands client entries that are allowed to access service as oauth endpoint
 */
public class OauthClient extends OauthDomainEntity<OauthDataSource.OauthClientDTO> {
    private String name;
    private String clientId;
    private String clientSecret;
    private String redirectUri;
    private Set<OauthAuthorization> authorizations;
    private Set<OauthToken> tokens;

    public Set<OauthToken> getTokens() {
        return tokens;
    }

    public void setTokens(Set<OauthToken> tokens) {
        this.tokens = tokens;
    }

    public Set<OauthAuthorization> getAuthorizations() {
        return authorizations;
    }

    public void setAuthorizations(Set<OauthAuthorization> authorizations) {
        this.authorizations = authorizations;
    }

    public OauthClient(String name, String clientId, String clientSecret, String redirectUri) {
        this();
        this.name = name;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    public OauthClient() {
    }

    public void setAttributes(Map attributes) {
        this.name = (String) attributes.get("name");
        this.clientId = (String) attributes.get("client_id");
        this.clientSecret = (String) attributes.get("client_secret");
        this.redirectUri = (String) attributes.get("redirect_uri");
        setIdIfAvailable(attributes);
    }

    public OauthDataSource.OauthClientDTO getDTO() {
        OauthDataSource.OauthClientDTO dto = new OauthDataSource.OauthClientDTO();
        dto.setId(getId());
        dto.setName(name);
        dto.setClientId(clientId);
        dto.setClientSecret(clientSecret);
        dto.setRedirectUri(redirectUri);
        return dto;
    }
}
