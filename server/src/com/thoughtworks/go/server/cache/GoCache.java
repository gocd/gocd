/*
 * Copyright 2016 ThoughtWorks, Inc.
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

import com.thoughtworks.go.domain.NullUser;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import net.sf.ehcache.Cache;
import net.sf.ehcache.Element;
import net.sf.ehcache.config.CacheConfiguration;
import net.sf.ehcache.statistics.LiveCacheStatistics;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.cache.ehcache.EhCacheFactoryBean;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands storing and retrieving objects from an underlying LRU cache
 */
public class GoCache {
    private final ThreadLocal<Boolean> doNotServeForTransaction = new ThreadLocal<>();

    static final String SUB_KEY_DELIMITER = "!_#$#_!";

    private Cache ehCache;

    private static final Logger LOGGER = Logger.getLogger(GoCache.class);
    private TransactionSynchronizationManager transactionSynchronizationManager;

    private final Set<Class<? extends PersistentObject>> nullObjectClasses;

    static class KeyList extends ArrayList<String> {
    }

    public GoCache(EhCacheFactoryBean ehCacheFactoryBean, TransactionSynchronizationManager transactionSynchronizationManager) {
        this((Cache) ehCacheFactoryBean.getObject(), transactionSynchronizationManager);
    }

    /**
     * @deprecated only for tests
     */
    public GoCache(GoCache goCache) {
        this(goCache.ehCache, goCache.transactionSynchronizationManager);
    }

    private GoCache(Cache cache, TransactionSynchronizationManager transactionSynchronizationManager) {
        this.ehCache = cache;
        this.transactionSynchronizationManager = transactionSynchronizationManager;
        this.nullObjectClasses = new HashSet<>();
        nullObjectClasses.add(NullUser.class);
        registerAsCacheEvictionListener();
    }

    protected void registerAsCacheEvictionListener() {
        ehCache.getCacheEventNotificationService().registerListener(new CacheEvictionListener(this));
    }

    public void stopServingForTransaction() {
        if (transactionSynchronizationManager.isTransactionBodyExecuting() && !doNotServeForTransaction()) {
            doNotServeForTransaction.set(true);
            transactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
                @Override
                public void beforeCompletion() {
                    doNotServeForTransaction.set(false);
                }
            });
        }
    }

    public void put(String key, Object value) {
        put(key, value, new TransactionActivityPredicate());
    }

    private void put(String key, Object value, Predicate predicate) {
        logUnsavedPersistentObjectInteraction(value, "PersistentObject %s added to cache without an id.");
        if (predicate.isTrue()) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(String.format("transaction active during cache put for %s = %s", key, value), new IllegalStateException());
            }
            return;
        }
        ehCache.put(new Element(key, value));
    }

    public List<String> getKeys() {
        return ehCache.getKeys();
    }

    /**
     * SHOULD ONLY BE USED IN AN AFTER-COMMIT CALLBACK. In all other cases you should be using put() which ensures that
     * no transaction is active at the moment before putting the value. This ensures that you don't end up having data
     * in cache which is invalid because the transaction has rolled back.
     *
     * @param key
     * @param value
     */
    public void putInAfterCommit(String key, Object value) {
        put(key, value, new InTransactionBodyPredicate());
    }

    private void logUnsavedPersistentObjectInteraction(Object value, String message) {
        if (value instanceof PersistentObject) {
            for (Class<? extends PersistentObject> nullObjectClass : nullObjectClasses) {
                if (value.getClass().equals(nullObjectClass)) {
                    return;
                }
            }
            PersistentObject persistentObject = (PersistentObject) value;
            if (!persistentObject.hasId()) {
                String msg = String.format(message, persistentObject);
                IllegalStateException exception = new IllegalStateException();
                LOGGER.error(msg, exception);
                throw bomb(msg, exception);
            }
        }
    }

    public void flush() {
        ehCache.flush();
    }

    public Object get(String key) {
        if (doNotServeForTransaction()) {
            return null;
        }
        return getWithoutTransactionCheck(key);
    }

    private Object getWithoutTransactionCheck(String key) {
        Element element = ehCache.get(key);
        if (element == null) {
            return null;
        }
        Object value = element.getObjectValue();
        logUnsavedPersistentObjectInteraction(value, "PersistentObject %s without an id served out of cache.");
        return value;
    }

    private boolean doNotServeForTransaction() {
        return doNotServeForTransaction.get() != null && doNotServeForTransaction.get();
    }

    public void clear() {
        ehCache.removeAll();
    }

    public boolean remove(String key) {
        synchronized (key.intern()) {
            Object value = getWithoutTransactionCheck(key);
            if (value instanceof KeyList) {
                for (String subKey : (KeyList) value) {
                    ehCache.remove(compositeKey(key, subKey));
                }
            }
            return ehCache.remove(key);
        }
    }

    public Object get(String key, String subKey) {
        return get(compositeKey(key, subKey));
    }

    public void put(String key, String subKey, Object value) {
        KeyList subKeys;
        synchronized (key.intern()) {
            subKeys = subKeyFamily(key);
            if (subKeys == null) {
                subKeys = new KeyList();
                put(key, subKeys);
            }
            subKeys.add(subKey);
        }
        put(compositeKey(key, subKey), value);
    }

    public void removeAll(List<String> keys) {
        for (String key : keys) {
            remove(key);
        }
    }

    public void removeAssociations(String key, Element element) {
        if (element.getObjectValue() instanceof KeyList) {
            synchronized (key.intern()) {
                for (String subkey : (KeyList) element.getObjectValue()) {
                    remove(compositeKey(key, subkey));
                }
            }
        } else if (key.contains(SUB_KEY_DELIMITER)) {
            String[] parts = StringUtils.splitByWholeSeparator(key, SUB_KEY_DELIMITER);
            String parentKey = parts[0];
            String childKey = parts[1];
            synchronized (parentKey.intern()) {
                Element parent = ehCache.get(parentKey);
                if (parent == null) {
                    return;
                }
                GoCache.KeyList subKeys = (GoCache.KeyList) parent.getObjectValue();
                subKeys.remove(childKey);
            }
        }
    }


    public boolean isKeyInCache(Object key) {
        return ehCache.isKeyInCache(key);
    }

    private KeyList subKeyFamily(String parentKey) {
        return (KeyList) get(parentKey);
    }

    private String compositeKey(String key, String subKey) {
        String concat = key + subKey;
        if (concat.contains(SUB_KEY_DELIMITER)) {
            bomb(String.format("Base and sub key concatenation(key = %s, subkey = %s) must not have pattern %s", key, subKey, SUB_KEY_DELIMITER));
        }
        return key + SUB_KEY_DELIMITER + subKey;
    }

    public void remove(String key, String subKey) {
        synchronized (key.intern()) {
            KeyList subKeys = subKeyFamily(key);
            subKeys.remove(subKey);
            remove(compositeKey(key, subKey));
        }
    }

    public LiveCacheStatistics statistics() {
        return ehCache.getLiveCacheStatistics();
    }

    public CacheConfiguration configuration() {
        return ehCache.getCacheConfiguration();
    }

    private interface Predicate {
        boolean isTrue();
    }

    private class TransactionActivityPredicate implements Predicate {
        public boolean isTrue() {
            return transactionSynchronizationManager.isActualTransactionActive();
        }
    }

    private class InTransactionBodyPredicate implements Predicate {
        public boolean isTrue() {
            return transactionSynchronizationManager.isTransactionBodyExecuting();
        }
    }
}
