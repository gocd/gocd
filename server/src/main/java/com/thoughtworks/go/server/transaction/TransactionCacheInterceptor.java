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
package com.thoughtworks.go.server.transaction;

import java.io.Serializable;

import com.thoughtworks.go.server.cache.GoCache;
import org.hibernate.EmptyInterceptor;
import org.hibernate.type.Type;

public class TransactionCacheInterceptor extends EmptyInterceptor {

    private final GoCache goCache;

    public TransactionCacheInterceptor(GoCache goCache) {
        this.goCache = goCache;
    }

    @Override public boolean onSave(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        stopServingFromCache();
        return super.onSave(entity, id, state, propertyNames, types);
    }

    @Override public boolean onFlushDirty(Object entity, Serializable id, Object[] currentState, Object[] previousState, String[] propertyNames, Type[] types) {
        stopServingFromCache();
        return super.onFlushDirty(entity, id, currentState, previousState, propertyNames, types);
    }

    @Override public void onDelete(Object entity, Serializable id, Object[] state, String[] propertyNames, Type[] types) {
        stopServingFromCache();
        super.onDelete(entity, id, state, propertyNames, types);
    }

    private void stopServingFromCache() {
        goCache.stopServingForTransaction();
    }
}
