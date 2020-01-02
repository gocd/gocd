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
package com.thoughtworks.go.agent.common.util;

import java.io.File;

public interface Downloader {
    String AGENT_BINARY = "agent.jar";
    String AGENT_LAUNCHER = "agent-launcher.jar";
    String AGENT_PLUGINS = "agent-plugins.zip";
    String TFS_IMPL = "tfs-impl.jar";

    File AGENT_BINARY_JAR = new File(AGENT_BINARY);
    File AGENT_LAUNCHER_JAR = new File(AGENT_LAUNCHER);
    File AGENT_PLUGINS_ZIP = new File(AGENT_PLUGINS);
    File TFS_IMPL_JAR = new File(TFS_IMPL);
}
