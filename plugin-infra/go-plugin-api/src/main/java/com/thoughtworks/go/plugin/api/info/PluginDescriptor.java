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
package com.thoughtworks.go.plugin.api.info;

import java.io.Serializable;
import java.util.List;

/**
 * Descriptor represent the plugin descriptor XML file.
 *
 * @see <a href="https://developer.gocd.org/current/writing_go_plugins/go_plugins_basics.html" target="_blank">Go Plugin Documentation</a>
 *
 * @author Go Team
 */
public interface PluginDescriptor extends Serializable {

    /**
     * Plugin Id.
     *
     * @return  a string containing the plugin id.
     */
    String id();

    /**
     * Provides version of the plugin which will be used by Go to identify the version of the plugin. It will be used in plugin upgrade flow and only integer values are allowed.
     *
     * @return  a string containing the plugin descriptor version.
     */
    String version();

    /**
     * Plugin author information.
     *
     * @return object containing the author information.
     */
    About about();

    /**
     * Plugin author information.
     *
     * @author Go Team
     */
    interface About {

        /**
         * Author name.
         *
         * @return a string containing the name.
         */
        String name();

        /**
         *  Provides version of the plugin which will be used by plugin authors to provide meaningful version for the plugin. It will be used for display purpose by Go.
         *
         * @return a string containing the plugin version.
         */
        String version();

        /**
         * Target Go version.
         *
         * @return a string containing the version of Go for which the
         * plugin is intended.
         */
        String targetGoVersion();

        /**
         * Plugin Description.
         *
         * @return a string containing the plugin description.
         */
        String description();

        /**
         * Plugin vendor details.
         *
         * @return an object containing the plugin vendor details.
         */
        Vendor vendor();

        /**
         * Operating systems on which this plugin can run.
         *
         * @return a list of valid operating system name strings.
         */
        List<String> targetOperatingSystems();
    }

    /**
     * Plugin vendor information.
     *
     */
    interface Vendor {

        /**
         * Vendor name.
         *
         * @return a string containing vendor name.
         */
        String name();

        /**
         * Vendor website URL
         *
         * @return a string containing website URL of the vendor.
         */
        String url();
    }
}
