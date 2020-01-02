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
package com.thoughtworks.go.apiv6.plugininfos.representers

import com.thoughtworks.go.apiv6.plugininfos.representers.extensions.SecretsExtensionRepresenter
import com.thoughtworks.go.plugin.domain.common.PluginInfo
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.apiv6.plugininfos.representers.ExtensionRepresenterResolver.resolveRepresenterFor
import static com.thoughtworks.go.helpers.PluginInfoMother.createSecretConfigPluginInfo
import static org.assertj.core.api.Java6Assertions.assertThat

class ExtensionRepresenterResolverTest {

  @Test
  void shouldReturnSecretExtensionRepresenter() {
    PluginInfo extension = createSecretConfigPluginInfo()
    def representer = resolveRepresenterFor(extension)

    assertThat(representer).isInstanceOf(SecretsExtensionRepresenter.class)
  }
}