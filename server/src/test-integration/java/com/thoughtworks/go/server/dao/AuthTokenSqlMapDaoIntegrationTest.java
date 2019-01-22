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
import com.thoughtworks.go.domain.User;
import org.apache.commons.lang.RandomStringUtils;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Date;

import static org.hamcrest.Matchers.is;
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
    private SessionFactory sessionFactory;

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        authTokenSqlMapDao.deleteAll();
    }

    @After
    public void teardown() throws Exception {
        authTokenSqlMapDao.deleteAll();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldSaveUsersIntoDatabase() throws Exception {
        String tokenName = "auth-token-for-apis";
        AuthToken authToken = authTokenWithName(tokenName);

        authTokenSqlMapDao.saveOrUpdate(authToken);

        AuthToken savedAuthToken = authTokenSqlMapDao.findAuthToken(tokenName);
        assertThat(savedAuthToken, is(authToken));
        assertThat(authTokenSqlMapDao.load(savedAuthToken.getId()), is(authToken));
    }


    private AuthToken authTokenWithName(String tokenName) {
        String tokenValue = RandomStringUtils.randomAlphanumeric(32).toUpperCase();
        String tokenDescription = RandomStringUtils.randomAlphanumeric(512).toUpperCase();
        Boolean isRevoked = false;
        Date createdAt = new Date();
        Date lastUsed = null;

        return new AuthToken(tokenName, tokenValue, tokenDescription, isRevoked, createdAt, lastUsed);
    }
}
