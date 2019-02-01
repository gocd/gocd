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
import com.thoughtworks.go.server.dao.AccessTokenSqlMapDao;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.exceptions.InvalidAccessTokenException;
import com.thoughtworks.go.server.exceptions.RevokedAccessTokenException;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.server.newsecurity.utils.SessionUtils.currentUsername;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class AccessTokenServiceIntegrationTest {
    @Autowired
    DatabaseAccessHelper dbHelper;

    @Autowired
    AccessTokenService accessTokenService;

    @Autowired
    AccessTokenSqlMapDao accessTokenSqlMapDao;
    private Username username;
    private String authConfigId;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        username = currentUsername();
        authConfigId = "auth-config-1";
    }

    @After
    public void tearDown() throws Exception {
        accessTokenSqlMapDao.deleteAll();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldCreateAnAccessToken() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        AccessToken createdToken = accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);

        AccessToken fetchedToken = accessTokenService.find(tokenName, username.getUsername().toString());

        assertThat(result.isSuccessful()).isTrue();
        assertThat(createdToken.getName()).isEqualTo(tokenName);
        assertThat(createdToken.getDescription()).isEqualTo(tokenDescription);
        assertThat(createdToken.getValue()).isNotNull();
        assertThat(createdToken.getOriginalValue()).isNotNull();
        assertThat(createdToken.getCreatedAt()).isNotNull();
        assertThat(createdToken.getLastUsed()).isNull();
        assertThat(createdToken.isRevoked()).isFalse();

        assertThat(fetchedToken.getValue()).isEqualTo(createdToken.getValue());
        assertThat(fetchedToken.getName()).isEqualTo(createdToken.getName());
        assertThat(fetchedToken.getDescription()).isEqualTo(createdToken.getDescription());
        assertThat(fetchedToken.getCreatedAt()).isEqualTo(createdToken.getCreatedAt());
        assertThat(fetchedToken.getLastUsed()).isEqualTo(createdToken.getLastUsed());
        assertThat(fetchedToken.isRevoked()).isEqualTo(createdToken.isRevoked());

        assertThat(fetchedToken.getOriginalValue()).isNull();
    }

    @Test
    public void shouldFailToCreateAccessTokenWhenOneWithTheSameNameAlreadyExists() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);
        assertThat(result.isSuccessful()).isTrue();

        AccessToken savedToken = accessTokenService.find(tokenName, username.getUsername().toString());
        assertThat(savedToken.getName()).isEqualTo(tokenName);

        accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.httpCode()).isEqualTo(409);
        assertThat(result.message()).isEqualTo("Validation Failed. Another access token with name 'token1' already exists.");
    }

    @Test
    public void shouldAllowDifferentUsersToCreateAccessTokenWhenWithSameName() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);
        assertThat(result.isSuccessful()).isTrue();

        AccessToken savedToken = accessTokenService.find(tokenName, username.getUsername().toString());
        assertThat(savedToken.getName()).isEqualTo(tokenName);

        accessTokenService.create(tokenName, tokenDescription, new Username("Another User"), authConfigId, result);

        assertThat(result.isSuccessful()).isTrue();
    }

    @Test
    public void shouldGetAccessTokenProvidedTokenValue() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first Token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        AccessToken createdToken = accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);
        String accessTokenInString = createdToken.getOriginalValue();
        createdToken.setOriginalValue(null);
        AccessToken fetchedToken = accessTokenService.findByAccessToken(accessTokenInString);

        assertThat(createdToken).isEqualTo(fetchedToken);
    }

    @Test
    public void shouldFailToGetAccessTokenWhenProvidedTokenLengthIsNotEqualTo40() {
        InvalidAccessTokenException exception = assertThrows(InvalidAccessTokenException.class, () -> accessTokenService.findByAccessToken("my-access-token"));
        assertThat("Invalid Personal Access Token.").isEqualTo(exception.getMessage());
    }

    @Test
    public void shouldFailToGetAccessTokenWhenProvidedTokenContainsInvalidSaltId() {
        String accessToken = RandomStringUtils.randomAlphanumeric(40);
        InvalidAccessTokenException exception = assertThrows(InvalidAccessTokenException.class, () -> accessTokenService.findByAccessToken(accessToken));
        assertThat("Invalid Personal Access Token.").isEqualTo(exception.getMessage());
    }

    @Test
    public void shouldFailToGetAccessTokenWhenProvidedTokenHashEqualityFails() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first Token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        AccessToken createdToken = accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);
        String accessTokenInString = createdToken.getOriginalValue();
        //replace last 5 characters to make the current token invalid
        String invalidAccessToken = StringUtils.replace(accessTokenInString, accessTokenInString.substring(35), "abcde");

        InvalidAccessTokenException exception = assertThrows(InvalidAccessTokenException.class, () -> accessTokenService.findByAccessToken(invalidAccessToken));
        assertThat("Invalid Personal Access Token.").isEqualTo(exception.getMessage());
    }

    @Test
    public void shouldNotGetAccessTokenProvidedTokenValueWhenTokenIsRevoked() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first Token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        AccessToken createdToken = accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, result);
        accessTokenService.revokeAccessToken(createdToken.getName(), currentUsername().getUsername().toString(), result);
        String accessTokenInString = createdToken.getOriginalValue();

        RevokedAccessTokenException exception = assertThrows(RevokedAccessTokenException.class, () -> accessTokenService.findByAccessToken(accessTokenInString));
        assertThat(exception.getMessage()).startsWith("Invalid Personal Access Token. Access token was revoked at: ");
    }

    @Test
    public void shouldRevokeAnAccessToken() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first Token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        AccessToken createdToken = accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, new HttpLocalizedOperationResult());

        assertThat(createdToken.isRevoked()).isFalse();

        accessTokenService.revokeAccessToken(tokenName, currentUsername().getUsername().toString(), result);
        assertThat(result.isSuccessful()).isTrue();

        AccessToken tokenAfterRevoking = accessTokenService.find(tokenName, currentUsername().getUsername().toString());
        assertThat(tokenAfterRevoking.isRevoked()).isTrue();
    }

    @Test
    public void shouldFailToRevokeAnAlreadyRevokedAccessToken() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first Token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        AccessToken createdToken = accessTokenService.create(tokenName, tokenDescription, currentUsername(), authConfigId, new HttpLocalizedOperationResult());

        assertThat(createdToken.isRevoked()).isFalse();

        accessTokenService.revokeAccessToken(tokenName, currentUsername().getUsername().toString(), result);
        assertThat(result.isSuccessful()).isTrue();

        AccessToken tokenAfterRevoking = accessTokenService.find(tokenName, currentUsername().getUsername().toString());
        assertThat(tokenAfterRevoking.isRevoked()).isTrue();

        accessTokenService.revokeAccessToken(tokenName, currentUsername().getUsername().toString(), result);
        assertThat(result.isSuccessful()).isFalse();

        assertThat(result.message()).isEqualTo(String.format("Validation Failed. Access Token with name '%s' for user '%s' has already been revoked.", tokenName, currentUsername().getUsername().toString()));
    }

    @Test
    public void shouldFailToRevokeNonExistingAccessToken() {
        String tokenName = "token1";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        accessTokenService.revokeAccessToken(tokenName, currentUsername().getUsername().toString(), result);
        assertThat(result.isSuccessful()).isFalse();
        assertThat(result.message()).isEqualTo(String.format("Validation Failed. Access Token with name '%s' for user '%s' does not exists.", tokenName, currentUsername().getUsername().toString()));
    }
}
