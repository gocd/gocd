/****
 * Copyright 2020 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *****/

package com.tw.go.testplugin;

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.annotation.Load;
import com.thoughtworks.go.plugin.api.annotation.UnLoad;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.info.PluginContext;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.tw.go.dependency.Console;

import java.util.Collections;

@Extension
public class DescriptorValidatorPlugin implements GoPlugin {
    public int loadCalled = 0;
    public int unloadCalled = 0;

    static {
        Logger.getLoggerFor(DescriptorValidatorPlugin.class).info("Boo");
    }

    Console console = new Console();
    Logger logger = Logger.getLoggerFor(DescriptorValidatorPlugin.class);

    @Load
    public void onLoad(PluginContext context) {
        logger.info("Boo");
        System.out.println("Plugin loaded");
        ++loadCalled;
    }

    @UnLoad
    public void onUnload(PluginContext context) {
        ++unloadCalled;
        System.out.println("Plugin unloaded");
    }

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor goApplicationAccessor) {
        if (loadCalled == 0) {
            throw new RuntimeException("Load callback has not been called for this plugin");
        }
        System.setProperty("testplugin.descriptorValidator.setPluginDescriptor.invoked", "PluginLoad: " + loadCalled + ", InitAccessor");
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest goPluginApiRequest) throws UnhandledRequestTypeException {
        return DefaultGoPluginApiResponse.success(goPluginApiRequest.requestBody());
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return new GoPluginIdentifier("notification", Collections.singletonList("2.0"));
    }
}
