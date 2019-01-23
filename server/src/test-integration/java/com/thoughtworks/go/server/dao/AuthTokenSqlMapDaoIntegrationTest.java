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
import com.thoughtworks.go.helper.AuthTokenMother;
import com.thoughtworks.go.server.cache.GoCache;
import org.apache.commons.lang.RandomStringUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static com.thoughtworks.go.helper.AuthTokenMother.authTokenWithName;
import static com.thoughtworks.go.helper.AuthTokenMother.authTokenWithNameForUser;
import static com.thoughtworks.go.server.dao.AuthTokenSqlMapDao.AUTH_TOKEN_CACHE_KEY;
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

        authTokenSqlMapDao.saveOrUpdate(authToken);

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

        authTokenSqlMapDao.saveOrUpdate(authToken);
        String cacheKey = String.format("%s_%s", username, tokenName);

        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey));
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey), is(savedAuthToken));
    }

    @Test
    public void shouldRemoveAuthTokenFromCacheUponUpdate() {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithNameForUser(tokenName, username);

        authTokenSqlMapDao.saveOrUpdate(authToken);

        String cacheKey = String.format("%s_%s", username, tokenName);

        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey));
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey), is(savedAuthToken));
        assertThat(goCache.get(AUTH_TOKEN_CACHE_KEY, savedAuthToken.getValue()), is(savedAuthToken));

        authTokenSqlMapDao.saveOrUpdate(authToken);

        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, cacheKey));
        assertNull(goCache.get(AUTH_TOKEN_CACHE_KEY, savedAuthToken.getValue()));
    }
}
