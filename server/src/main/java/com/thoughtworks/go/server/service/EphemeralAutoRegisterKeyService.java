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

package com.thoughtworks.go.server.service;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.thoughtworks.go.util.SystemEnvironment;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.ConcurrentMap;

import static java.util.UUID.randomUUID;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

@Service
public class EphemeralAutoRegisterKeyService {
    private final ConcurrentMap<String, String> keyStore;

    @Autowired
    public EphemeralAutoRegisterKeyService(SystemEnvironment systemEnvironment) {
        keyStore = Caffeine.newBuilder()
            .expireAfterWrite(systemEnvironment.getEphemeralAutoRegisterKeyExpiryInMillis(), MILLISECONDS)
            .<String, String>build()
            .asMap();
    }

    public String autoRegisterKey() {
        String uuid = randomUUID().toString();
        keyStore.put(uuid, uuid);

        return uuid;
    }

    public boolean validateAndRevoke(String autoRegisterKey) {
        return keyStore.remove(autoRegisterKey, autoRegisterKey);
    }
}
