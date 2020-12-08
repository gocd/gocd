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

package com.thoughtworks.go.server.messaging;

import com.google.gson.Gson;
import com.thoughtworks.go.config.Agent;
import com.thoughtworks.go.server.service.AgentRuntimeInfo;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class SerializationTest {
    @Test
    void test1() {
        AgentRuntimeInfo old = AgentRuntimeInfo.fromServer(
                new Agent("uuid", "localhost", "127.0.0.1"), false, "/var/lib", 0L, "linux");

        Gson gson = new Gson();
        String toJson = gson.toJson(old);

        AgentRuntimeInfo newObj = gson.fromJson(toJson, AgentRuntimeInfo.class);
        assertTrue(EqualsBuilder.reflectionEquals(old, newObj));
    }

    @Test
    void test2() {

    }
}
