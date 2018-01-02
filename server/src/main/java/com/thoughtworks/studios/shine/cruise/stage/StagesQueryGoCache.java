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

package com.thoughtworks.studios.shine.cruise.stage;

import java.util.List;

import com.thoughtworks.go.server.cache.GoCache;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class StagesQueryGoCache implements StagesQueryCache {
    private GoCache goCache;

    @Autowired
    public StagesQueryGoCache(GoCache goCache) {
        this.goCache = goCache;
    }

    public List get(CacheKey key) {
        return (List) goCache.get(key.getKey());
    }

    public void put(List bvs, CacheKey key) {
        goCache.put(key.getKey(), bvs);
    }

    public void clear() {
        goCache.clear();
    }
}
