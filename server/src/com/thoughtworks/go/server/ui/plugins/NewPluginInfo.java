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

package com.thoughtworks.go.server.ui.plugins;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;

@Deprecated
public class NewPluginInfo {
    protected final PluginDescriptor descriptor;
    protected final String extensionName;

    public NewPluginInfo(PluginDescriptor descriptor, String extensionName) {
        this.descriptor = descriptor;
        this.extensionName = extensionName;
    }

    public PluginDescriptor getDescriptor() {
        return descriptor;
    }

    public String getExtensionName() {
        return extensionName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        NewPluginInfo that = (NewPluginInfo) o;

        if (descriptor != null ? !descriptor.equals(that.descriptor) : that.descriptor != null) return false;
        return extensionName != null ? extensionName.equals(that.extensionName) : that.extensionName == null;
    }

    @Override
    public int hashCode() {
        int result = descriptor != null ? descriptor.hashCode() : 0;
        result = 31 * result + (extensionName != null ? extensionName.hashCode() : 0);
        return result;
    }
}
