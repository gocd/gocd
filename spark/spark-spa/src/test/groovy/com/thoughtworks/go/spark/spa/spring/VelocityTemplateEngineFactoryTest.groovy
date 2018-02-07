/*
 * Copyright 2018 ThoughtWorks, Inc.
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

package com.thoughtworks.go.spark.spa.spring

import com.thoughtworks.go.server.service.RailsAssetsService
import com.thoughtworks.go.server.service.SecurityService
import com.thoughtworks.go.server.service.VersionInfoService
import com.thoughtworks.go.server.service.WebpackAssetsService
import com.thoughtworks.go.spark.spa.RolesControllerDelegate
import org.apache.commons.lang.StringEscapeUtils
import org.assertj.core.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import spark.ModelAndView

import static org.mockito.Mockito.mock
import static org.mockito.MockitoAnnotations.initMocks

class VelocityTemplateEngineFactoryTest {

  InitialContextProvider initialContextProvider
  private VelocityTemplateEngineFactory engine

  @BeforeEach
  void setUp() {
    initMocks(this)
    initialContextProvider = new InitialContextProvider(mock(RailsAssetsService.class), mock(WebpackAssetsService), mock(SecurityService), mock(VersionInfoService))
    engine = new VelocityTemplateEngineFactory(initialContextProvider, new DefaultResourceLoader(getClass().getClassLoader()), "classpath:velocity")
    engine.afterPropertiesSet()
  }

  @Test
  void 'it should render a basic template'() {
    def output = engine.create(RolesControllerDelegate.class, "layouts/test-layout.vm")
      .render(new ModelAndView(Collections.emptyMap(), "templates/test-template.vm"))
    Assertions.assertThat(output)
      .contains("begin parent layout")
      .contains("this is the actual template content")
      .contains("end parent layout")
  }

  @Test
  void 'it should escape html entities by default'() {
    def userInput = "<script>alert('i can has hax')</script>"
    def output = engine.create(RolesControllerDelegate.class, "layouts/test-layout.vm")
      .render(new ModelAndView(Collections.singletonMap("user-input", userInput), "templates/escape-html-entities.vm"))
    Assertions.assertThat(output)
      .contains("begin parent layout")
      .contains(StringEscapeUtils.escapeHtml(userInput))
      .contains("end parent layout")
  }
}
