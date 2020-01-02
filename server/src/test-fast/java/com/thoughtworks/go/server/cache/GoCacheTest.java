/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.cache;

import ch.qos.logback.classic.Level;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.util.LogFixture;
import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.config.Configuration;
import net.sf.ehcache.store.MemoryStoreEvictionPolicy;
import org.apache.commons.codec.digest.DigestUtils;
import org.junit.*;

import java.io.IOException;
import java.util.Arrays;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

public class GoCacheTest {

    private static CacheManager cacheManager;
    private GoCache goCache;
    private String largeObject;

    @BeforeClass
    public static void beforeClass() {
        cacheManager = CacheManager.newInstance(new Configuration().name(GoCacheTest.class.getName()));
    }

    @Before
    public void setUp() throws Exception {
        Cache cache = new Cache(new CacheConfiguration(getClass().getName(), 100).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU));
        cacheManager.addCache(cache);
        this.goCache = new GoCache(cache, mock(TransactionSynchronizationManager.class));
    }

    @After
    public void tearDown() {
        cacheManager.removeAllCaches();
    }

    @AfterClass
    public static void afterClass() {
        cacheManager.shutdown();
    }

    @Test
    public void shouldAllowAddingUnpersistedNullObjects() {
        NullUser user = new NullUser();
        goCache.put("loser_user", user);
        assertThat(goCache.get("loser_user"), is(user));
        try (LogFixture logFixture = logFixtureFor(GoCache.class, Level.DEBUG)) {
            String result;
            synchronized (logFixture) {
                result = logFixture.getLog();
            }
            String allLogs = result;
            assertThat(allLogs, not(containsString("added to cache without an id.")));
            assertThat(allLogs, not(containsString("without an id served out of cache.")));
        }
    }

    @Test
    public void shouldBeAbleToGetAnObjectThatIsPutIntoIt() {
        Object o = new Object();
        goCache.put("someKey", o);
        assertSame(o, goCache.get("someKey"));
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
    public void get_shouldBombWhenValueIsAPersistentObjectWithoutId() {
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
    public void put_shouldBombWhenValueIsAPersistentObjectWithoutId() {
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
    public void shouldNotRunOutOfMemoryOnSubKeyPuts() {
        for (Long n = 0L; n < 1; n++) {
            String key = "key" + (n % 10);
            String subKey = n.toString();
            goCache.put(key, subKey, n);
            assertThat(goCache.get(key, subKey), is(n));
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

    private Object largeObject() {
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
        assertThat(goCache.get("foo", "bar"), is("baz"));
    }

    @Test
    public void put_shouldWriteScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        assertThat(goCache.get("foo", "bar"), is("baz"));
        assertThat(goCache.get("foo", "baz"), is("quux"));
    }

    @Test
    public void delete_shouldDeleteScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        goCache.remove("foo", "baz");
        assertThat(goCache.get("foo", "bar"), is("baz"));
        assertThat(goCache.get("foo", "baz"), is(nullValue()));
    }

    @Test
    public void delete_shouldNotThrowAnExceptionWhenNoFamilySubKeysAreFound() {
        goCache.remove("foo", "baz");
        assertThat(goCache.get("foo", "baz"), nullValue());
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
    public void shouldRemoveSpecifiedKeysFromCache() {
        goCache.put("foo", "1");
        goCache.put("bar", "2");
        goCache.put("baz", "3");
        goCache.removeAll(Arrays.asList("foo", "bar"));
        assertThat(goCache.get("foo"), is(nullValue()));
        assertThat(goCache.get("bar"), is(nullValue()));
        assertThat(goCache.get("baz"), is("3"));
    }

    @Test
    public void shouldEvictSubkeyFromParentCacheWhenTheSubkeyEntryGetsEvicted() throws InterruptedException {
        goCache.configuration().setMaxEntriesLocalHeap(2);
        String parentKey = "parent";
        goCache.put(parentKey, "child1", "value");
        assertThat(goCache.get(parentKey), is(not(nullValue())));
        assertThat(goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1"), is(not(nullValue())));
        Thread.sleep(1);//so that the timestamps on the cache entries are different
        goCache.put(parentKey, "child2", "value");

        assertThat(goCache.get(parentKey), is(not(nullValue())));
        assertThat(goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1"), is(nullValue()));
        assertThat(goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child2"), is(not(nullValue())));
        GoCache.KeyList list = (GoCache.KeyList) goCache.get(parentKey);
        assertThat(list.size(), is(1));
        assertThat(list.contains("child2"), is(true));
    }

    @Test
    public void shouldEvictSubkeyFromParentCacheWhenTheSubkeyEntryGetsExpired() throws InterruptedException {
        goCache.configuration().setEternal(false);
        goCache.configuration().setTimeToLiveSeconds(1);
        String parentKey = "parent";
        goCache.put(parentKey, "child1", "value");
        assertThat(goCache.get(parentKey), is(not(nullValue())));
        assertThat(goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1"), is(not(nullValue())));
        waitForCacheElementsToExpire();

        goCache.put(parentKey, "child2", "value");
        assertThat(goCache.get(parentKey), is(not(nullValue())));
        assertThat(goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1"), is(nullValue()));
        assertThat(goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child2"), is(not(nullValue())));
        GoCache.KeyList list = (GoCache.KeyList) goCache.get(parentKey);
        assertThat(list.size(), is(1));
        assertThat(list.contains("child2"), is(true));
    }

    @Test
    public void shouldEvictAllSubkeyCacheEntriesWhenTheParentEntryGetsEvicted() throws InterruptedException {
        goCache.configuration().setMaxEntriesLocalHeap(2);
        String parentKey = "parent";
        goCache.put(parentKey, new GoCache.KeyList());
        assertThat(goCache.get(parentKey), is(not(nullValue())));
        goCache.put(parentKey, "child1", "value");
        Thread.sleep(1); //so that the timestamps on the cache entries are different
        goCache.get(parentKey, "child1");  //so that the parent is least recently used
        Thread.sleep(1); //so that the timestamps on the cache entries are different
        goCache.put("unrelatedkey", "value");
        waitForCacheElementsToExpire();
        assertThat(goCache.getKeys().size(), is(1));
        assertThat(goCache.get("unrelatedkey"), is("value"));
    }

    @Test
    public void shouldHandleNonSerializableValuesDuringEviction() throws InterruptedException {
        goCache.configuration().setMaxEntriesLocalHeap(1);
        NonSerializableClass value = new NonSerializableClass();
        String key = "key";
        goCache.put(key, value);
        Thread.sleep(1);//so that the timestamps on the cache entries are different
        goCache.put("another_entry", "value");
        assertThat(goCache.get(key), is(nullValue()));
    }


    private class NonSerializableClass {
    }

    private void waitForCacheElementsToExpire() throws InterruptedException {
        Thread.sleep(2000);
    }
}
