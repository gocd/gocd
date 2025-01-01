/*
 * Copyright Thoughtworks, Inc.
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
import org.junit.jupiter.api.*;

import java.util.List;

import static com.thoughtworks.go.util.LogFixture.logFixtureFor;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class GoCacheTest {

    private static CacheManager cacheManager;
    private GoCache goCache;

    @BeforeAll
    public static void beforeClass() {
        cacheManager = CacheManager.newInstance(new Configuration().name(GoCacheTest.class.getName()));
    }

    @BeforeEach
    public void setUp() throws Exception {
        Cache cache = new Cache(new CacheConfiguration(getClass().getName(), 100).memoryStoreEvictionPolicy(MemoryStoreEvictionPolicy.LRU));
        cacheManager.addCache(cache);
        this.goCache = new GoCache(cache, mock(TransactionSynchronizationManager.class));
    }

    @AfterEach
    public void tearDown() {
        goCache.destroy();
    }

    @AfterAll
    public static void afterClass() {
        cacheManager.shutdown();
    }

    @Test
    public void shouldAllowAddingUnpersistedNullObjects() {
        NullUser user = new NullUser();
        goCache.put("loser_user", user);
        assertThat((Object) goCache.get("loser_user")).isEqualTo(user);
        try (LogFixture logFixture = logFixtureFor(GoCache.class, Level.DEBUG)) {
            String result;
            synchronized (logFixture) {
                result = logFixture.getLog();
            }
            String allLogs = result;
            assertThat(allLogs).doesNotContain("added to cache without an id.");
            assertThat(allLogs).doesNotContain("without an id served out of cache.");
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
    public void get_shouldGetScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        assertThat(goCache.get("foo", "bar")).isEqualTo("baz");
    }

    @Test
    public void put_shouldWriteScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        assertThat(goCache.get("foo", "bar")).isEqualTo("baz");
        assertThat(goCache.get("foo", "baz")).isEqualTo("quux");
    }

    @Test
    public void delete_shouldDeleteScopedValueForSubKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        goCache.remove("foo", "baz");
        assertThat(goCache.get("foo", "bar")).isEqualTo("baz");
        assertThat(goCache.get("foo", "baz")).isNull();
    }

    @Test
    public void delete_shouldNotThrowAnExceptionWhenNoFamilySubKeysAreFound() {
        goCache.remove("foo", "baz");
        assertThat(goCache.get("foo", "baz")).isNull();
    }

    @Test
    public void delete_shouldDeleteAllSubValuesForParentKey() {
        goCache.put("foo", "bar", "baz");
        goCache.put("foo", "baz", "quux");
        goCache.remove("foo");
        assertThat(goCache.get("foo", "bar")).isNull();
        assertThat(goCache.get("foo", "baz")).isNull();
        assertThat((Object) goCache.get("foo")).isNull();
    }

    @Test
    public void put_shouldNotAllowAdditionOfBaseAndSubKeyPairThatUserInternalDemarkator() {
        try {
            goCache.put("foo!_#$", "#_!bar", "baz");
            fail("should not have allowed use of internal key seperator");
        } catch (Exception e) {
            assertThat(e.getMessage()).isEqualTo("Base and sub key concatenation(key = foo!_#$, subkey = #_!bar) must not have pattern !_#$#_!");
        }
    }


    @Test
    public void shouldRemoveSpecifiedKeysFromCache() {
        goCache.put("foo", "1");
        goCache.put("bar", "2");
        goCache.put("baz", "3");
        goCache.removeAll(List.of("foo", "bar"));
        assertThat((Object) goCache.get("foo")).isNull();
        assertThat((Object) goCache.get("bar")).isNull();
        assertThat((Object) goCache.get("baz")).isEqualTo("3");
    }

    @Test
    public void shouldEvictSubkeyFromParentCacheWhenTheSubkeyEntryGetsEvicted() throws InterruptedException {
        goCache.configuration().setMaxEntriesLocalHeap(2);
        String parentKey = "parent";
        goCache.put(parentKey, "child1", "value");
        assertThat((Object) goCache.get(parentKey)).isNotNull();
        assertThat((Object) goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1")).isNotNull();
        Thread.sleep(1);//so that the timestamps on the cache entries are different
        goCache.put(parentKey, "child2", "value");

        assertThat((Object) goCache.get(parentKey)).isNotNull();
        assertThat((Object) goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1")).isNull();
        assertThat((Object) goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child2")).isNotNull();
        GoCache.KeyList list = goCache.get(parentKey);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.contains("child2")).isTrue();
    }

    @Test
    public void shouldEvictSubkeyFromParentCacheWhenTheSubkeyEntryGetsExpired() throws InterruptedException {
        goCache.configuration().setEternal(false);
        goCache.configuration().setTimeToLiveSeconds(1);
        String parentKey = "parent";
        goCache.put(parentKey, "child1", "value");
        assertThat((Object) goCache.get(parentKey)).isNotNull();
        assertThat((Object) goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1")).isNotNull();
        waitForCacheElementsToExpire();

        goCache.put(parentKey, "child2", "value");
        assertThat((Object) goCache.get(parentKey)).isNotNull();
        assertThat((Object) goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child1")).isNull();
        assertThat((Object) goCache.get(parentKey + GoCache.SUB_KEY_DELIMITER + "child2")).isNotNull();
        GoCache.KeyList list = goCache.get(parentKey);
        assertThat(list.size()).isEqualTo(1);
        assertThat(list.contains("child2")).isTrue();
    }

    @Test
    public void shouldEvictAllSubkeyCacheEntriesWhenTheParentEntryGetsEvicted() throws InterruptedException {
        goCache.configuration().setMaxEntriesLocalHeap(2);
        String parentKey = "parent";
        goCache.put(parentKey, new GoCache.KeyList());
        assertThat((Object) goCache.get(parentKey)).isNotNull();
        goCache.put(parentKey, "child1", "value");
        Thread.sleep(1); //so that the timestamps on the cache entries are different
        goCache.get(parentKey, "child1");  //so that the parent is least recently used
        Thread.sleep(1); //so that the timestamps on the cache entries are different
        goCache.put("unrelatedkey", "value");
        waitForCacheElementsToExpire();
        assertThat(goCache.getKeys().size()).isEqualTo(1);
        assertThat((Object) goCache.get("unrelatedkey")).isEqualTo("value");
    }

    @Test
    public void shouldHandleNonSerializableValuesDuringEviction() throws InterruptedException {
        goCache.configuration().setMaxEntriesLocalHeap(1);
        NonSerializableClass value = new NonSerializableClass();
        String key = "key";
        goCache.put(key, value);
        Thread.sleep(1);//so that the timestamps on the cache entries are different
        goCache.put("another_entry", "value");
        assertThat((Object) goCache.get(key)).isNull();
    }


    private static class NonSerializableClass {
    }

    private void waitForCacheElementsToExpire() throws InterruptedException {
        Thread.sleep(2000);
    }
}
