/*
 * Copyright 2016 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.studios.shine.cruise.stage;

import java.text.MessageFormat;
import java.util.List;

import com.thoughtworks.go.domain.StageIdentifier;

public interface StagesQueryCache {

    class CacheKey {
        private final String sparql;
        private final StageIdentifier stageIdentifier;

        public CacheKey(String sparql, StageIdentifier stageIdentifier) {
            this.sparql = sparql;
            this.stageIdentifier = stageIdentifier;
        }

        public String getKey() {
            int hash = 31 * sparql.hashCode() + stageIdentifier.stageLocator().hashCode();
            return MessageFormat.format("shine.{0}", hash);
        }
    }

    List get(CacheKey key);

    void put(List resultModels, CacheKey key);

    void clear();
}
