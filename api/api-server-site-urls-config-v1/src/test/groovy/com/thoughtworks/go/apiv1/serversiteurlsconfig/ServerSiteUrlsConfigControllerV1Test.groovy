/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.apiv1.serversiteurlsconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.serversiteurlsconfig.representers.ServerSiteUrlsConfigRepresenter
import com.thoughtworks.go.config.BasicCruiseConfig
import com.thoughtworks.go.config.SiteUrls
import com.thoughtworks.go.config.exceptions.GoConfigInvalidException
import com.thoughtworks.go.domain.SecureSiteUrl
import com.thoughtworks.go.domain.SiteUrl
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.ServerConfigService
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class ServerSiteUrlsConfigControllerV1Test implements SecurityServiceTrait, ControllerTrait<ServerSiteUrlsConfigControllerV1> {

  @Mock
  ServerConfigService serverConfigService

  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  ServerSiteUrlsConfigControllerV1 createControllerInstance() {
    new ServerSiteUrlsConfigControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, serverConfigService)
  }

  @Nested
  class Index {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

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
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should return server site urls config'() {
        def siteUrls = new SiteUrls()
        when(serverConfigService.getServerSiteUrls()).thenReturn(siteUrls)
        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ ServerSiteUrlsConfigRepresenter.toJSON(it, siteUrls) }))

        verify(serverConfigService, times(1)).getServerSiteUrls()
        verifyNoMoreInteractions(serverConfigService)
      }
    }
  }

  @Nested
  class Create {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "createOrUpdate"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerBasePath(), [])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should create server site urls config with specified urls'() {
        def siteUrls = new SiteUrls(new SiteUrl("http://foo"), new SecureSiteUrl("https://foo"))

        def jsonPayload = ["site_url"       : "http://foo",
                           "secure_site_url": "https://foo"]

        when(serverConfigService.getServerSiteUrls()).thenReturn(siteUrls)
        postWithApiHeader(controller.controllerBasePath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ ServerSiteUrlsConfigRepresenter.toJSON(it, siteUrls) }))
      }

      @Test
      void 'should return error when server site url is invalid'() {
        def jsonPayload = ['site_url'       : 'foo',
                           'secure_site_url': 'https://foo']

        when(serverConfigService.createOrUpdateServerSiteUrls(Mockito.any() as SiteUrls)).thenThrow(new RuntimeException("failed to save : # anon _ site urlsite urls is invalid. foo should conform to the pattern - (https?://.+)?"))

        postWithApiHeader(controller.controllerBasePath(), jsonPayload)


        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("failed to save : # anon _ site urlsite urls is invalid. foo should conform to the pattern - (https?://.+)?")
      }

      @Test
      void 'should return error when secure site url is invalid'() {
        def jsonPayload = ['site_url'       : 'http://foo',
                           'secure_site_url': 'foo']

        when(serverConfigService.createOrUpdateServerSiteUrls(Mockito.any() as SiteUrls)).thenThrow(new RuntimeException("failed to save : # anon _ secure site urlsite urls is invalid. foo should conform to the pattern - (https://.+)?"))

        postWithApiHeader(controller.controllerBasePath(), jsonPayload)


        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("failed to save : # anon _ secure site urlsite urls is invalid. foo should conform to the pattern - (https://.+)?")
      }
    }
  }

  @Nested
  class Update {
    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "createOrUpdate"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerBasePath(), [])
      }
    }

    @Nested
    class AsAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void 'should update server site urls config with specified urls'() {
        def siteUrls = new SiteUrls(new SiteUrl("http://foo"), new SecureSiteUrl("https://foo"))

        def jsonPayload = ["site_url"       : "http://foo",
                           "secure_site_url": "https://foo"]

        when(serverConfigService.getServerSiteUrls()).thenReturn(siteUrls)
        putWithApiHeader(controller.controllerBasePath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasBodyWithJson(toObjectString({ ServerSiteUrlsConfigRepresenter.toJSON(it, siteUrls) }))
      }

      @Test
      void 'should not update and return error when server site url is invalid'() {
        def jsonPayload = ['site_url'       : 'foo',
                           'secure_site_url': 'https://foo']

        when(serverConfigService.createOrUpdateServerSiteUrls(Mockito.any() as SiteUrls)).thenThrow(new GoConfigInvalidException(new BasicCruiseConfig(), "Invalid format for site url."))

        putWithApiHeader(controller.controllerBasePath(), jsonPayload)


        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Invalid format for site url.")
      }

      @Test
      void 'should not update and return error when secure site url is invalid'() {
        def jsonPayload = ['site_url'       : 'http://foo',
                           'secure_site_url': 'foo']

        when(serverConfigService.createOrUpdateServerSiteUrls(Mockito.any() as SiteUrls)).thenThrow(new GoConfigInvalidException(new BasicCruiseConfig(), "Invalid format for secure site url. 'http://foo.bar' must start with https"))

        putWithApiHeader(controller.controllerBasePath(), jsonPayload)


        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Invalid format for secure site url. 'http://foo.bar' must start with https")
      }
    }
  }
}