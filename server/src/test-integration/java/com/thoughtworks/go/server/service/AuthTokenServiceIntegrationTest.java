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
import com.thoughtworks.go.server.dao.AuthTokenSqlMapDao;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static com.thoughtworks.go.server.newsecurity.utils.SessionUtils.currentUsername;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class AuthTokenServiceIntegrationTest {
    @Autowired
    DatabaseAccessHelper dbHelper;

    @Autowired
    AuthTokenService authTokenService;

    @Autowired
    AuthTokenSqlMapDao authTokenSqlMapDao;
    private Username username;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        username = currentUsername();
    }

    @After
    public void tearDown() throws Exception {
        authTokenSqlMapDao.deleteAll();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldCreateAnAuthToken() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        AuthToken createdToken = authTokenService.create(tokenName, tokenDescription, currentUsername(), result);

        AuthToken fetchedToken = authTokenService.find(tokenName, username);

        assertTrue(result.isSuccessful());
        assertThat(createdToken.getName(), is(tokenName));
        assertThat(createdToken.getDescription(), is(tokenDescription));
        assertNotNull(createdToken.getValue());
        assertNotNull(createdToken.getOriginalValue());
        assertNotNull(createdToken.getCreatedAt());
        assertNull(createdToken.getLastUsed());
        assertFalse(createdToken.isRevoked());

        assertThat(fetchedToken.getValue(), is(createdToken.getValue()));
        assertThat(fetchedToken.getName(), is(createdToken.getName()));
        assertThat(fetchedToken.getDescription(), is(createdToken.getDescription()));
        assertThat(fetchedToken.getCreatedAt(), is(createdToken.getCreatedAt()));
        assertThat(fetchedToken.getLastUsed(), is(createdToken.getLastUsed()));
        assertThat(fetchedToken.isRevoked(), is(createdToken.isRevoked()));

        assertNull(fetchedToken.getOriginalValue());
    }

    @Test
    public void shouldFailToCreateAuthTokenWhenOneWithTheSameNameAlreadyExists() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        authTokenService.create(tokenName, tokenDescription, currentUsername(), result);
        assertTrue(result.isSuccessful());

        AuthToken savedToken = authTokenService.find(tokenName, username);
        assertThat(savedToken.getName(), is(tokenName));

        authTokenService.create(tokenName, tokenDescription, currentUsername(), result);
        assertFalse(result.isSuccessful());
        Assert.assertThat(result.httpCode(), is(409));
        Assert.assertThat(result.message(), is("Validation Failed. Another auth token with name 'token1' already exists."));
    }

    @Test
    public void shouldAllowDifferentUsersToCreateAuthTokenWhenWithSameName() throws Exception {
        String tokenName = "token1";
        String tokenDescription = "This is my first token";
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();

        authTokenService.create(tokenName, tokenDescription, currentUsername(), result);
        assertTrue(result.isSuccessful());

        AuthToken savedToken = authTokenService.find(tokenName, username);
        assertThat(savedToken.getName(), is(tokenName));

        authTokenService.create(tokenName, tokenDescription, new Username("Another User"), result);

        assertTrue(result.isSuccessful());
    }
}
