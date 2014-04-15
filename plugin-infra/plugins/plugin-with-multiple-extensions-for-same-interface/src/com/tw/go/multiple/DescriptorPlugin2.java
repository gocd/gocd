/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
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
 *************************GO-LICENSE-END***********************************/

package com.tw.go.multiple;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.annotation.Load;
import com.thoughtworks.go.plugin.api.annotation.UnLoad;
import com.thoughtworks.go.plugin.api.info.PluginContext;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;
import com.thoughtworks.go.plugin.api.logging.Logger;

@Extension
public class DescriptorPlugin2 implements PluginDescriptorAware {
    public int loadCalled = 0;
    public int unloadCalled = 0;

    static {
        Logger.getLoggerFor(DescriptorPlugin2.class).info("Boo");
    }

    Logger logger = Logger.getLoggerFor(DescriptorPlugin2.class);

    @Load
    public void onLoad(PluginContext context) {
        logger.info("Boo");
        System.out.println("Plugin loaded");
        ++loadCalled;
    }

    public void setPluginDescriptor(PluginDescriptor descriptor) {
        if (loadCalled == 0) {
            throw new RuntimeException("Load callback has not been called for this plugin");
        }
        System.out.println("Got the descriptor: " + descriptor);
        System.setProperty("testplugin.multiple.extension.DescriptorPlugin2.setPluginDescriptor.invoked", descriptor.toString());
    }

    @UnLoad
    public void onUnload(PluginContext context) {
        ++unloadCalled;
        System.out.println("Plugin unloaded");
    }
}
