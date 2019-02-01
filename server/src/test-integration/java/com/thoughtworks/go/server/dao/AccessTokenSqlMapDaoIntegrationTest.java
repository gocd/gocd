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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.List;

import static com.thoughtworks.go.helper.AccessTokenMother.accessTokenWithName;
import static com.thoughtworks.go.helper.AccessTokenMother.accessTokenWithNameForUser;
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
    private DatabaseAccessHelper dbHelper;

    private String username;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        username = "Bob";
    }

    @After
    public void teardown() throws Exception {
        accessTokenSqlMapDao.deleteAll();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() {
        String tokenName = "access-token-for-apis";
        AccessToken accessToken = accessTokenWithName(tokenName);

        accessTokenSqlMapDao.saveOrUpdate(accessToken);

        AccessToken savedAccessToken = accessTokenSqlMapDao.findAccessToken(tokenName, username);
        assertThat(savedAccessToken).isEqualTo(accessToken);
        assertThat(accessTokenSqlMapDao.load(savedAccessToken.getId())).isEqualTo(accessToken);
    }

    @Test
    public void shouldReturnNullWhenNoAccessTokenFoundForTheSpecifiedName() {
        String tokenName = "access-token-for-apis";
        AccessToken savedAccessToken = accessTokenSqlMapDao.findAccessToken(tokenName, username);
        assertThat(savedAccessToken).isNull();
    }

    @Test
    public void shouldReturnAllTheAccessTokensBelongingToAUser() {
        String user1 = "Bob";
        String user2 = "John";

        String tokenName1 = "token1-created-by-Bob";
        String tokenName2 = "token2-created-by-Bob";
        String tokenName3 = "token2-created-by-John";

        accessTokenSqlMapDao.saveOrUpdate(accessTokenWithNameForUser(tokenName1, user1));
        accessTokenSqlMapDao.saveOrUpdate(accessTokenWithNameForUser(tokenName2, user1));
        accessTokenSqlMapDao.saveOrUpdate(accessTokenWithNameForUser(tokenName3, user2));

        List<AccessToken> user1AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user1);
        List<AccessToken> user2AccessTokens = accessTokenSqlMapDao.findAllTokensForUser(user2);

        assertThat(user1AccessTokens).hasSize(2);
        assertThat(user2AccessTokens).hasSize(1);

        assertThat(user1AccessTokens.get(0).getName()).isEqualTo(tokenName1);
        assertThat(user1AccessTokens.get(1).getName()).isEqualTo(tokenName2);

        assertThat(user2AccessTokens.get(0).getName()).isEqualTo(tokenName3);
    }

    @Test
    public void shouldLoadAccessTokenBasedOnSaltId() {
        String tokenName = "access-token-for-apis";
        AccessToken accessToken = accessTokenWithName(tokenName);

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
}
