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

package com.thoughtworks.go.server.service;

import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.server.dao.AccessTokenDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AccessTokenServiceTest {
    @Mock
    private AccessTokenDao accessTokenDao;
    private AccessTokenService accessTokenService;
    private HttpLocalizedOperationResult result;
    private Username username;
    private String authConfigId;

    @BeforeEach
    void setUp() {
        initMocks(this);
        accessTokenService = new AccessTokenService(accessTokenDao);
        result = new HttpLocalizedOperationResult();

        username = new Username("Bob");
        authConfigId = "auth-config-1";
    }

    @Test
    void shouldValidateAccessTokenName() throws Exception {
        String invalidTokenName = "@#my_%_fancy_%_token#@";
        accessTokenService.create(invalidTokenName, null, username, authConfigId, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(result.message()).isEqualTo(String.format("Invalid access token name '%s'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.", invalidTokenName));

        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldValidateAccessTokenDescription() throws Exception {
        String tokenName = "token1";
        String longerDescription = RandomStringUtils.randomAlphanumeric(1025).toUpperCase();
        accessTokenService.create(tokenName, longerDescription, username, authConfigId, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(result.message()).isEqualTo("Validation Failed. Access token description can not be longer than 1024 characters.");

        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldMakeACallToSQLDaoForAccessTokenCreation() throws Exception {
        String tokenName = "token1";
        String longerDescription = RandomStringUtils.randomAlphanumeric(1024).toUpperCase();
        accessTokenService.create(tokenName, longerDescription, username, authConfigId, result);

        assertThat(result.isSuccessful()).isTrue();

        verify(accessTokenDao, times(1)).saveOrUpdate(any(AccessToken.class));
    }

    @Test
    void shouldMakeACallToSQLDaoForFetchingAccessToken() {
        String tokenName = "token1";
        accessTokenService.find(tokenName, username.getUsername().toString());

        verify(accessTokenDao, times(1)).findAccessToken(tokenName, username.getUsername().toString());
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldMakeACallToSQLDaoForFetchingAllAccessTokensBelongingToAUser() {
        accessTokenService.findAllTokensForUser(username);

        verify(accessTokenDao, times(1)).findAllTokensForUser(username.getUsername().toString());
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void shouldValidateExistenceOfAnotherAccessTokenWithTheSameName() throws Exception {
        String tokenName = "token1";
        String longerDescription = RandomStringUtils.randomAlphanumeric(1024).toUpperCase();

        when(accessTokenDao.findAccessToken(tokenName, username.getUsername().toString())).thenReturn(new AccessToken(tokenName, "value"));

        accessTokenService.create(tokenName, longerDescription, username, authConfigId, result);

        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(409);
        assertThat(result.message()).isEqualTo("Validation Failed. Another access token with name 'token1' already exists.");

        verify(accessTokenDao, times(1)).findAccessToken(tokenName, username.getUsername().toString());
        verifyNoMoreInteractions(accessTokenDao);
    }

    @Test
    void hashToken_shouldHashTheProvidedString() throws Exception {
        String tokenValue = "token1";
        String saltValue = "salt1";
        String hashed = accessTokenService.digestToken(tokenValue, saltValue);

        SecretKey key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(new PBEKeySpec(tokenValue.toCharArray(), saltValue.getBytes(), 4096, 256));

        assertThat(hashed).isEqualTo(Hex.encodeHexString(key.getEncoded()));
    }

    @Test
    void hashToken_shouldGenerateTheSameHashValueForTheSameInputString() throws Exception {
        String tokenValue = "new-token";
        String saltValue = "new-salt";
        String hashed1 = accessTokenService.digestToken(tokenValue, saltValue);
        String hashed2 = accessTokenService.digestToken(tokenValue, saltValue);

        assertThat(hashed1).isEqualTo(hashed2);
    }
}
