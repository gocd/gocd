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
package com.thoughtworks.go.plugin.infra;

import com.thoughtworks.go.plugin.infra.plugininfo.GoPluginDescriptor;

import java.util.List;
import java.util.Map;

/**
 * A hook which runs after plugin load, and before listeners are called. Has the power to stop loading the plugin and marking it invalid.
 */
public interface PluginPostLoadHook {
    Result run(GoPluginDescriptor pluginDescriptor, Map<String, List<String>> extensionsInfoFromThePlugin);

    class Result {
        private final boolean isAFailure;
        private final String message;

        public Result(boolean isAFailure, String message) {
            this.isAFailure = isAFailure;
            this.message = message;
        }

        public boolean isAFailure() {
            return isAFailure;
        }

        public String getMessage() {
            return message;
        }
    }
}
