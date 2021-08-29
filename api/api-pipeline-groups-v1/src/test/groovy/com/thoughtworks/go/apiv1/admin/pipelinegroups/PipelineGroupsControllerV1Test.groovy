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
package com.thoughtworks.go.apiv1.admin.pipelinegroups

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.apiv1.admin.pipelinegroups.representers.PipelineGroupRepresenter
import com.thoughtworks.go.apiv1.admin.pipelinegroups.representers.PipelineGroupsRepresenter
import com.thoughtworks.go.config.Authorization
import com.thoughtworks.go.config.BasicPipelineConfigs
import com.thoughtworks.go.domain.PipelineGroups
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.PipelineConfigsService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*

@MockitoSettings(strictness = Strictness.LENIENT)
class PipelineGroupsControllerV1Test implements SecurityServiceTrait, ControllerTrait<PipelineGroupsControllerV1> {


  @Mock
  private PipelineConfigsService pipelineConfigsService

  @Mock
  private PipelineConfigService pipelineConfigService

  @Mock
  private EntityHashingService entityHashingService

  @Override
  PipelineGroupsControllerV1 createControllerInstance() {
    return new PipelineGroupsControllerV1(pipelineConfigsService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, securityService)
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
      void 'should list pipeline groups'() {
        def configs = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig('pipeline1'))
        configs.setGroup("group")
        def expectedPipelineGroups = new PipelineGroups([configs])
        when(entityHashingService.hashForEntity(expectedPipelineGroups)).thenReturn("some-etag")
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(expectedPipelineGroups)

        getWithApiHeader(controller.controllerPath())

        assertThatResponse()
          .isOk()
          .hasEtag('"some-etag"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(PipelineGroupsRepresenter, expectedPipelineGroups)
      }

      @Test
      void 'should render 304 if etag matches'() {
        def configs = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig('pipeline1'))
        configs.setGroup("group")
        def expectedPipelineGroups = new PipelineGroups([configs])

        when(entityHashingService.hashForEntity(expectedPipelineGroups)).thenReturn("some-etag")
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(expectedPipelineGroups)

        getWithApiHeader(controller.controllerPath(), ['if-none-match': '"some-etag"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Create {

    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), [name: 'group'])
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
      void "should allow admin users create a new pipeline group"() {
        def pipelineGroup = new BasicPipelineConfigs()
        pipelineGroup.setGroup("new_grp")

        postWithApiHeader(controller.controllerPath(), [name: "new_grp"])

        verify(pipelineConfigsService).createGroup(any(), any(), any())
        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(PipelineGroupRepresenter, pipelineGroup)
      }

      @Test
      void "should handle server validation errors"() {
        HttpLocalizedOperationResult result
        def pipelineGroup = new BasicPipelineConfigs()
        pipelineGroup.setGroup("group")

        when(pipelineConfigsService.createGroup(any(), any(), any())).then({ InvocationOnMock invocation ->
          pipelineGroup.addError("group", "Invalid name")
          result = invocation.getArguments()[2]
          result.unprocessableEntity("message from server")
        })

        postWithApiHeader(controller.controllerPath(), [name: 'group'])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("message from server")
      }

      @Test
      void "should fail if a group by same name already exists"() {
        def pipelineGroup = new BasicPipelineConfigs()
        pipelineGroup.setGroup("group")

        when(pipelineConfigsService.getGroupsForUser(any())).thenReturn([pipelineGroup])

        postWithApiHeader(controller.controllerPath(), [name: 'group'])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add pipeline group 'group'. Another pipeline group with the same name already exists.")
      }

      @Test
      void "should fail if a group by same name different case already exists"() {
        def pipelineGroup = new BasicPipelineConfigs()
        pipelineGroup.setGroup("group")

        when(pipelineConfigsService.getGroupsForUser(any())).thenReturn([pipelineGroup])

        postWithApiHeader(controller.controllerPath(), [name: 'Group'])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add pipeline group 'Group'. Another pipeline group with the same name already exists.")
      }
    }
  }

  @Nested
  class Show {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {


      @Override
      String getControllerMethodUnderTest() {
        return "show"
      }

      @Override
      void makeHttpCall() {
        getWithApiHeader(controller.controllerPath('/group'))
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
      void 'should show pipeline config for an admin'() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig('pipeline1'))
        group.setGroup("group")
        def pipelineGroups = new PipelineGroups([group])
        def group_digest = 'digest_for_group'

        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(pipelineGroups)
        when(entityHashingService.hashForEntity(group)).thenReturn(group_digest)

        getWithApiHeader(controller.controllerPath("/group"))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(PipelineGroupRepresenter, group)
          .hasEtag('"digest_for_group"')
      }

      @Test
      void "should return 304 for show pipeline config if etag sent in request is fresh"() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig('pipeline1'))
        group.setGroup("group")
        def pipelineGroups = new PipelineGroups([group])
        def group_digest = 'digest_for_group'

        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(pipelineGroups)
        when(entityHashingService.hashForEntity(group)).thenReturn(group_digest)

        getWithApiHeader(controller.controllerPath('/group'), ['if-none-match': '"digest_for_group"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should return 404 for show pipeline config if pipeline is not found"() {
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(Collections.emptyList())

        getWithApiHeader(controller.controllerPath("/group"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("group"))
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should show pipeline config if etag sent in request is stale"() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig('pipeline1'))
        group.setGroup("group")
        def pipelineGroups = new PipelineGroups([group])
        def group_digest = 'digest_for_group'

        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(pipelineGroups)
        when(entityHashingService.hashForEntity(group)).thenReturn(group_digest)

        getWithApiHeader(controller.controllerPath('/group'), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"digest_for_group"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(PipelineGroupRepresenter, group)
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
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig('pipeline1'))
        group.setGroup("group")
        def headers = [
          'If-Match': 'cached-digest',
        ]
        putWithApiHeader(controller.controllerPath('/group'), headers, toObjectString({
          PipelineGroupRepresenter.toJSON(it, group)
        }))
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setup() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void "should update group for an admin"() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"))
        group.setGroup("group1")
        when(entityHashingService.hashForEntity(group)).thenReturn('digest')
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(new PipelineGroups([group]))
        when(pipelineConfigsService.updateGroup(any(), any(), any(), any())).thenReturn(group)

        def headers = [
          'If-Match': 'digest',
        ]

        putWithApiHeader(controller.controllerPath("/group1"), headers, toObjectString({
          PipelineGroupRepresenter.toJSON(it, group)
        }))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(PipelineGroupRepresenter, group)
      }

      @Test
      void "should not update group if etag passed does not match the one on server"() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"))
        group.setGroup("group1")
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(new PipelineGroups([group]))

        def headers = [
          'If-Match': 'old-etag',
        ]

        putWithApiHeader(controller.controllerPath("/group1"), headers, toObjectString({
          PipelineGroupRepresenter.toJSON(it, group)
        }))

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for pipeline group 'group1'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void "should not update group if no etag is passed"() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"))
        group.setGroup("group1")
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(new PipelineGroups([group]))

        putWithApiHeader(controller.controllerPath("/group1"), toObjectString({
          PipelineGroupRepresenter.toJSON(it, group)
        }))

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for pipeline group 'group1'. Please update your copy of the config with the changes and try again.")
      }

      @Test
      void "should handle server validation errors"() {
        HttpLocalizedOperationResult result
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"))
        group.setGroup("group1")
        when(entityHashingService.hashForEntity(group)).thenReturn('digest')
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(new PipelineGroups([group]))

        group.addError("authorization", "Invalid authorization")

        when(pipelineConfigsService.updateGroup(any(), any(), any(), any())).then({ InvocationOnMock invocation ->
          result = invocation.getArguments()[3]
          result.unprocessableEntity("message from server")
        })

        def headers = [
          'If-Match': 'digest',
        ]

        putWithApiHeader(controller.controllerPath("/group1"), headers, toObjectString({
          PipelineGroupRepresenter.toJSON(it, group)
        }))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("message from server")
      }

      @Test
      void "should return 404 for show pipeline config if pipeline is not found"() {
        def group = new BasicPipelineConfigs(PipelineConfigMother.pipelineConfig("pipeline1"))
        group.setGroup("group1")
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(Collections.emptyList())

        putWithApiHeader(controller.controllerPath("/group"), toObjectString({
          PipelineGroupRepresenter.toJSON(it, group)
        }))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("group"))
          .hasContentType(controller.mimeType)
      }
    }
  }

  @Nested
  class Destroy {

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "destroy"
      }

      @Override
      void makeHttpCall() {
        deleteWithApiHeader(controller.controllerPath('/foo'))
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setup() {
        enableSecurity()
        loginAsAdmin()
      }

      @Test
      void "should delete empty pipeline group for an admin"() {
        def group = new BasicPipelineConfigs("group1", new Authorization())
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(new PipelineGroups([group]))

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage("The group 'group1' was deleted successfully.")
        }).when(pipelineConfigsService).deleteGroup(any(), eq(group), any())


        deleteWithApiHeader(controller.controllerPath("/group1"))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("The group 'group1' was deleted successfully.")
      }

      @Test
      void "should render not found if the specified pipeline group is absent"() {
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(Collections.emptyList())

        deleteWithApiHeader(controller.controllerPath("/non-existent-group"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(controller.entityType.notFoundMessage("non-existent-group"))
      }

      @Test
      void "should not delete pipeline group when the group is not empty"() {
        def group = new BasicPipelineConfigs("group1", new Authorization(), PipelineConfigMother.pipelineConfig("pipeline1"))
        when(pipelineConfigsService.getGroupsForUser(currentUserLoginName().toString())).thenReturn(new PipelineGroups([group]))

        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.unprocessableEntity("Cannot delete group when not empty")
        }).when(pipelineConfigsService).deleteGroup(any(), eq(group), any())


        deleteWithApiHeader(controller.controllerPath("/group1"))

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Cannot delete group when not empty")
      }
    }
  }
}
