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

import net.sf.ehcache.CacheException;
import net.sf.ehcache.Ehcache;
import net.sf.ehcache.Element;
import net.sf.ehcache.event.CacheEventListener;

public class CacheEvictionListener implements CacheEventListener {
    private final GoCache goCache;

    public CacheEvictionListener(GoCache goCache) {
        this.goCache = goCache;
    }

    @Override
    public void notifyElementRemoved(Ehcache cache, Element element) throws CacheException {
        // do nothing
    }

    @Override
    public void notifyElementPut(Ehcache cache, Element element) throws CacheException {
        // do nothing
    }

    @Override
    public void notifyElementUpdated(Ehcache cache, Element element) throws CacheException {
        // do nothing
    }

    @Override
    public void notifyElementExpired(Ehcache cache, Element element) {
        removeCompositeKeyFromParentCache(element);
    }

    @Override
    public void notifyElementEvicted(Ehcache cache, Element element) {
        removeCompositeKeyFromParentCache(element);
    }

    private void removeCompositeKeyFromParentCache(Element element) {
        goCache.removeAssociations((String) element.getKey(), element);
    }

    @Override
    public void notifyRemoveAll(Ehcache cache) {

    }

    @Override
    public void dispose() {

    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        super.clone();
        throw new CloneNotSupportedException();
    }
}
