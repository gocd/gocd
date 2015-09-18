/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.cache;

import java.io.IOException;
import java.util.Arrays;

import com.ibatis.sqlmap.client.SqlMapClient;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.UserSqlMapDao;
import com.thoughtworks.go.server.database.DatabaseStrategy;
import com.thoughtworks.go.server.transaction.SqlMapClientDaoSupport;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.LogFixture;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoCacheTest {
    @Autowired private GoCache goCache;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private SqlMapClient sqlMapClient;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private UserSqlMapDao userSqlMapDao;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private DatabaseStrategy databaseStrategy;

    private static String largeObject;
    private LogFixture logFixture;
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();

    @Before
    public void setUp() throws Exception {
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        dbHelper.onSetUp();
        goCache.clear();
        logFixture = LogFixture.startListening();
    }

    @After
    public void tearDown() throws Exception {
        goCache.clear();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldAllowAddingUnpersistedNullObjects() {
        NullUser user = new NullUser();
        goCache.put("loser_user", user);
        assertThat((NullUser) goCache.get("loser_user"), is(user));
        String allLogs = logFixture.allLogs();
        assertThat(allLogs, not(containsString("added to cache without an id.")));
        assertThat(allLogs, not(containsString("without an id served out of cache.")));
    }

    @Test
    public void shouldBeAbleToGetAnObjectThatIsPutIntoIt() {
        Object o = new Object();
        goCache.put("someKey", o);
        assertSame(o, goCache.get("someKey"));
    }

    @Test
    public void put_shouldNotUpdateCacheWhenInTransaction() {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                Object o = new Object();
                goCache.put("someKey", o);
            }
        });
        assertNull(goCache.get("someKey"));
    }

    @Test
    public void shouldBeAbleToRemoveAnObjectThatIsPutIntoIt() {
        Object o = new Object();
        goCache.put("someKey", o);
        assertNotNull(goCache.get("someKey"));
        assertTrue(goCache.remove("someKey"));
        assertNull(goCache.get("someKey"));
    }

    @Test
    public void get_shouldBombWhenValueIsAPersistentObjectWithoutId() throws Exception{
        HgMaterial material = MaterialsMother.hgMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();
        materialInstance.setId(10);
        goCache.put("foo", materialInstance);
        materialInstance.setId(-1);

        try {
            goCache.get("foo");
            fail("should not allow getting a persistent object without id: " + materialInstance);
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void put_shouldBombWhenValueIsAPersistentObjectWithoutId() throws Exception{
        HgMaterial material = MaterialsMother.hgMaterial();
        MaterialInstance materialInstance = material.createMaterialInstance();
        try {
            goCache.put("foo", materialInstance);
            fail("should not allow getting a persistent object without id: " + materialInstance);
        } catch (Exception e) {
            // ok
        }
    }

    @Test
    public void shouldClear() {
        Object o = new Object();
        goCache.put("someKey", o);
        goCache.clear();
        assertNull(goCache.get("someKey"));
    }

    @Test
    public void shouldNotRunOutOfMemoryOnSubKeyPuts() throws IOException {
        for (Long n = 0L; n < 1; n++) {
            String key = "key" + (n % 10);
            String subKey = n.toString();
            goCache.put(key, subKey, n);
            assertThat((Long) goCache.get(key, subKey), is(n));
        }
    }

    @Test
    public void shouldNotRunOutOfMemoryOnKeyPuts() throws IOException {
        for (Long n = 0L; n < 100000; n++) {
            String key = "key" + n;
            Object value = largeObject();
            goCache.put(key, value);
            assertThat(goCache.get(key), is(value));
        }
    }

    private Object largeObject() throws IOException {
        if (largeObject == null) {
            StringBuilder s = new StringBuilder();
            for (int i = 0; i < 16000; i++) {
                s.append(random32CharString()).append("\n");
            }
            largeObject = s.toString();
        }
        return largeObject;
    }

    private String random32CharString() {
        return DigestUtils.md5Hex(String.valueOf(Math.random()));
    }

    @Test
    public void get_shouldGetScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        assertThat((String) goCache.get("foo", "bar"), is("baz"));
    }

    @Test
    public void put_shouldWriteScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        assertThat((String) goCache.get("foo", "bar"), is("baz"));
        assertThat((String) goCache.get("foo", "baz"), is("quux"));
    }

    @Test
    public void delete_shouldDeleteScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        goCache.remove("foo", "baz");
        assertThat((String) goCache.get("foo", "bar"), is("baz"));
        assertThat((String) goCache.get("foo", "baz"), is(nullValue()));
    }

    @Test
    public void delete_shouldDeleteAllSubValuesForParentKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        goCache.remove("foo");
        assertThat(goCache.get("foo", "bar"), is(nullValue()));
        assertThat(goCache.get("foo", "baz"), is(nullValue()));
        assertThat(goCache.get("foo"), is(nullValue()));
    }

    @Test
    public void put_shouldNotAllowAdditionOfBaseAndSubKeyPairThatUserInternalDemarkator() {
        try {
            goCache.put("foo!_#$", "#_!bar", "baz");
            fail("should not have allowed use of internal key seperator");
        } catch (Exception e) {
            assertThat(e.getMessage(), is("Base and sub key concatenation(key = foo!_#$, subkey = #_!bar) must not have pattern !_#$#_!"));
        }
    }

    @Test
    public void shouldStartServingThingsOutOfCacheOnceTransactionCompletes() {
        final SqlMapClientDaoSupport daoSupport = new SqlMapClientDaoSupport(goCache, sqlMapClient, systemEnvironment, databaseStrategy);
        goCache.put("foo", "bar");
        final String[] valueInCleanTxn = new String[1];
        final String[] valueInDirtyTxn = new String[1];
        final String[] valueInAfterCommit = new String[1];
        final String[] valueInAfterCompletion = new String[1];

        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override protected void doInTransactionWithoutResult(TransactionStatus status) {
                valueInCleanTxn[0] = (String) goCache.get("foo");
                User user = new User("loser", "Massive Loser", "boozer@loser.com");
                userSqlMapDao.saveOrUpdate(user);
                valueInDirtyTxn[0] = (String) goCache.get("foo");
                transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                    @Override public void afterCommit() {
                        valueInAfterCommit[0] = (String) goCache.get("foo");
                    }

                    @Override public void afterCompletion(int status) {
                        valueInAfterCompletion[0] = (String) goCache.get("foo");
                    }
                });
            }
        });
        assertThat(valueInCleanTxn[0], is("bar"));
        assertThat(valueInDirtyTxn[0], is(nullValue()));
        assertThat(valueInAfterCommit[0], is("bar"));
        assertThat(valueInAfterCompletion[0], is("bar"));
    }

    @Test
    public void shouldRemoveSpecifiedKeysFromCache(){
        goCache.put("foo", "1");
        goCache.put("bar", "2");
        goCache.put("baz", "3");
        goCache.removeAll(Arrays.asList("foo", "bar"));
        assertThat(goCache.get("foo"), is(nullValue()));
        assertThat(goCache.get("bar"), is(nullValue()));
        assertThat((String) goCache.get("baz"), is("3"));

    }
}
