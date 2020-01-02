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
package com.thoughtworks.go.spark

import com.thoughtworks.go.config.CaseInsensitiveString
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.PipelineConfigs

import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when

trait PipelinesHelperTrait {
  PipelineConfigs pipelinesFor(String... names) {
    List<PipelineConfig> configs = names.collect { String name ->
      PipelineConfig p = mock(PipelineConfig.class)
      CaseInsensitiveString ciName = new CaseInsensitiveString(name)
      when(p.name()).thenReturn(ciName)
      when(p.getName()).thenReturn(ciName)
      return p
    }

    PipelineConfigs all = mock(PipelineConfigs.class)
    when(all.getPipelines()).thenReturn(configs)

    return all
  }
}