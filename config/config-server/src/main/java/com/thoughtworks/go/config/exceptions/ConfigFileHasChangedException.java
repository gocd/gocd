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
package com.thoughtworks.go.config.exceptions;

/**
 * @understands the condition when the config file change failed because it has already been changed by someone else
 */
public class ConfigFileHasChangedException extends RuntimeException {
    public static final String CONFIG_CHANGED_PLEASE_REFRESH = "Configuration file has been modified by someone else.";

    public ConfigFileHasChangedException() {
        super(CONFIG_CHANGED_PLEASE_REFRESH);
    }
}
