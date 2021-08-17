/*
 * Copyright 2021 ThoughtWorks, Inc.
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
import com.thoughtworks.go.server.service.AccessTokenFilter;
import com.thoughtworks.go.util.Clock;
import org.apache.commons.lang3.RandomStringUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessToken;
import static com.thoughtworks.go.helper.AccessTokenMother.randomAccessTokenForUser;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class AccessTokenSqlMapDaoIntegrationTest {
    @Autowired
    private AccessTokenSqlMapDao accessTokenSqlMapDao;

    @Autowired
    private Clock clock;

    @Autowired
    private DatabaseAccessHelper dbHelper;

    @BeforeEach
    public void setup() throws Exception {
        dbHelper.onSetUp();
    }

    @AfterEach
    public void teardown() throws Exception {
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() {
        AccessToken accessToken = randomAccessToken();

        accessTokenSqlMapDao.saveOrUpdate(accessToken);

        AccessToken savedAccessToken = accessTokenSqlMapDao.loadForAdminUser(accessToken.getId());
        assertThat(savedAccessToken).isEqualTo(accessToken);
    }

    @Test
    public void shouldReturnNullWhenNoAccessTokenFound() {
        AccessToken savedAccessToken = accessTokenSqlMapDao.loadForAdminUser(-1);
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

        List<AccessToken> user1AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user1, AccessTokenFilter.all);
        List<AccessToken> user2AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user2, AccessTokenFilter.all);

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

        List<AccessToken> user1AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user1, AccessTokenFilter.all);
        List<AccessToken> user2AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user2, AccessTokenFilter.all);

        assertThat(user1AccessTokens).hasSize(2).containsExactlyInAnyOrder(token1, token2);
        assertThat(user2AccessTokens).hasSize(1).containsExactlyInAnyOrder(token3);

        accessTokenSqlMapDao.revokeTokensBecauseOfUserDelete(Arrays.asList("bob", "john"), "admin");

        assertThat(accessTokenSqlMapDao.findAllTokensForUser("bob", AccessTokenFilter.all)).isEmpty();
        assertThat(accessTokenSqlMapDao.findAllTokensForUser("john", AccessTokenFilter.all)).isEmpty();
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

        assertThat(accessTokenSqlMapDao.findAllTokens(AccessTokenFilter.all))
                .hasSize(2)
                .containsExactlyInAnyOrder(accessTokenSqlMapDao.loadForAdminUser(token1.getId()), accessTokenSqlMapDao.loadForAdminUser(token2.getId()));
    }

    @Test
    public void shouldListAllTokensThatAreRevoked() {
        String user1 = "will-be-deleted";
        String user2 = "will-be-revoked";

        AccessToken token1 = randomAccessTokenForUser(user1);
        AccessToken token2 = randomAccessTokenForUser(user2);

        accessTokenSqlMapDao.saveOrUpdate(token1);
        accessTokenSqlMapDao.saveOrUpdate(token2);

        accessTokenSqlMapDao.saveOrUpdate(token1.revoke("admin", "user is making too many requests", clock.currentTimestamp()));
        accessTokenSqlMapDao.revokeTokensBecauseOfUserDelete(Collections.singletonList(user2), "admin");

        assertThat(accessTokenSqlMapDao.findAllTokens(AccessTokenFilter.revoked))
                .hasSize(2)
                .containsExactlyInAnyOrder(accessTokenSqlMapDao.loadForAdminUser(token1.getId()), accessTokenSqlMapDao.loadForAdminUser(token2.getId()));
    }

    @Test
    public void shouldListAllTokensThatAreActive() {
        String user1 = "bob";
        String user2 = "will-be-revoked";

        AccessToken token1 = randomAccessTokenForUser(user1);
        AccessToken token2 = randomAccessTokenForUser(user2);

        accessTokenSqlMapDao.saveOrUpdate(token1);
        accessTokenSqlMapDao.saveOrUpdate(token2);

        accessTokenSqlMapDao.saveOrUpdate(token2.revoke("admin", "user is making too many requests", clock.currentTimestamp()));

        assertThat(accessTokenSqlMapDao.findAllTokens(AccessTokenFilter.active))
                .hasSize(1)
                .containsExactlyInAnyOrder(accessTokenSqlMapDao.loadForAdminUser(token1.getId()));
    }

    @Test
    public void adminsShouldBeAbleToLoadTokensRevokedByAnyone() {
        String user = RandomStringUtils.random(32);

        AccessToken token = randomAccessTokenForUser(user);

        accessTokenSqlMapDao.saveOrUpdate(token);
        accessTokenSqlMapDao.saveOrUpdate(token.revoke("admin", "user is making too many requests", clock.currentTimestamp()));

        assertThat(accessTokenSqlMapDao.loadForAdminUser(token.getId())).isEqualTo(token);
    }

    @Test
    public void adminsShouldBeAbleToLoadTokensRevokedBecauseOfUserDeletionButUsersCannot() {
        String user = RandomStringUtils.random(32);

        AccessToken token = randomAccessTokenForUser(user);

        accessTokenSqlMapDao.saveOrUpdate(token);
        accessTokenSqlMapDao.saveOrUpdate(token.revokeBecauseOfUserDelete("admin", clock.currentTimestamp()));

        assertThat(accessTokenSqlMapDao.loadForAdminUser(token.getId())).isEqualTo(token);
        assertThat(accessTokenSqlMapDao.loadNotDeletedTokenForUser(token.getId(), user)).isNull();
    }

    @Test
    public void shouldUpdateLastUsedTimeForGivenTokens() {
        AccessToken token1 = randomAccessTokenForUser("bob");
        accessTokenSqlMapDao.saveOrUpdate(token1);
        AccessToken token2 = randomAccessTokenForUser("bob");
        accessTokenSqlMapDao.saveOrUpdate(token2);
        assertThat(token1.getLastUsed()).isNull();
        assertThat(token2.getLastUsed()).isNull();

        final DateTime now = DateTime.now();
        final Timestamp lastUsedTimeForToken1 = new Timestamp(now.getMillis());
        now.plusHours(2);
        final Timestamp lastUsedTimeForToken2 = new Timestamp(now.getMillis());

        final HashMap<Long, Timestamp> accessTokenIdToLastUsedTimestamp = new HashMap<>();
        accessTokenIdToLastUsedTimestamp.put(token1.getId(), lastUsedTimeForToken1);
        accessTokenIdToLastUsedTimestamp.put(token2.getId(), lastUsedTimeForToken2);

        accessTokenSqlMapDao.updateLastUsedTime(accessTokenIdToLastUsedTimestamp);

        final List<AccessToken> allTokensForUser = accessTokenSqlMapDao.findAllTokensForUser("bob", AccessTokenFilter.all);
        assertThat(allTokensForUser).hasSize(2);
        assertThat(allTokensForUser.get(0).getLastUsed())
                .isEqualTo(lastUsedTimeForToken1);
        assertThat(allTokensForUser.get(1).getLastUsed())
                .isEqualTo(lastUsedTimeForToken2);
    }
}
