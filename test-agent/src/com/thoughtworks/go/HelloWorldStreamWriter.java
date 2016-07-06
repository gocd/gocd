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

package com.thoughtworks.go;

import com.thoughtworks.go.agent.ServerUrlGenerator;
import com.thoughtworks.go.agent.common.launcher.AgentProcessParent;

import java.util.Map;

public class HelloWorldStreamWriter implements AgentProcessParent {

    @Override public int run(String launcherVersion, String launcherMd5, ServerUrlGenerator urlGenerator, Map<String, String> env, Map context) {
        System.err.print("Hello World Fellas!");
        return 42;
    }
}
