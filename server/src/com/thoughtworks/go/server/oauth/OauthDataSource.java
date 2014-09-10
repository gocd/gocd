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

package com.thoughtworks.go.server.oauth;

import java.util.Collection;
import java.util.Map;
import java.util.List;

public interface OauthDataSource {

    void transaction(Runnable txn);

    OauthClientDTO findOauthClientById(long id);

    OauthClientDTO findOauthClientByClientId(String clientId);

    OauthClientDTO findOauthClientByName(String name);

    OauthClientDTO findOauthClientByRedirectUri(String redirectUri);

    Collection<OauthClientDTO> findAllOauthClient();

    OauthClientDTO saveOauthClient(Map attributes);

    void deleteOauthClient(long id);

    Collection<OauthAuthorizationDTO> findAllOauthAuthorizationByOauthClientId(String oauthClientId);

    OauthAuthorizationDTO findOauthAuthorizationById(long id);

    OauthAuthorizationDTO findOauthAuthorizationByCode(String code);

    OauthAuthorizationDTO saveOauthAuthorization(Map attributes);

    void deleteOauthAuthorization(long id);

    OauthTokenDTO findOauthTokenById(long id);

    Collection<OauthTokenDTO> findAllOauthTokenByOauthClientId(String oauthClientId);

    Collection<OauthTokenDTO> findAllOauthTokenByUserId(String userId);

    OauthTokenDTO findOauthTokenByAccessToken(String accessToken);

    OauthTokenDTO findOauthTokenByRefreshToken(String refreshToken);

    OauthTokenDTO saveOauthToken(Map attributes);

    void deleteOauthToken(long id);

    void deleteUsersOauthGrants(List<String> userIds);


    class OauthClientDTO {
        private long id;
        private String name;
        private String clientId;
        private String clientSecret;
        private String redirectUri;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getClientId() {
            return clientId;
        }

        public void setClientId(String clientId) {
            this.clientId = clientId;
        }

        public String getClientSecret() {
            return clientSecret;
        }

        public void setClientSecret(String clientSecret) {
            this.clientSecret = clientSecret;
        }

        public String getRedirectUri() {
            return redirectUri;
        }

        public void setRedirectUri(String redirectUri) {
            this.redirectUri = redirectUri;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OauthClientDTO that = (OauthClientDTO) o;

            if (id != that.id) {
                return false;
            }
            if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) {
                return false;
            }
            if (clientSecret != null ? !clientSecret.equals(that.clientSecret) : that.clientSecret != null) {
                return false;
            }
            if (name != null ? !name.equals(that.name) : that.name != null) {
                return false;
            }
            if (redirectUri != null ? !redirectUri.equals(that.redirectUri) : that.redirectUri != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (name != null ? name.hashCode() : 0);
            result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
            result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
            result = 31 * result + (redirectUri != null ? redirectUri.hashCode() : 0);
            return result;
        }
    }

    class OauthAuthorizationDTO {
        private long id;
        private String userId;
        private String oauthClientId;
        private String code;
        private long expiresAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getOauthClientId() {
            return oauthClientId;
        }

        public void setOauthClientId(String oauthClientId) {
            this.oauthClientId = oauthClientId;
        }

        public void setClientId(String clientId) {
            this.oauthClientId = clientId;
        }

        public String getClientId() {
            return this.oauthClientId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OauthAuthorizationDTO that = (OauthAuthorizationDTO) o;

            if (expiresAt != that.expiresAt) {
                return false;
            }
            if (id != that.id) {
                return false;
            }
            if (code != null ? !code.equals(that.code) : that.code != null) {
                return false;
            }
            if (oauthClientId != null ? !oauthClientId.equals(that.oauthClientId) : that.oauthClientId != null) {
                return false;
            }
            if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            result = 31 * result + (oauthClientId != null ? oauthClientId.hashCode() : 0);
            result = 31 * result + (code != null ? code.hashCode() : 0);
            result = 31 * result + (int) (expiresAt ^ (expiresAt >>> 32));
            return result;
        }
    }

    class OauthTokenDTO {
        private long id;
        private String userId;
        private String oauthClientId;
        private String accessToken;
        private String refreshToken;
        private long expiresAt;

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getUserId() {
            return userId;
        }

        public void setUserId(String userId) {
            this.userId = userId;
        }

        public String getOauthClientId() {
            return oauthClientId;
        }

        public void setOauthClientId(String oauthClientId) {
            this.oauthClientId = oauthClientId;
        }

        public String getClientId() {
            return oauthClientId;
        }

        public void setClientId(String oauthClientId) {
            this.oauthClientId = oauthClientId;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public void setAccessToken(String accessToken) {
            this.accessToken = accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }

        public void setRefreshToken(String refreshToken) {
            this.refreshToken = refreshToken;
        }

        public long getExpiresAt() {
            return expiresAt;
        }

        public void setExpiresAt(long expiresAt) {
            this.expiresAt = expiresAt;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            OauthTokenDTO that = (OauthTokenDTO) o;

            if (expiresAt != that.expiresAt) {
                return false;
            }
            if (id != that.id) {
                return false;
            }
            if (accessToken != null ? !accessToken.equals(that.accessToken) : that.accessToken != null) {
                return false;
            }
            if (oauthClientId != null ? !oauthClientId.equals(that.oauthClientId) : that.oauthClientId != null) {
                return false;
            }
            if (refreshToken != null ? !refreshToken.equals(that.refreshToken) : that.refreshToken != null) {
                return false;
            }
            if (userId != null ? !userId.equals(that.userId) : that.userId != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (userId != null ? userId.hashCode() : 0);
            result = 31 * result + (oauthClientId != null ? oauthClientId.hashCode() : 0);
            result = 31 * result + (accessToken != null ? accessToken.hashCode() : 0);
            result = 31 * result + (refreshToken != null ? refreshToken.hashCode() : 0);
            result = 31 * result + (int) (expiresAt ^ (expiresAt >>> 32));
            return result;
        }
    }
}