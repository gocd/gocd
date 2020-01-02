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
package com.thoughtworks.go.config;

import java.util.UUID;

public class DefaultAgentRegistry implements AgentRegistry {

    private final GuidService guidService;
    private final TokenService tokenService;

    public DefaultAgentRegistry() {
        guidService = new GuidService();
        tokenService = new TokenService();
    }

    @Override
    public String uuid() {
        if (!guidService.dataPresent()) {
            guidService.store(UUID.randomUUID().toString());
        }
        return guidService.load();
    }

    @Override
    public String token() {
        return tokenService.load();
    }

    @Override
    public boolean tokenPresent() {
        return tokenService.dataPresent();
    }

    @Override
    public void storeTokenToDisk(String token) {
        tokenService.store(token);
    }

    @Override
    public void deleteToken() {
        tokenService.delete();
    }

    @Override
    public boolean guidPresent() {
        return guidService.dataPresent();
    }
}
