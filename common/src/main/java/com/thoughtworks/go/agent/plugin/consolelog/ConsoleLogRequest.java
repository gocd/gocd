/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.agent.plugin.consolelog;

import static com.thoughtworks.go.plugin.domain.common.PluginConstants.*;
import static java.lang.String.format;

public enum ConsoleLogRequest {
    ARTIFACT_PLUGIN_CONSOLE_LOG(ARTIFACT_EXTENSION),
    SCM_PLUGIN_CONSOLE_LOG(SCM_EXTENSION),
    TASK_PLUGIN_CONSOLE_LOG(PLUGGABLE_TASK_EXTENSION);

    private final String requestName;
    private final String extension;

    ConsoleLogRequest(String extension) {
        this.extension = extension;
        this.requestName = format("go.processor.%s.console-log", extension);
    }

    public static ConsoleLogRequest fromString(String requestName) {
        if (requestName != null) {
            for (ConsoleLogRequest request : values()) {
                if (requestName.equalsIgnoreCase(request.requestName)) {
                    return request;
                }
            }
        }

        throw new RuntimeException("Invalid console log request name.");
    }

    public String getExtension() {
        return extension;
    }

    public String requestName() {
        return requestName;
    }
}
