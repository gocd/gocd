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
public class GadgetOauthAuthorizationCode extends OauthDomainEntity<GadgetDataSource.GadgetOauthAuthorizationCodeDTO> {
    private String userId;
    private long gadgetsOauthClientId;
    private String code;
    private Long expiresIn;

    public GadgetOauthAuthorizationCode() {
    }

    public GadgetOauthAuthorizationCode(String userId, GadgetOauthClient client, String code, Long expiresIn) {
        this(userId, client.getId(), code, expiresIn);
    }

    public GadgetOauthAuthorizationCode(Map attributes) {
        this((String)attributes.get("user_id"), (Long)attributes.get("gadgets_oauth_client_id"), (String)attributes.get("code"), (Long) attributes.get("expires_in"));
        setIdIfAvailable(attributes);
    }

    private GadgetOauthAuthorizationCode(String userId, long gadgetsOauthClientId, String code, Long expiresIn) {
        this.userId = userId;
        this.gadgetsOauthClientId = gadgetsOauthClientId;
        this.code = code;
        this.expiresIn = expiresIn;
    }

    @Override public GadgetDataSource.GadgetOauthAuthorizationCodeDTO getDTO() {
        return new GadgetDataSource.GadgetOauthAuthorizationCodeDTO(getId(), userId, gadgetsOauthClientId, code, expiresIn);
    }
}