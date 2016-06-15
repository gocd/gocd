/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.server.gadget;

import java.util.Collection;
import java.util.Map;

public interface GadgetDataSource {

    void transaction(Runnable txn);
    
    // find
    GadgetOauthClientDTO findGadgetsOauthClientById(long id);

    Collection<GadgetOauthClientDTO> findAllGadgetsOauthClient();

    Collection<GadgetOauthAccessTokenDTO> findAllOauthAccessTokenByUserId(String userId);

    Collection<GadgetOauthAuthorizationCodeDTO> findAllOauthAuthorizationCodeByUserId(String userId);

    GadgetOauthAccessTokenDTO findOauthAccessTokensForClientAndUserId(long gadgetsOauthClientId, String userId);

    Collection<GadgetOauthAccessTokenDTO> findAllOauthAccessTokenByGadgetsOauthClientId(long gadgetOauthClientId);

    Collection<GadgetOauthAuthorizationCodeDTO> findAllOauthAuthorizationCodeByGadgetsOauthClientId(long gadgetOauthClientId);

    GadgetOauthAuthorizationCodeDTO findAuthorizationCodesForClientAndUserId(long gadgetOauthClientId, String userId);

    GadgetOauthClientDTO findGadgetsOauthClientByServiceName(String serviceName);

    GadgetOauthClientDTO findGadgetsOauthClientByOauthAuthorizeUrl(String oauthAuthorizeUrl);

    // delete
    void deleteGadgetsOauthClient(long id);

    void deleteOauthAuthorizationCode(long id);

    void deleteOauthAccessToken(long id);

    // save
    GadgetOauthClientDTO saveGadgetsOauthClient(Map attributes);

    GadgetOauthAuthorizationCodeDTO saveOauthAuthorizationCode(Map attributes);

    GadgetOauthAccessTokenDTO saveOauthAccessToken(Map attributes);

    class GadgetOauthClientDTO {
        private long id;
        private String oauthAuthorizeUrl;
        private String serviceName;
        private String clientId;
        private String clientSecret;

        public GadgetOauthClientDTO(String oauthAuthorizeUrl, String serviceName, String clientId, String clientSecret) {
            this.oauthAuthorizeUrl = oauthAuthorizeUrl;
            this.serviceName = serviceName;
            this.clientId = clientId;
            this.clientSecret = clientSecret;
        }

        public long getId() {
            return id;
        }

        public void setId(long id) {
            this.id = id;
        }

        public String getOauthAuthorizeUrl() {
            return oauthAuthorizeUrl;
        }

        public void setOauthAuthorizeUrl(String oauthAuthorizeUrl) {
            this.oauthAuthorizeUrl = oauthAuthorizeUrl;
        }

        public String getServiceName() {
            return serviceName;
        }

        public void setServiceName(String serviceName) {
            this.serviceName = serviceName;
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

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GadgetOauthClientDTO that = (GadgetOauthClientDTO) o;

            if (id != that.id) {
                return false;
            }
            if (clientId != null ? !clientId.equals(that.clientId) : that.clientId != null) {
                return false;
            }
            if (clientSecret != null ? !clientSecret.equals(that.clientSecret) : that.clientSecret != null) {
                return false;
            }
            if (oauthAuthorizeUrl != null ? !oauthAuthorizeUrl.equals(that.oauthAuthorizeUrl) : that.oauthAuthorizeUrl != null) {
                return false;
            }
            if (serviceName != null ? !serviceName.equals(that.serviceName) : that.serviceName != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = (int) (id ^ (id >>> 32));
            result = 31 * result + (oauthAuthorizeUrl != null ? oauthAuthorizeUrl.hashCode() : 0);
            result = 31 * result + (serviceName != null ? serviceName.hashCode() : 0);
            result = 31 * result + (clientId != null ? clientId.hashCode() : 0);
            result = 31 * result + (clientSecret != null ? clientSecret.hashCode() : 0);
            return result;
        }
    }

    class GadgetOauthAuthorizationCodeDTO {
        private long id;
        private String userId;
        private long gadgetsOauthClientId;
        private String code;
        private Long expiresIn;

        public GadgetOauthAuthorizationCodeDTO(long id, String userId, long gadgetOauthClientId, String code, Long expiresIn) {
            this.id = id;
            this.userId = userId;
            this.gadgetsOauthClientId = gadgetOauthClientId;
            this.code = code;
            this.expiresIn = expiresIn;
        }

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

        public long getGadgetsOauthClientId() {
            return gadgetsOauthClientId;
        }

        public void setGadgetsOauthClientId(long gadgetsOauthClientId) {
            this.gadgetsOauthClientId = gadgetsOauthClientId;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(Long expiresIn) {
            this.expiresIn = expiresIn;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GadgetOauthAuthorizationCodeDTO that = (GadgetOauthAuthorizationCodeDTO) o;

            if (gadgetsOauthClientId != that.gadgetsOauthClientId) {
                return false;
            }
            if (id != that.id) {
                return false;
            }
            if (code != null ? !code.equals(that.code) : that.code != null) {
                return false;
            }
            if (expiresIn != null ? !expiresIn.equals(that.expiresIn) : that.expiresIn != null) {
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
            result = 31 * result + (int) (gadgetsOauthClientId ^ (gadgetsOauthClientId >>> 32));
            result = 31 * result + (code != null ? code.hashCode() : 0);
            result = 31 * result + (expiresIn != null ? expiresIn.hashCode() : 0);
            return result;
        }
    }

    class GadgetOauthAccessTokenDTO {
        private long id;
        private String userId;
        private long gadgetsOauthClientId;
        private String accessToken;
        private String refreshToken;
        private Long expiresIn;

        public GadgetOauthAccessTokenDTO(long id, String userId, long gadgetOauthClientId, String accessToken, String refreshToken, Long expiresIn) {
            this.id = id;
            this.userId = userId;
            this.gadgetsOauthClientId = gadgetOauthClientId;
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
            this.expiresIn = expiresIn;
        }

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

        public long getGadgetsOauthClientId() {
            return gadgetsOauthClientId;
        }

        public void setGadgetsOauthClientId(long gadgetsOauthClientId) {
            this.gadgetsOauthClientId = gadgetsOauthClientId;
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

        public Long getExpiresIn() {
            return expiresIn;
        }

        public void setExpiresIn(long expiresIn) {
            this.expiresIn = expiresIn;
        }


        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            GadgetOauthAccessTokenDTO that = (GadgetOauthAccessTokenDTO) o;

            if (gadgetsOauthClientId != that.gadgetsOauthClientId) {
                return false;
            }
            if (id != that.id) {
                return false;
            }
            if (accessToken != null ? !accessToken.equals(that.accessToken) : that.accessToken != null) {
                return false;
            }
            if (expiresIn != null ? !expiresIn.equals(that.expiresIn) : that.expiresIn != null) {
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
            result = 31 * result + (int) (gadgetsOauthClientId ^ (gadgetsOauthClientId >>> 32));
            result = 31 * result + (accessToken != null ? accessToken.hashCode() : 0);
            result = 31 * result + (refreshToken != null ? refreshToken.hashCode() : 0);
            result = 31 * result + (expiresIn != null ? expiresIn.hashCode() : 0);
            return result;
        }
    }
}
