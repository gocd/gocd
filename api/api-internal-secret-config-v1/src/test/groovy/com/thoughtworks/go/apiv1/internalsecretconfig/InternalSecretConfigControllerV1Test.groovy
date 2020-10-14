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

package com.thoughtworks.go.apiv1.internalsecretconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.internalsecretconfig.models.SecretConfigsViewModel
import com.thoughtworks.go.apiv1.internalsecretconfig.representers.SecretConfigsViewModelRepresenter
import com.thoughtworks.go.config.SecretConfig
import com.thoughtworks.go.config.SecretConfigs
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.packagerepository.PackageRepositories
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.domain.scm.SCMs
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EnvironmentConfigService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.SecretConfigService
import com.thoughtworks.go.server.service.materials.PackageRepositoryService
import com.thoughtworks.go.server.service.materials.PluggableScmService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.domain.packagerepository.PackageRepositoryMother.create
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig
import static org.mockito.ArgumentMatchers.any
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class InternalSecretConfigControllerV1Test implements SecurityServiceTrait, ControllerTrait<InternalSecretConfigControllerV1> {
  @Mock
  private SecretConfigService secretConfigService
  @Mock
  private PipelineConfigService pipelineConfigService
  @Mock
  private EnvironmentConfigService environmentConfigService
  @Mock
  private PluggableScmService pluggableScmService
  @Mock
  private PackageRepositoryService packageRepositoryService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  InternalSecretConfigControllerV1 createControllerInstance() {
    new InternalSecretConfigControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), secretConfigService,
      pipelineConfigService, environmentConfigService, pluggableScmService, packageRepositoryService)
  }

  @Nested
  class Index {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "index"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerBasePath())
      }
    }

    @Nested
    class AsAdmin {
      SecretConfigsViewModel model

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        def groups = new PipelineGroups()
        groups.addPipeline("group1", pipelineConfig("pipeline1"))
        when(pipelineConfigService.viewableGroupsFor(any(Username.class))).thenReturn(groups)
        when(environmentConfigService.getEnvironmentNames()).thenReturn(["env1"])
        when(pluggableScmService.listAllScms()).thenReturn(new SCMs(SCMMother.create("scm1")))
        when(packageRepositoryService.getPackageRepositories()).thenReturn(new PackageRepositories(create("pkg-repo1")))

        model = new SecretConfigsViewModel()
        model.getAutoSuggestions().put("pipeline_group", ["group1"])
        model.getAutoSuggestions().put("environment", ["env1"])
        model.getAutoSuggestions().put("pluggable_scm", ["scm-scm1"])
        model.getAutoSuggestions().put("package_repository", ["repo-pkg-repo1"])
      }

      @Test
      void 'should list all secrets configs with etag header'() {
        def expectedConfigs = new SecretConfigs(new SecretConfig("ForDeploy", "file",
          ConfigurationPropertyMother.create("username", false, "Jane"),
          ConfigurationPropertyMother.create("password", true, "Doe")
        ))
        model.setSecretConfigs(expectedConfigs)

        when(secretConfigService.getAllSecretConfigs()).thenReturn(expectedConfigs)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigsViewModelRepresenter.class, model)
      }

      @Test
      void 'should return 304 if secret configs are not modified since last request'() {
        when(secretConfigService.getAllSecretConfigs()).thenReturn(new SecretConfigs())
        when(pipelineConfigService.viewableGroupsFor(any(Username.class))).thenReturn(new PipelineGroups())
        when(environmentConfigService.getEnvironmentNames()).thenReturn([])
        when(pluggableScmService.listAllScms()).thenReturn(new SCMs())
        when(packageRepositoryService.getPackageRepositories()).thenReturn(new PackageRepositories())

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"4366029eac9405b2e8cfbf9f2c328ff11cd1a6056b3f7acdc3fba8d7b2244e78"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
          .hasNoBody()
      }

      @Test
      void 'should return empty list if there are no secrets configs'() {
        def expectedConfigs = new SecretConfigs()
        when(secretConfigService.getAllSecretConfigs()).thenReturn(expectedConfigs)
        model.setSecretConfigs(expectedConfigs)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(SecretConfigsViewModelRepresenter.class, model)
      }
    }
  }
}
