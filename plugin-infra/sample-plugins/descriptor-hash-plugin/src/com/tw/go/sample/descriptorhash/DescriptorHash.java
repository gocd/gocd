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

package com.tw.go.sample.descriptorhash;

import com.google.common.hash.*;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.annotation.Load;
import com.thoughtworks.go.plugin.api.annotation.UnLoad;
import com.thoughtworks.go.plugin.api.info.PluginContext;
import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.api.info.PluginDescriptorAware;
import com.thoughtworks.go.plugin.api.logging.Logger;

import java.util.List;

/**
 * Calculates the hash of the given plugin descriptor using
 * google guava and logs the same
 * (descriptor and hash code) to the plugin log file.
 *
 * @author Go Team
 *
 */
@Extension
public class DescriptorHash implements PluginDescriptorAware {
    Logger logger = Logger.getLoggerFor(DescriptorHash.class);
    private Hasher hasher;

    @Load
    public void onLoad(PluginContext context) {
        logger.info("Creating a hasher.....");
        HashFunction hashFunction = Hashing.md5();
        hasher = hashFunction.newHasher();
    }

    @UnLoad
    public void onUnload(PluginContext context) {
        logger.info("Nothing to do here, please unload me.....");
    }

    @Override
    public void setPluginDescriptor(PluginDescriptor descriptor) {
        HashCode hashCode = getHashOfDescriptor(descriptor);
        logger.info("-------------------------------------------------------------");
        logger.info("Descriptor: " + descriptor);
        logger.info("-------------------------------------------------------------");
        logger.info("Hash of the descriptor: " + hashCode);
        logger.info("-------------------------------------------------------------");
    }

    // -------------------------------------------------------------
    //  Hash Code Calculation methods using Google Guava.
    // -------------------------------------------------------------
    private HashCode getHashOfDescriptor(PluginDescriptor descriptor) {
        return hasher.putString(descriptor.id()).putString(descriptor.version())
                .putObject(descriptor.about(), aboutFunnel())
                .putObject(descriptor.about().vendor(), vendorFunnel())
                .putObject(descriptor.about().targetOperatingSystems(), targetOsFunnel()).hash();
    }

    private Funnel<List<String>> targetOsFunnel() {
        return new Funnel<List<String>>() {
            @Override
            public void funnel(List<String> targetOs, PrimitiveSink sink) {
                for (String os : targetOs) {
                    sink.putString(os);
                }
            }
        };
    }

    private Funnel<PluginDescriptor.Vendor> vendorFunnel() {
        return new Funnel<PluginDescriptor.Vendor>() {
            @Override
            public void funnel(PluginDescriptor.Vendor vendor, PrimitiveSink sink) {
                sink.putString(vendor.name()).putString(vendor.url());
            }
        };
    }

    private Funnel<PluginDescriptor.About> aboutFunnel() {
        return new Funnel<PluginDescriptor.About>() {
            @Override
            public void funnel(PluginDescriptor.About about, PrimitiveSink sink) {
                sink.putString(about.version()).putString(about.description()).putString(about.name())
                        .putString(about.targetGoVersion());
            }
        };
    }
}
