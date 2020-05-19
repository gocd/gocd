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
package com.thoughtworks.go.apiv1.scms

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.scms.representers.SCMRepresenter
import com.thoughtworks.go.apiv1.scms.representers.SCMsRepresenter
import com.thoughtworks.go.domain.config.Configuration
import com.thoughtworks.go.domain.packagerepository.ConfigurationPropertyMother
import com.thoughtworks.go.domain.scm.SCM
import com.thoughtworks.go.domain.scm.SCMMother
import com.thoughtworks.go.domain.scm.SCMs
import com.thoughtworks.go.security.GoCipher
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.materials.PluggableScmService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.server.service.result.LocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.DeprecatedApiTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import groovy.json.JsonBuilder
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.util.HaltApiMessages.etagDoesNotMatch
import static com.thoughtworks.go.api.util.HaltApiMessages.renameOfEntityIsNotSupportedMessage
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class SCMControllerV1Test implements SecurityServiceTrait, ControllerTrait<SCMControllerV1>, DeprecatedApiTrait {
  @Mock
  PluggableScmService scmService

  @Mock
  EntityHashingService entityHashingService

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Override
  SCMControllerV1 createControllerInstance() {
    return new SCMControllerV1(new ApiAuthenticationHelper(securityService, goConfigService), scmService, entityHashingService)
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
        getWithApiHeader(controller.controllerPath())
      }
    }

    @Nested
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should list all SCMs'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        SCMs scms = new SCMs(scm)

        when(entityHashingService.hashForEntity(scms)).thenReturn("some-etag")
        when(scmService.listAllScms()).thenReturn(scms)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"some-etag"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(scms, SCMsRepresenter)
      }

      @Test
      void 'should render 304 if etag matches'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        SCMs scms = new SCMs(scm)

        when(entityHashingService.hashForEntity(scms)).thenReturn("some-etag")
        when(scmService.listAllScms()).thenReturn(scms)

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"some-etag"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return 'show'
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath("/gitDeploy"))
      }
    }

    @Nested
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should return scm with specified material_name'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        when(entityHashingService.hashForEntity(scm)).thenReturn('digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(scm);

        getWithApiHeader(controller.controllerPath('/foobar'))

        assertThatResponse()
          .isOk()
          .hasEtag('"digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(scm, SCMRepresenter)
      }

      @Test
      void 'should return 404 if scm with specified name does not exist'() {
        getWithApiHeader(controller.controllerPath('/barbaz'))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("barbaz"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 304 if scm is not modified'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        when(entityHashingService.hashForEntity(scm)).thenReturn('digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(scm);

        getWithApiHeader(controller.controllerPath('/foobar'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void 'should return 200 with scm if etag does not match'() {
        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        when(entityHashingService.hashForEntity(scm)).thenReturn('digest-new')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(scm)

        getWithApiHeader(controller.controllerPath('/foobar'), ['if-none-match': '"digest"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest-new"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(scm, SCMRepresenter)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), '{}')
      }
    }

    @Nested
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should create scm from given json payload'() {
        def jsonPayload = [
          "id"             : "1",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "value1"
            ], [
              "key"            : "key2",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        SCM scm = SCMMother.create("1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        when(scmService.listAllScms()).thenReturn(new SCMs())
        when(entityHashingService.hashForEntity(Mockito.any() as SCM)).thenReturn('some-digest')

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(scm, SCMRepresenter)
      }


      @Test
      void 'should generate id if not specified in json payload'() {
        def jsonPayload = [
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "value1"
            ], [
              "key"            : "key2",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        when(scmService.listAllScms()).thenReturn(new SCMs())
        when(entityHashingService.hashForEntity(Mockito.any() as SCM)).thenReturn('some-digest')

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"some-digest"')
          .hasContentType(controller.mimeType)

        assertThatJson(response.getContentAsString()).hasProperty("id")
      }

      @Test
      void 'should not create scm if one already exist with same name'() {
        def jsonPayload = [
          "id"             : "scm1",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "value1"
            ], [
              "key"            : "key2",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        SCM existingSCM = SCMMother.create("scm1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        when(entityHashingService.hashForEntity(Mockito.any() as SCM)).thenReturn('some-digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(existingSCM)

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "Failed to add scm 'foobar'. Another scm with the same name already exists.",
          data   : [
            id             : "scm1",
            name           : "foobar",
            auto_update    : true,
            plugin_metadata: [
              "id"     : "plugin1",
              "version": "v1.0"
            ],
            "configuration": [
              [
                "key"  : "key1",
                "value": "value1"
              ],
              [
                "key"            : "key2",
                "encrypted_value": new GoCipher().encrypt("secret")
              ]
            ],
            "errors"       : [
              "name": ["SCM name should be unique. SCM with name 'foobar' already exists."]
            ]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }

      @Test
      void 'should not create scm in case of validation error and return the scm with errors'() {
        def jsonPayload = [
          "id"             : "scm1",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "value1"
            ], [
              "key"            : "key2",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        when(scmService.listAllScms()).thenReturn(new SCMs())
        when(scmService.createPluggableScmMaterial(Mockito.any() as Username, Mockito.any() as SCM, Mockito.any() as LocalizedOperationResult))
          .then({ InvocationOnMock invocation ->
          SCM scm = invocation.getArguments()[1]
          scm.addError("id", "Invalid id specified")
          HttpLocalizedOperationResult result = invocation.getArguments().last()
          result.unprocessableEntity("validation failed")
        })

        postWithApiHeader(controller.controllerPath(), jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id               : "scm1",
            "name"           : "foobar",
            "auto_update"    : true,
            "plugin_metadata": [
              "id"     : "plugin1",
              "version": "v1.0"
            ],
            "configuration"  : [
              [
                "key"  : "key1",
                "value": "value1"
              ], [
                "key"            : "key2",
                "encrypted_value": new GoCipher().encrypt("secret")
              ]
            ],
            errors           : [id: ["Invalid id specified"]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }
    }
  }

  @Nested
  class Update {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "update"
      }

      @Override
      void makeHttpCall() {
        putWithApiHeader(controller.controllerPath('/foobar'), '{}')
      }
    }

    @Nested
    class AsGroupAdmin {
      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsGroupAdmin()
      }

      @Test
      void 'should update scm if etag matches'() {
        SCM existingSCM = SCMMother.create("scm1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        SCM updatedSCM = SCMMother.create("scm1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "updated_value"),
          ConfigurationPropertyMother.create("key3", true, "secret"),
        ))

        def jsonPayload = [
          "id"             : "scm1",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "updated_value"
            ], [
              "key"            : "key3",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        when(entityHashingService.hashForEntity(existingSCM)).thenReturn('some-digest')
        when(entityHashingService.hashForEntity(updatedSCM)).thenReturn('new-digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(existingSCM)

        putWithApiHeader(controller.controllerPath("/foobar"), ['if-match': 'some-digest'], jsonPayload)

        assertThatResponse()
          .isOk()
          .hasEtag('"new-digest"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(updatedSCM, SCMRepresenter)
      }

      @Test
      void 'should not update scm if etag does not match'() {
        SCM existingSCM = SCMMother.create("scm1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        def jsonPayload = [
          "id"             : "scm1",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "updated_value"
            ], [
              "key"            : "key3",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        when(entityHashingService.hashForEntity(existingSCM)).thenReturn('some-digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(existingSCM)

        putWithApiHeader(controller.controllerPath("/foobar"), ['if-match': 'wrong-digest'], jsonPayload)

        assertThatResponse()
          .isPreconditionFailed()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(etagDoesNotMatch("scm", "scm1"))
      }

      @Test
      void 'should return 404 if the scm does not exist'() {
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(null)
        putWithApiHeader(controller.controllerPath("/foobar"), ['if-match': 'wrong-digest'], [:])

        assertThatResponse()
          .isNotFound()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(controller.entityType.notFoundMessage("foobar"))
      }

      @Test
      void 'should return 422 if attempted rename'() {
        SCM existingSCM = SCMMother.create("scm1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        def jsonPayload = [
          "id"             : "scm11",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "updated_value"
            ], [
              "key"            : "key3",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        when(entityHashingService.hashForEntity(existingSCM)).thenReturn('some-digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(existingSCM)

        putWithApiHeader(controller.controllerPath("/foobar"), ['if-match': 'some-digest'], jsonPayload)

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonMessage(renameOfEntityIsNotSupportedMessage('scm'))
      }

      @Test
      void 'should return 422 for validation error'() {
        SCM existingSCM = SCMMother.create("scm1", "foobar", "plugin1", "v1.0", new Configuration(
          ConfigurationPropertyMother.create("key1", false, "value1"),
          ConfigurationPropertyMother.create("key2", true, "secret"),
        ))

        def jsonPayload = [
          "id"             : "scm1",
          "name"           : "foobar",
          "auto_update"    : true,
          "plugin_metadata": [
            "id"     : "plugin1",
            "version": "v1.0"
          ],
          "configuration"  : [
            [
              "key"  : "key1",
              "value": "value1"
            ], [
              "key"            : "key2",
              "encrypted_value": new GoCipher().encrypt("secret")
            ]
          ]
        ]

        when(entityHashingService.hashForEntity(existingSCM)).thenReturn('some-digest')
        when(scmService.findPluggableScmMaterial("foobar")).thenReturn(existingSCM)

        when(scmService.updatePluggableScmMaterial(
          Mockito.any() as Username,
          Mockito.any() as SCM,
          Mockito.any() as LocalizedOperationResult,
          Mockito.any() as String)
        ).then(
          {
            InvocationOnMock invocation ->
              SCM scm = invocation.getArguments()[1]
              scm.addError("id", "Invalid id specified.")
              HttpLocalizedOperationResult result = invocation.getArguments()[2]
              result.unprocessableEntity("validation failed")
          })

        putWithApiHeader(controller.controllerPath("/foobar"), ['if-match': 'some-digest'], jsonPayload)

        def expectedResponseBody = [
          message: "validation failed",
          data   : [
            id               : "scm1",
            "name"           : "foobar",
            "auto_update"    : true,
            "plugin_metadata": [
              "id"     : "plugin1",
              "version": "v1.0"
            ],
            "configuration"  : [
              [
                "key"  : "key1",
                "value": "value1"
              ], [
                "key"            : "key2",
                "encrypted_value": new GoCipher().encrypt("secret")
              ]
            ],
            errors           : [id: ["Invalid id specified."]]
          ]
        ]

        assertThatResponse()
          .isUnprocessableEntity()
          .hasContentType(controller.mimeType)
          .hasJsonBody(new JsonBuilder(expectedResponseBody).toString())
      }
    }
  }
}
