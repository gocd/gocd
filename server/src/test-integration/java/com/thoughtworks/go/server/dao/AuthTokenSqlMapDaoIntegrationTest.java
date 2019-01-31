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
import static org.assertj.core.api.Assertions.assertThat;

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
    }

    @Test
    public void shouldSaveUsersIntoDatabase() {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithName(tokenName);

        authTokenSqlMapDao.saveOrUpdate(authToken);

        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(savedAuthToken).isEqualTo(authToken);
        assertThat(authTokenSqlMapDao.load(savedAuthToken.getId())).isEqualTo(authToken);
    }

    @Test
    public void shouldReturnNullWhenNoAuthTokenFoundForTheSpecifiedName() {
        String tokenName = "auth-token-for-apis";
        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName, username);
        assertThat(savedAuthToken).isNull();
    }

    @Test
    public void shouldReturnAllTheAuthTokensBelongingToAUser() {
        String user1 = "Bob";
        String user2 = "John";

        String tokenName1 = "token1-created-by-Bob";
        String tokenName2 = "token2-created-by-Bob";
        String tokenName3 = "token2-created-by-John";

        authTokenSqlMapDao.saveOrUpdate(authTokenWithNameForUser(tokenName1, user1));
        authTokenSqlMapDao.saveOrUpdate(authTokenWithNameForUser(tokenName2, user1));
        authTokenSqlMapDao.saveOrUpdate(authTokenWithNameForUser(tokenName3, user2));

        List<AuthToken> user1AuthTokens = authTokenSqlMapDao.findAllTokensForUser(user1);
        List<AuthToken> user2AuthTokens = authTokenSqlMapDao.findAllTokensForUser(user2);

        assertThat(user1AuthTokens).hasSize(2);
        assertThat(user2AuthTokens).hasSize(1);

        assertThat(user1AuthTokens.get(0).getName()).isEqualTo(tokenName1);
        assertThat(user1AuthTokens.get(1).getName()).isEqualTo(tokenName2);

        assertThat(user2AuthTokens.get(0).getName()).isEqualTo(tokenName3);
    }

    @Test
    public void shouldLoadAccessTokenBasedOnSaltId() {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithName(tokenName);

        authTokenSqlMapDao.saveOrUpdate(authToken);

        AuthToken savedAuthToken = authTokenSqlMapDao.findTokenBySaltId(authToken.getSaltId());
        assertThat(savedAuthToken).isEqualTo(authToken);
    }

    @Test
    public void shouldReturnNullWhenNoAuthTokenFoundForTheSpecifiedSaltId() {
        String saltId = "auth-token-for-apis";
        AuthToken savedAuthToken = authTokenSqlMapDao.findTokenBySaltId(saltId);
        assertThat(savedAuthToken).isNull();
    }
}
