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
package com.thoughtworks.go.plugin.domain.secrets;

import com.thoughtworks.go.plugin.api.info.PluginDescriptor;
import com.thoughtworks.go.plugin.domain.common.Image;
import com.thoughtworks.go.plugin.domain.common.PluggableInstanceSettings;
import com.thoughtworks.go.plugin.domain.common.PluginConstants;
import com.thoughtworks.go.plugin.domain.common.PluginInfo;

import java.util.Objects;

public class SecretsPluginInfo extends PluginInfo {
    private final PluggableInstanceSettings secretsConfigSettings;

    public SecretsPluginInfo(PluginDescriptor descriptor, PluggableInstanceSettings secretsConfigSettings, Image image) {
        super(descriptor, PluginConstants.SECRETS_EXTENSION, null, image);

        this.secretsConfigSettings = secretsConfigSettings;
    }

    public PluggableInstanceSettings getSecretsConfigSettings() {
        return secretsConfigSettings;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SecretsPluginInfo that = (SecretsPluginInfo) o;
        return Objects.equals(secretsConfigSettings, that.secretsConfigSettings);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), secretsConfigSettings);
    }
}
