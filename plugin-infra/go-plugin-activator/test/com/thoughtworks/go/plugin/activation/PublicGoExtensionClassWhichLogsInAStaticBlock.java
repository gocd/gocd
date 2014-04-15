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

package com.thoughtworks.go.plugin.activation;

import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.annotation.Load;
import com.thoughtworks.go.plugin.api.annotation.UnLoad;
import com.thoughtworks.go.plugin.api.info.PluginContext;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;
import com.thoughtworks.go.plugin.api.logging.Logger;

@Extension
public class PublicGoExtensionClassWhichLogsInAStaticBlock implements PluginDescriptorAware {
    private static Logger logger;

    static {
        logger = Logger.getLoggerFor(PublicGoExtensionClassWhichLogsInAStaticBlock.class);
        logger.info("HELLO from static block in PublicGoExtensionClassWhichLogsInAStaticBlock");
    }

    public PublicGoExtensionClassWhichLogsInAStaticBlock() {
        logger.info("HELLO from constructor in PublicGoExtensionClassWhichLogsInAStaticBlock");
    }

    @Load
    public void setupData(PluginContext context) {
        logger.info("HELLO from load in PublicGoExtensionClassWhichLogsInAStaticBlock");
    }

    @UnLoad
    public void tearDown(PluginContext context) {
        logger.info("HELLO from unload in PublicGoExtensionClassWhichLogsInAStaticBlock");
    }
    @Override
    public void setPluginDescriptor(PluginDescriptor descriptor) {
    }
}
