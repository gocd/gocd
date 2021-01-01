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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.config.Agent;

public final class NullAgent extends Agent {
    private NullAgent() {
        this("Unknown-uuid");
    }

    private NullAgent(String uuid) {
        super(uuid, "", "Unknown");
    }

    @Override
    public ConfigErrors errors() {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public static NullAgent createNullAgent() {
        return new NullAgent();
    }

    public static NullAgent createNullAgent(String uuid) {
        return new NullAgent(uuid);
    }

    @Override
    public boolean isNull() {
        return true;
    }

    public String getHostnameForDisplay() {
        return "Not yet assigned";
    }
}
