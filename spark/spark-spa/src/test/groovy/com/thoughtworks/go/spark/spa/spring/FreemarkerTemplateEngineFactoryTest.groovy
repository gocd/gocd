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
package com.thoughtworks.go.spark.spa.spring

import com.thoughtworks.go.server.newsecurity.utils.SessionUtils
import com.thoughtworks.go.server.security.userdetail.GoUserPrinciple
import com.thoughtworks.go.server.service.*
import com.thoughtworks.go.server.service.plugins.builder.DefaultPluginInfoFinder
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.server.service.support.toggle.Toggles
import com.thoughtworks.go.spark.spa.RolesController
import freemarker.template.utility.StringUtil
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.core.io.DefaultResourceLoader
import spark.ModelAndView

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.MockitoAnnotations.initMocks

class FreemarkerTemplateEngineFactoryTest {

  InitialContextProvider initialContextProvider
  private FreemarkerTemplateEngineFactory engine

  @BeforeEach
  void setUp() {
    initMocks(this)
    Toggles.initializeWith(mock(FeatureToggleService.class))
    initialContextProvider = new InitialContextProvider(mock(RailsAssetsService.class), mock(WebpackAssetsService), mock(SecurityService), mock(VersionInfoService), mock(DefaultPluginInfoFinder), mock(MaintenanceModeService))
    engine = new FreemarkerTemplateEngineFactory(initialContextProvider, new DefaultResourceLoader(getClass().getClassLoader()), "classpath:velocity")
    engine.afterPropertiesSet()
    SessionUtils.setCurrentUser(new GoUserPrinciple("bob", "Bob"))
  }

  @Test
  void 'it should render a basic template'() {
    def output = engine.create(RolesController.class, { return "layouts/test-layout.ftlh" })
      .render(new ModelAndView(Collections.emptyMap(), "templates/test-template.ftlh"))
    assertThat(output)
      .contains("begin parent layout")
      .contains("this is the actual template content")
      .contains("end parent layout")
  }

  @Test
  void 'it should escape html entities by default'() {
    def userInput = "<script>alert('i can has hax')</script>"
    def output = engine.create(RolesController.class, { return "layouts/test-layout.ftlh" })
      .render(new ModelAndView(Collections.singletonMap("userInput", userInput), "templates/escape-html-entities.ftlh"))

    assertThat(output)
      .contains("begin parent layout")
      .contains(StringUtil.XHTMLEnc(userInput))
      .contains("end parent layout")
  }
}
