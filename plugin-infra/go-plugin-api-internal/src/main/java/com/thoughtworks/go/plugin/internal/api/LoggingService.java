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
package com.thoughtworks.go.plugin.internal.api;

public interface LoggingService {
    void debug(String pluginId, String loggerName, String message);

    void debug(String pluginId, String loggerName, String message, Throwable throwable);

    void debug(String pluginId, String loggerName, String message, Object arg);

    void debug(String pluginId, String loggerName, String message, Object arg1, Object arg2);

    void debug(String pluginId, String loggerName, String message, Object... arguments);

    void info(String pluginId, String loggerName, String message);

    void info(String pluginId, String loggerName, String message, Throwable throwable);

    void info(String pluginId, String loggerName, String message, Object arg);

    void info(String pluginId, String loggerName, String message, Object arg1, Object arg2);

    void info(String pluginId, String loggerName, String message, Object... arguments);

    void warn(String pluginId, String loggerName, String message);

    void warn(String pluginId, String loggerName, String message, Throwable throwable);

    void warn(String pluginId, String loggerName, String message, Object arg);

    void warn(String pluginId, String loggerName, String message, Object arg1, Object arg2);

    void warn(String pluginId, String loggerName, String message, Object... arguments);

    void error(String pluginId, String loggerName, String message);

    void error(String pluginId, String loggerName, String message, Throwable throwable);

    void error(String pluginId, String loggerName, String message, Object arg);

    void error(String pluginId, String loggerName, String message, Object arg1, Object arg2);

    void error(String pluginId, String loggerName, String message, Object... arguments);
}
