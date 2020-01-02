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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.transaction.TestTransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
* @understands: StubGoCache
*/
public class StubGoCache extends GoCache {

    private Map<String, Object> map;

    public StubGoCache(final TestTransactionSynchronizationManager transactionSynchronizationManager) {
        super(null, transactionSynchronizationManager);
        map = new HashMap<>();
    }

    @Override public void put(String key, Object value) {
        map.put(key, value);
    }

    @Override public Object get(String key) {
        return map.get(key);
    }

    @Override public boolean remove(String key) {
        map.remove(key);
        return true;
    }

    @Override
    public void clear() {
        map.clear();
    }

    @Override
    public List<String> getKeys() {
        return new ArrayList<>(map.keySet());
    }

    @Override
    protected void registerAsCacheEvictionListener() {
    }
}
