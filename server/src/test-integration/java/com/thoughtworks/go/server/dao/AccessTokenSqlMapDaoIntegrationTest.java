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

import com.thoughtworks.go.domain.AccessToken;
import com.thoughtworks.go.util.Clock;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessToken;
import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessTokenForUser;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:testPropertyConfigurer.xml"
})
public class AccessTokenSqlMapDaoIntegrationTest {
    @Autowired
    private AccessTokenSqlMapDao accessTokenSqlMapDao;

    @Autowired
    private Clock clock;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() {
        AccessToken accessToken = randomAccessToken();

        accessTokenSqlMapDao.saveOrUpdate(accessToken);

        AccessToken savedAccessToken = accessTokenSqlMapDao.load(accessToken.getId());
        assertThat(savedAccessToken).isEqualTo(accessToken);
    }

    @Test
    public void shouldReturnNullWhenNoAccessTokenFound() {
        AccessToken savedAccessToken = accessTokenSqlMapDao.load(-1);
        assertThat(savedAccessToken).isNull();
    }

    @Test
    public void shouldReturnAllTheAccessTokensBelongingToAUser() {
        String user1 = "Bob";
        String user2 = "John";

        AccessToken token1 = randomAccessTokenForUser(user1);
        AccessToken token2 = randomAccessTokenForUser(user1);
        AccessToken token3 = randomAccessTokenForUser(user2);

        accessTokenSqlMapDao.saveOrUpdate(token1);
        accessTokenSqlMapDao.saveOrUpdate(token2);
        accessTokenSqlMapDao.saveOrUpdate(token3);

        List<AccessToken> user1AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user1);
        List<AccessToken> user2AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user2);

        assertThat(user1AccessTokens).hasSize(2).containsExactlyInAnyOrder(token1, token2);
        assertThat(user2AccessTokens).hasSize(1).containsExactlyInAnyOrder(token3);
    }

    @Test
    public void shouldLoadAccessTokenBasedOnSaltId() {
        AccessToken accessToken = randomAccessToken();

        accessTokenSqlMapDao.saveOrUpdate(accessToken);

        AccessToken savedAccessToken = accessTokenSqlMapDao.findAccessTokenBySaltId(accessToken.getSaltId());
        assertThat(savedAccessToken).isEqualTo(accessToken);
    }

    @Test
    public void shouldReturnNullWhenNoAccessTokenFoundForTheSpecifiedSaltId() {
        String saltId = "access-token-for-apis";
        AccessToken savedAccessToken = accessTokenSqlMapDao.findAccessTokenBySaltId(saltId);
        assertThat(savedAccessToken).isNull();
    }

    @Test
    public void shouldNotListDeletedTokens() {
        String user1 = "Bob";
        String user2 = "John";

        AccessToken token1 = randomAccessTokenForUser(user1);
        AccessToken token2 = randomAccessTokenForUser(user1);
        AccessToken token3 = randomAccessTokenForUser(user2);

        accessTokenSqlMapDao.saveOrUpdate(token1);
        accessTokenSqlMapDao.saveOrUpdate(token2);
        accessTokenSqlMapDao.saveOrUpdate(token3);

        List<AccessToken> user1AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user1);
        List<AccessToken> user2AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user2);

        assertThat(user1AccessTokens).hasSize(2).containsExactlyInAnyOrder(token1, token2);
        assertThat(user2AccessTokens).hasSize(1).containsExactlyInAnyOrder(token3);

        accessTokenSqlMapDao.revokeTokensBecauseOfUserDelete(Arrays.asList("bob", "john"), "admin");

        assertThat(accessTokenSqlMapDao.findAllTokensForUser("bob")).isEmpty();
        assertThat(accessTokenSqlMapDao.findAllTokensForUser("john")).isEmpty();
    }

    @Test
    public void shouldListAllTokens() {
        String user1 = "will-be-deleted";
        String user2 = "will-be-revoked";

        AccessToken token1 = randomAccessTokenForUser(user1);
        AccessToken token2 = randomAccessTokenForUser(user2);

        accessTokenSqlMapDao.saveOrUpdate(token1);
        accessTokenSqlMapDao.saveOrUpdate(token2);

        accessTokenSqlMapDao.saveOrUpdate(token1.revoke("admin", "user is making too many requests", clock.currentTimestamp()));
        accessTokenSqlMapDao.revokeTokensBecauseOfUserDelete(Collections.singletonList(user2), "admin");

        assertThat(accessTokenSqlMapDao.findAllTokens())
                .hasSize(2).containsExactlyInAnyOrder(accessTokenSqlMapDao.load(token1.getId()), accessTokenSqlMapDao.load(token2.getId()));
    }
}
