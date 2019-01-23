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

import com.thoughtworks.go.domain.AuthToken;
import com.thoughtworks.go.server.dao.AuthTokenDao;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.codec.binary.Hex;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.security.MessageDigest;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

class AuthTokenServiceTest {
    @Mock
    private AuthTokenDao authTokenDao;
    private AuthTokenService authTokenService;
    private HttpLocalizedOperationResult result;
    private Username username;

    @BeforeEach
    void setUp() {
        initMocks(this);
        authTokenService = new AuthTokenService(authTokenDao);
        result = new HttpLocalizedOperationResult();

        username = new Username("Bob");
    }

    @Test
    void shouldValidateAuthTokenName() throws Exception {
        String invalidTokenName = "@#my_%_fancy_%_token#@";
        authTokenService.create(invalidTokenName, null, username, result);

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(422));
        assertThat(result.message(), is(String.format("Invalid auth token name '%s'. This must be alphanumeric and can contain underscores and periods (however, it cannot start with a period). The maximum allowed length is 255 characters.", invalidTokenName)));

        verifyNoMoreInteractions(authTokenDao);
    }

    @Test
    void shouldValidateAuthTokenDescription() throws Exception {
        String tokenName = "token1";
        String longerDescription = RandomStringUtils.randomAlphanumeric(1025).toUpperCase();
        authTokenService.create(tokenName, longerDescription, username, result);

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(422));
        assertThat(result.message(), is("Validation Failed. Auth token description can not be longer than 1024 characters."));

        verifyNoMoreInteractions(authTokenDao);
    }

    @Test
    void shouldMakeACallToSQLDaoForAuthTokenCreation() throws Exception {
        String tokenName = "token1";
        String longerDescription = RandomStringUtils.randomAlphanumeric(1024).toUpperCase();
        authTokenService.create(tokenName, longerDescription, username, result);

        assertTrue(result.isSuccessful());

        verify(authTokenDao, times(1)).saveOrUpdate(any(AuthToken.class));
    }

    @Test
    void shouldValidateExistenceOfAnotherAuthTokenWithTheSameName() throws Exception {
        String tokenName = "token1";
        String longerDescription = RandomStringUtils.randomAlphanumeric(1024).toUpperCase();

        when(authTokenDao.findAuthToken(tokenName, username.getUsername().toString())).thenReturn(new AuthToken(tokenName, "value"));

        authTokenService.create(tokenName, longerDescription, username, result);

        assertFalse(result.isSuccessful());
        assertThat(result.httpCode(), is(409));
        assertThat(result.message(), is("Validation Failed. Another auth token with name 'token1' already exists."));

        verify(authTokenDao, times(1)).findAuthToken(tokenName, username.getUsername().toString());
        verifyNoMoreInteractions(authTokenDao);
    }

    @Test
    void hashToken_shouldHashTheProvidedString() throws Exception {
        String tokenValue = "token1";
        String hashed = authTokenService.hashToken(tokenValue);

        String expectedHash = new String(Hex.encodeHex(MessageDigest.getInstance("SHA-256").digest(tokenValue.getBytes())));
        assertThat(hashed, is(expectedHash));
    }

    @Test
    void hashToken_shouldGenerateTheSameHashValueForTheSameInputString() throws Exception {
        String tokenValue = "new-token";
        String hashed1 = authTokenService.hashToken(tokenValue);
        String hashed2 = authTokenService.hashToken(tokenValue);

        String expectedHash = new String(Hex.encodeHex(MessageDigest.getInstance("SHA-256").digest(tokenValue.getBytes())));
        assertThat(hashed1, is(expectedHash));
        assertThat(hashed1, is(hashed2));
    }
}
