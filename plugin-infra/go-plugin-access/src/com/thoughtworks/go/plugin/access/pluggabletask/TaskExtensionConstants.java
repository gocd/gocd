/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.pluggabletask;

import java.util.Arrays;
import java.util.List;

public interface TaskExtensionConstants {
    List<String> SUPPORTED_VERSIONS = Arrays.asList(JsonBasedTaskExtensionHandler_V1.VERSION);
    String TASK_EXTENSION = "task";
    String CONFIGURATION_REQUEST = "configuration";
    String VALIDATION_REQUEST = "validate";
    String EXECUTION_REQUEST = "execute";
    String TASK_VIEW_REQUEST = "view";
}
