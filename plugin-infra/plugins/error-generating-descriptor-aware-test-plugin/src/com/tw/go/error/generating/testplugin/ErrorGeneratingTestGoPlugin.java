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

package com.tw.go.error.generating.testplugin;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import java.util.Collections;

@Extension
public class ErrorGeneratingTestGoPlugin implements GoPlugin {
    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("extension-1", Collections.singletonList("1.0"));
    }

    /* This should fail with an AbstractMethodError if you call something on it, which doesn't exist. */
}
