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

package com.thoughtworks.go.server.dao;

import com.thoughtworks.go.domain.AuthToken;
import com.thoughtworks.go.server.cache.GoCache;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static com.thoughtworks.go.helper.AuthTokenMother.authTokenWithName;
import static com.thoughtworks.go.helper.AuthTokenMother.authTokenWithNameForUser;
import static com.thoughtworks.go.server.dao.AuthTokenSqlMapDao.AUTH_TOKEN_CACHE_KEY;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class AuthTokenSqlMapDaoIntegrationTest {
    @Autowired
    private AuthTokenSqlMapDao authTokenSqlMapDao;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Autowired
    private GoCache goCache;
    private String username;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        username = "Bob";
    }

    @After
    public void teardown() throws Exception {
        authTokenSqlMapDao.deleteAll();
        dbHelper.onTearDown();
        goCache.clear();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() throws Exception {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithName(tokenName);

        authTokenSqlMapDao.save(authToken);

        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(savedAuthToken, is(authToken));
        assertThat(authTokenSqlMapDao.load(savedAuthToken.getId()), is(authToken));
    }

    @Test
    public void shouldReturnNullWhenNoAuthTokenFoundForTheSpecifiedName() {
        String tokenName = "auth-token-for-apis";
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertNull(savedAuthToken);
    }

    @Test
    public void shouldReturnAllTheAuthTokensBelongingToAUser() {
        String user1 = "Bob";
        String user2 = "John";

        String tokenName1 = "token1-created-by-Bob";
        String tokenName2 = "token2-created-by-Bob";
        String tokenName3 = "token2-created-by-John";

        authTokenSqlMapDao.save(authTokenWithNameForUser(tokenName1, user1));
        authTokenSqlMapDao.save(authTokenWithNameForUser(tokenName2, user1));
        authTokenSqlMapDao.save(authTokenWithNameForUser(tokenName3, user2));

        List<AuthToken> user1AuthTokens = authTokenSqlMapDao.findAllTokensForUser(user1);
        List<AuthToken> user2AuthTokens = authTokenSqlMapDao.findAllTokensForUser(user2);

        assertThat(user1AuthTokens, hasSize(2));
        assertThat(user2AuthTokens, hasSize(1));

        assertThat(user1AuthTokens.get(0).getName(), is(tokenName1));
        assertThat(user1AuthTokens.get(1).getName(), is(tokenName2));

        assertThat(user2AuthTokens.get(0).getName(), is(tokenName3));
    }

    @Test
    public void shouldNotPopulateCacheWhenNoAuthTokenFoundForTheSpecifiedTokenName() {
        String tokenName = "auth-token-for-apis";
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);

        assertNull(savedAuthToken);
        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, tokenName));
    }

    @Test
    public void shouldCacheAuthTokenValueUponFirstGet() {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithNameForUser(tokenName, username);

        authTokenSqlMapDao.save(authToken);
        String cacheKey = String.format("%s_%s", username, tokenName);

        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey));
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey), is(savedAuthToken));
    }

    @Test
    public void shouldRemoveAuthTokenFromCacheUponUpdate() {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithNameForUser(tokenName, username);

        authTokenSqlMapDao.save(authToken);

        String cacheKey = String.format("%s_%s", username, tokenName);

        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey));
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey), is(savedAuthToken));
        assertThat(goCache.get(AUTH_TOKEN_CACHE_KEY, savedAuthToken.getValue()), is(savedAuthToken));

        authTokenSqlMapDao.save(authToken);

        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey));
        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, savedAuthToken.getValue()));
    }
}
