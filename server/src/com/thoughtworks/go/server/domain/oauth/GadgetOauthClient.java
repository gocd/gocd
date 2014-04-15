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
public class GadgetOauthClient extends OauthDomainEntity<GadgetDataSource.GadgetOauthClientDTO> {
    private String oauthAuthorizeUrl;
    private String serviceName;
    private String clientId;
    private String clientSecret;

    public GadgetOauthClient() {
    }

    public GadgetOauthClient(String oauthAuthorizeUrl, String serviceName, String clientId, String clientSecret) {
        this.oauthAuthorizeUrl = oauthAuthorizeUrl;
        this.serviceName = serviceName;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public void setAttributes(Map attributes) {
        this.oauthAuthorizeUrl = (String) attributes.get("oauth_authorize_url");
        this.serviceName = (String) attributes.get("service_name");
        this.clientId = (String) attributes.get("client_id");
        this.clientSecret = (String) attributes.get("client_secret");
        setIdIfAvailable(attributes);
    }

    @Override public GadgetDataSource.GadgetOauthClientDTO getDTO() {
        GadgetDataSource.GadgetOauthClientDTO dto = new GadgetDataSource.GadgetOauthClientDTO(oauthAuthorizeUrl, serviceName, clientId, clientSecret);
        dto.setId(getId());
        return dto;
    }

}