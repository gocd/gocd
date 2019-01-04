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

package com.thoughtworks.go.config;

/*  @description
 * Keeps track all config repo parsing states
 */


import com.thoughtworks.go.config.remote.PartialConfig;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConfigReposMaterialParseResultManager {
    private Map<String, PartialConfigParseResult> fingerprintOfPartialToParseResultMap = new ConcurrentHashMap<>();

    public PartialConfigParseResult get(String fingerprint) {
        return fingerprintOfPartialToParseResultMap.get(fingerprint);
    }

    public Set<String> allFingerprints() {
        return fingerprintOfPartialToParseResultMap.keySet();
    }

    public void remove(String fingerprint) {
        fingerprintOfPartialToParseResultMap.remove(fingerprint);
    }

    public void parseFailed(String fingerprint, String revision, Exception exception) {
        fingerprintOfPartialToParseResultMap.put(fingerprint, new PartialConfigParseResult(revision, exception));
    }

    public void parseSuccess(String fingerprint, String revision, PartialConfig newPart) {
        fingerprintOfPartialToParseResultMap.put(fingerprint, new PartialConfigParseResult(revision, newPart));
    }
}
