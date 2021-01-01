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
package com.thoughtworks.go.apiv2.securityauthconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.SecurityAuthConfig
import com.thoughtworks.go.plugin.domain.common.ValidationResult
import com.thoughtworks.go.plugin.domain.common.VerifyConnectionResponse
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.SecurityAuthConfigService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother.create
import static com.thoughtworks.go.spark.Routes.SecurityAuthConfigAPI.VERIFY_CONNECTION
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class SecurityAuthConfigInternalControllerV2Test implements SecurityServiceTrait, ControllerTrait<SecurityAuthConfigInternalControllerV2> {

  @Mock
  private SecurityAuthConfigService securityAuthConfigService

  @Mock
  private EntityHashingService entityHashingService

  @BeforeEach
  void setup() {
    initMocks(this)
  }

  @Override
  SecurityAuthConfigInternalControllerV2 createControllerInstance() {
    new SecurityAuthConfigInternalControllerV2(securityAuthConfigService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService)
  }

  @Nested
  class VerifyConnection {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'verifyConnection'
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), [:])
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
      void "should return 200 on successful verification"() {
        def authConfig = new SecurityAuthConfig("file", "cd.go.authorization.file", create("Path", "/var/lib/pass.prop"))
        def jsonPayload = [
          id                               : 'file',
          plugin_id                        : "cd.go.authorization.file",
          "allow_only_known_users_to_login": false,
          properties                       : [
            [
              "key"  : "Path",
              "value": "/var/lib/pass.prop"
            ]
          ]]

        when(securityAuthConfigService.verifyConnection(authConfig)).thenReturn(new VerifyConnectionResponse("success", "Ok", new ValidationResult()))

        postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), jsonPayload)

        def expected = ["status"     : "success",
                        "message"    : "Ok",
                        "auth_config": [
                          "_links"                         : [
                            "self": ["href": "http://test.host/go/api/admin/security/auth_configs/file"],
                            "doc" : ["href": apiDocsUrl("#auth_configs")],
                            "find": ["href": "http://test.host/go/api/admin/security/auth_configs/:id"]
                          ],
                          "id"                             : "file",
                          "plugin_id"                      : "cd.go.authorization.file",
                          "allow_only_known_users_to_login": false,
                          "properties"                     : [["key": "Path", "value": "/var/lib/pass.prop"]]
                        ]]

        assertThatResponse()
          .isOk()
          .hasJsonBody(expected)
      }

      @Test
      void "should return 422 with errors on verify connection failure"() {
        def authConfig = new SecurityAuthConfig("file", "cd.go.authorization.file")
        def jsonPayload = [
          id        : 'file',
          plugin_id : "cd.go.authorization.file",
          properties: []
        ]

        when(securityAuthConfigService.verifyConnection(authConfig)).then({ InvocationOnMock invocation ->
          SecurityAuthConfig config = invocation.getArguments()[0]
          config.addError("Path", "Must not be blank.")
          return new VerifyConnectionResponse("failure", "Verify connection failed", new ValidationResult())
        })

        postWithApiHeader(controller.controllerPath(VERIFY_CONNECTION), jsonPayload)

        def expected = ["status"     : "failure",
                        "message"    : "Verify connection failed",
                        "auth_config": [
                          "_links"                         : [
                            "self": ["href": "http://test.host/go/api/admin/security/auth_configs/file"],
                            "doc" : ["href": apiDocsUrl("#auth_configs")],
                            "find": ["href": "http://test.host/go/api/admin/security/auth_configs/:id"]
                          ],
                          "id"                             : "file",
                          "plugin_id"                      : "cd.go.authorization.file",
                          "allow_only_known_users_to_login": false,
                          "properties"                     : [],
                          "errors"                         : ["Path": ["Must not be blank."]]
                        ]]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonBody(expected)
      }
    }
  }
}
