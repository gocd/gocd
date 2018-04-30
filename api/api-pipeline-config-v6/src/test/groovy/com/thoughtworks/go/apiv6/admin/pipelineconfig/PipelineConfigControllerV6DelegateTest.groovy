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

package com.thoughtworks.go.apiv6.admin.pipelineconfig

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.api.util.HaltApiMessages
import com.thoughtworks.go.apiv6.admin.pipelineconfig.representers.PipelineConfigRepresenter
import com.thoughtworks.go.config.PipelineConfig
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.git.GitMaterialConfig
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.config.remote.RepoConfigOrigin
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.service.EntityHashingService
import com.thoughtworks.go.server.service.PipelineConfigService
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.GroupAdminUserSecurity
import com.thoughtworks.go.spark.SecurityServiceTrait
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.eq
import static org.mockito.Mockito.*
import static org.mockito.MockitoAnnotations.initMocks

class PipelineConfigControllerV6DelegateTest implements SecurityServiceTrait, ControllerTrait<PipelineConfigControllerV6Delegate> {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  private PipelineConfigService pipelineConfigService

  @Mock
  private EntityHashingService entityHashingService

  @Mock
  private PasswordDeserializer passwordDeserializer


  @Override
  PipelineConfigControllerV6Delegate createControllerInstance() {
    return new PipelineConfigControllerV6Delegate(pipelineConfigService, new ApiAuthenticationHelper(securityService, goConfigService), entityHashingService, passwordDeserializer, goConfigService)
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
        getWithApiHeader(controller.controllerPath('/foo'))
      }
    }

    @Nested
    class AsAdmin {

      @BeforeEach
      void setUp() {
        enableSecurity()
        loginAsAdmin()

        when(securityService.hasViewPermissionForPipeline(any(), any())).thenReturn(true)
      }

      @Test
      void "should not show pipeline config for Non Admin users"() {
        loginAsPipelineViewUser()
        def pipeline = PipelineConfigMother.pipelineConfig('pipeline1')

        getWithApiHeader(controller.controllerPath("/pipeline1"))
        assertThatResponse()
          .hasStatus(401)
          .hasJsonMessage("You are not authorized to perform this action.")
      }

      @Test
      void 'should show pipeline config for an admin'() {
        def pipeline = PipelineConfigMother.pipelineConfig('pipeline1')
        pipeline.setOrigin(new FileConfigOrigin())
        def pipeline_md5 = 'md5_for_pipeline_config'

        when(pipelineConfigService.getPipelineConfig('pipeline1')).thenReturn(pipeline)
        when(entityHashingService.md5ForEntity(pipeline)).thenReturn(pipeline_md5)

        getWithApiHeader(controller.controllerPath("/pipeline1"))

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(pipeline, PipelineConfigRepresenter)
          .hasHeader("Etag", '"md5_for_pipeline_config"')
      }

      @Test
      void "should return 304 for show pipeline config if etag sent in request is fresh"() {
        def pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())
        def pipeline_md5 = 'md5_for_pipeline_config'

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(entityHashingService.md5ForEntity(pipeline)).thenReturn(pipeline_md5)

        getWithApiHeader(controller.controllerPath('/pipeline1'), ['if-none-match': '"md5_for_pipeline_config"'])

        assertThatResponse()
          .isNotModified()
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should return 404 for show pipeline config if pipeline is not found"() {
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)

        getWithApiHeader(controller.controllerPath("/pipeline1"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage(HaltApiMessages.notFoundMessage())
          .hasContentType(controller.mimeType)
      }

      @Test
      void "should show pipeline config if etag sent in request is stale"() {
        def pipeline = PipelineConfigMother.pipelineConfig("pipeline1")
        pipeline.setOrigin(new FileConfigOrigin())
        def pipeline_md5 = 'md5_for_pipeline_config'

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
        when(entityHashingService.md5ForEntity(pipeline)).thenReturn(pipeline_md5)

        getWithApiHeader(controller.controllerPath('/pipeline1'), ['if-none-match': '"junk"'])

        assertThatResponse()
          .isOk()
          .hasEtag('"md5_for_pipeline_config"')
          .hasContentType(controller.mimeType)
          .hasBodyWithJsonObject(pipeline, PipelineConfigRepresenter)
      }
    }
  }

  @Nested
  class Create {

    @BeforeEach
    void setUp() {
      enableSecurity()
      loginAsAdmin()

      when(securityService.hasViewPermissionForPipeline(any(), any())).thenReturn(true)
    }

    @Nested
    class Security implements SecurityTestTrait, GroupAdminUserSecurity {
      @Override
      String getControllerMethodUnderTest() {
        return "create"
      }

      @Override
      void makeHttpCall() {
        postWithApiHeader(controller.controllerPath(), "{}")

      }
    }

    @Nested
    class AsAdmin {

      @Test
      void "should not allow non admin users to create a new pipeline config"() {
        loginAsPipelineViewUser()

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: pipeline()])

        assertThatResponse()
          .hasStatus(401)
          .hasJsonMessage("You are not authorized to perform this action.")

      }

      @Test
      void "should not allow admin users of one pipeline group to create a new pipeline config in another group"() {
        loginAsGroupAdmin()
        when(securityService.isUserAdminOfGroup(any(), any())).thenReturn(false)
        when(securityService.isUserAdmin(any())).thenReturn(false)
        postWithApiHeader(controller.controllerPath(), [group: 'another_group', pipeline: pipeline()])

        assertThatResponse()
          .hasStatus(401)
          .hasJsonMessage("You are not authorized to perform this action.")
      }

      @Test
      void "should allow admin users create a new pipeline config in any group"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: pipeline()])

        verify(pipelineConfigService.createPipelineConfig(any(), any(), any(), "new_group"))
        //TODO: verify pipeline pause service
        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(pipelineConfig, PipelineConfigRepresenter)
      }

      @Test
      void "should handle server validation errors"() {
        HttpLocalizedOperationResult result
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)

//        allow(controller).to receive(: get_pipeline_from_request) do
//        controller.instance_variable_set(: @pipeline_config_from_request, @pipeline)
//        end

        when(pipelineConfigService.createPipelineConfig(any(), any(), any(), "group")).then({ InvocationOnMock invocation ->
          pipelineConfig.addError("labelTemplate", String.format(PipelineConfig.LABEL_TEMPLATE_ERROR_MESSAGE, "foo bar"))
          result = invocation.getArguments()[2]
          result.setMessage("message from server")
        })
        when(pipelineConfigService.createPipelineConfig(any(), any(), result, "group"))

        postWithApiHeader(controller.controllerPath(), [group: 'group', pipeline: invalidPipeline])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("message from server")

      }

      @Test
      void "should fail if a pipeline by same name already exists"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        postWithApiHeader(controller.controllerPath(), [group: "new_grp", pipeline: pipeline()])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Failed to add pipeline. The pipeline 'pipeline1' already exists.")
      }

      @Test
      void "should fail if group is blank"() {
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)

        postWithApiHeader(controller.controllerPath(), [group: '', pipeline: pipeline()])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Pipeline group must be specified for creating a pipeline.")
      }

//      @Test
//      void "should set package definition on to package material before save"() {
//        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
//        pipelineConfig.setOrigin(new FileConfigOrigin())
//
//        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(null)
//        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)
//        expect(@pipeline_pause_service).to receive(: pause).with("pipeline1", "Under construction", @user)
//
//        allow(@pipeline_config_service).to receive(: createPipelineConfig) do | user, pipeline, result, group |
//          pipeline_being_saved = pipeline
//        end
//
//        post_with_api_header:
//        create, : pipeline = > pipeline_with_pluggable_material("pipeline1", "package", "package-name"), : group = > "group"
//        expect(response).to be_ok
//        expect(pipeline_being_saved.materialConfigs().first().getPackageDefinition()).to eq(@repo.findPackage("package-name"))
//      }
//
//      @Test
//      void "should set scm config on to pluggable scm material before save"() {
//        login_as_pipeline_group_admin_user("group")
//        pipeline_being_saved = nil
//        expect(@pipeline_config_service).to receive(: getPipelineConfig).with(@pipeline_name).and_return(nil)
//        expect(@pipeline_config_service).to receive(: getPipelineConfig).with(@pipeline_name).and_return(@pipeline)
//        expect(@pipeline_pause_service).to receive(: pause).with("pipeline1", "Under construction", @user)
//
//        allow(@pipeline_config_service).to receive(: createPipelineConfig) do | user, pipeline, result, group |
//          pipeline_being_saved = pipeline
//        end
//
//        post_with_api_header:
//        create, : pipeline = > pipeline_with_pluggable_material("pipeline1", "plugin", "scm-id"), : group = > "group"
//        expect(response).to be_ok
//        expect(pipeline_being_saved.materialConfigs().first().getSCMConfig()).to eq(@scm)
//      }
    }

    def expectedDataWithValidationErrors =
      [
        lock_behavior        : "none",
        errors               : [label_template: ["Invalid label 'foo bar'. Label should be composed of alphanumeric text, it can contain the build number as \${COUNT}, can contain a material revision as \${<material-name>} of \${<material-name>[:<number>]}, or use params as #{<param-name>}."]],
        label_template       : "\${COUNT}",
        materials            : [[type: "svn", attributes: [url: "http://some/svn/url", destination: "svnDir", filter: null, invert_filter: false, name: "http___some_svn_url", auto_update: true, check_externals: false, username: null]]],
        name                 : "pipeline1",
        origin               : [
          _links: [
            self: [
              href: 'http://test.host/go/admin/config_xml'
            ],
            doc : [
              href: 'https://api.gocd.org/current/#get-configuration'
            ]
          ],
          type  : 'gocd'
        ],
        environment_variables: [],
        parameters           : [],
        stages               : [[name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: [type: "success", authorization: [roles: [], users: []]], environment_variables: [], jobs: []]],
        template             : null,
        timer                : null,
        tracking_tool        : null
      ]

    def invalidPipeline =
      [
        label_template: "${COUNT}",
        lock_behavior : "none",
        name          : "pipeline1",
        materials     : [
          [
            type      : "SvnMaterial",
            attributes: [
              name           : "http___some_svn_url",
              auto_update    : true,
              url            : "http://some/svn/url",
              destination    : "svnDir",
              filter         : null,
              check_externals: false,
              username       : null,
              password       : null
            ]
          ]
        ],
        stages        : [[name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: [type: "success", authorization: []], jobs: []]],
        errors        : [label_template: ["Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as \${COUNT}, can contain a material revision as \${<material-name>} of \${<material-name>[:<number>]}, or use params as #{<param-name>}."]]
      ]
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
        putWithApiHeader(controller.controllerPath('/foo'), [
          'accept'      : controller.mimeType,
          'If-Match'    : 'cached-md5',
          'content-type': 'application/json'
        ], toObjectString({ PipelineConfigRepresenter.toJSON(it, PipelineConfigMother.pipelineConfig("foo")) }))
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
      void "should not update pipeline config if the user is not admin or pipeline group admin"() {
        loginAsPipelineViewUser()

        putWithApiHeader(controller.controllerPath("pipeline1"), [pipeline: pipeline()])

        assertThatResponse()
          .hasStatus(401)
          .hasJsonMessage("You are not authorized to perform this action.")
      }

      @Test
      void "should update pipeline config for an admin"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(entityHashingService.md5ForEntity(pipelineConfig)).thenReturn('md5')
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)
        doNothing().when(pipelineConfigService.updatePipelineConfig(any(), any(), "md5", any()))

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, [pipeline: pipeline()])

        assertThatResponse()
          .isOk()
          .hasBodyWithJsonObject(pipeline, PipelineConfigRepresenter)
      }

      @Test
      void "should not update pipeline config if etag passed does not match the one on server"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'old-etag',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, [pipeline: pipeline()])

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes.")
      }

      @Test
      void "should not update pipeline config if no etag is passed"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'content-type': 'application/json'
        ]
        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, [pipeline: pipeline()])

        assertThatResponse()
          .isPreconditionFailed()
          .hasJsonMessage("Someone has modified the configuration for pipeline 'pipeline1'. Please update your copy of the config with the changes.")

      }

      @Test
      void "should not update pipeline config when the pipeline is defined remotely"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        def gitMaterial = new GitMaterialConfig("https://github.com/config-repos/repo", "master")
        def origin = new RepoConfigOrigin(new ConfigRepoConfig(gitMaterial, "json-plugib"), "revision1")
        pipelineConfig.setOrigin(origin)

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]

        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, [pipeline: pipeline()])

        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'.")
      }

      @Test
      void "should handle server validation errors"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        pipelineConfig.addError("labelTemplate", String.format(PipelineConfig.LABEL_TEMPLATE_ERROR_MESSAGE, 'foo bar'))

        when(pipelineConfigService.updatePipelineConfig(any(), any(), "md5", any())).then({ InvocationOnMock invocation ->
          result = invocation.getArguments()[2]
          result.setMessage("message from server")
        })

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]


        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, [pipeline: invalidPipeline])

        assertThatResponse()
          .hasJsonMessage("message from server")

        def actualJson = toObjectString({ PipelineConfigRepresenter.toJSON(it, pipelineConfig) })
        assertThatJson(actualJson).isEqualTo(expectedDataWithValidationErrors)
      }

      @Test
      void "should not allow renaming a pipeline"() {
        def pipelineConfig = PipelineConfigMother.pipelineConfig("pipeline1")
        pipelineConfig.setOrigin(new FileConfigOrigin())
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipelineConfig)

        def headers = [
          'accept'      : controller.mimeType,
          'If-Match'    : 'md5',
          'content-type': 'application/json'
        ]


        putWithApiHeader(controller.controllerPath("/pipeline1"), headers, [pipeline: pipeline("renamed_pipeline")])

        assertThatResponse()
          .hasStatus(406)
          .hasJsonMessage("Renaming the pipeline resource is not supported by this API.")
      }

//      void "should set package definition on to package material before save"() {
//        expect(@pipeline_config_service).to receive(: getPipelineConfig).twice.with(@pipeline_name).and_return(@pipeline)
//        pipeline_being_saved = nil
//        allow(@pipeline_config_service).to receive(: updatePipelineConfig) do | user, pipeline, result |
//          pipeline_being_saved = pipeline
//        end
//        controller.request.env['HTTP_IF_MATCH'] =
//        @latest_etag
//
//          put_with_api_header: update, pipeline_name:
//        @pipeline_name, : pipeline = > pipeline_with_pluggable_material("pipeline1", "package", "package-name")
//
//        expect(response).to be_ok
//        expect(pipeline_being_saved.materialConfigs().first().getPackageDefinition()).to eq(@repo.findPackage("package-name"))
//      }
//
//      void "should set scm config on to pluggable scm material before save"() {
//        pipeline_being_saved = nil
//        expect(@pipeline_config_service).to receive(: getPipelineConfig).twice.with(@pipeline_name).and_return(@pipeline)
//
//        allow(@pipeline_config_service).to receive(: updatePipelineConfig) do | user, pipeline, result |
//          pipeline_being_saved = pipeline
//        end
//        controller.request.env['HTTP_IF_MATCH'] =
//        @latest_etag
//
//          put_with_api_header: update, pipeline_name:
//        @pipeline_name, : pipeline = > pipeline_with_pluggable_material("pipeline1", "plugin", "scm-id")
//        expect(response).to be_ok
//        expect(pipeline_being_saved.materialConfigs().first().getSCMConfig()).to eq(@scm)
//      }

    }

    def expectedDataWithValidationErrors =
      [
        lock_behavior        : "none",
        errors               : [label_template: ["Invalid label 'foo bar'. Label should be composed of alphanumeric text, it can contain the build number as \${COUNT}, can contain a material revision as \${<material-name>} of \${<material-name>[:<number>]}, or use params as #{<param-name>}."]],
        label_template       : "\${COUNT}",
        materials            : [[type: "svn", attributes: [url: "http://some/svn/url", destination: "svnDir", filter: null, invert_filter: false, name: "http___some_svn_url", auto_update: true, check_externals: false, username: null]]],
        name                 : "pipeline1",
        origin               : [
          _links: [
            self: [
              href: 'http://test.host/go/admin/config_xml'
            ],
            doc : [
              href: 'https://api.gocd.org/current/#get-configuration'
            ]
          ],
          type  : 'gocd'
        ],
        environment_variables: [],
        parameters           : [],
        stages               : [[name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: [type: "success", authorization: [roles: [], users: []]], environment_variables: [], jobs: []]],
        template             : null,
        timer                : null,
        tracking_tool        : null
      ]

    def invalidPipeline =
      [
        label_template: "${COUNT}",
        lock_behavior : "none",
        name          : "pipeline1",
        materials     : [
          [
            type      : "SvnMaterial",
            attributes: [
              name           : "http___some_svn_url",
              auto_update    : true,
              url            : "http://some/svn/url",
              destination    : "svnDir",
              filter         : null,
              check_externals: false,
              username       : null,
              password       : null
            ]
          ]
        ],
        stages        : [[name: "mingle", fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: [type: "success", authorization: []], jobs: []]],
        errors        : [label_template: ["Invalid label. Label should be composed of alphanumeric text, it should contain the builder number as \${COUNT}, can contain a material revision as \${<material-name>} of \${<material-name>[:<number>]}, or use params as #{<param-name>}."]]
      ]

  }




  @Nested
  class Destroy {
    private PipelineConfig pipeline = PipelineConfigMother.pipelineConfig("pipeline1");

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
        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)
      }

      @Test
      void "should delete pipeline config for an admin"() {
        doAnswer({ InvocationOnMock invocation ->
          HttpLocalizedOperationResult result = invocation.arguments.last()
          result.setMessage("The pipeline 'pipeline1' was deleted successfully.")
        }).when(pipelineConfigService).deletePipelineConfig(any(), eq(this.pipeline), any())


        deleteWithApiHeader(controller.controllerPath("/pipeline1"))

        assertThatResponse()
          .isOk()
          .hasJsonMessage("The pipeline 'pipeline1' was deleted successfully.")
      }

      @Test
      void "should render not found if the specified pipeline is absent"() {
        when(pipelineConfigService.getPipelineConfig("non-existent-pipeline")).thenReturn(null)

        deleteWithApiHeader(controller.controllerPath("/non-existent-pipeline"))

        assertThatResponse()
          .isNotFound()
          .hasJsonMessage("Either the resource you requested was not found, or you are not authorized to perform this action.")
      }

      @Test
      void "should not delete pipeline config when the pipeline is defined remotely"() {
        def pipeline = PipelineConfigMother.pipelineConfig("pipeline1")

        def gitMaterial = new GitMaterialConfig("https://github.com/config-repos/repo", "master")
        def origin = new RepoConfigOrigin(new ConfigRepoConfig(gitMaterial, "json-plugin"), "revision1")
        pipeline.setOrigin(origin)

        when(pipelineConfigService.getPipelineConfig("pipeline1")).thenReturn(pipeline)

        deleteWithApiHeader(controller.controllerPath("/pipeline1"))
        assertThatResponse()
          .isUnprocessableEntity()
          .hasJsonMessage("Can not operate on pipeline 'pipeline1' as it is defined remotely in 'https://github.com/config-repos/repo at revision1'.")
      }
    }
  }

  def pipeline(pipeline_name = "pipeline1", material_type = "hg", task_type = "exec") {
    return [
      label_template       : "Jyoti-\${COUNT}",
      lock_behavior        : "none",
      name                 : pipeline_name,
      template_name        : null,
      parameters           : [],
      environment_variables: [],
      materials            :
        [[
           type       : material_type,
           attributes :
             [
               url        : "../manual-testing/ant_hg/dummy",
               destination: "dest_dir",
               filter     :
                 [
                   ignore: []
                 ]
             ],
           name       : "dummyhg",
           auto_update: true
         ]],
      stages               :
        [[
           name                   : "up42_stage",
           fetch_materials        : true,
           clean_working_directory: false,
           never_cleanup_artifacts: false,
           approval               :
             [
               type         : "success",
               authorization:
                 [
                   roles: [],
                   users: []
                 ]
             ],
           environment_variables  : [],
           jobs                   :
             [[
                name                 : "up42_job",
                run_on_all_agents    : false,
                environment_variables:
                  [],
                resources            :
                  [],
                tasks                :
                  [[
                     type      : task_type,
                     attributes:
                       [
                         command    : "ls",
                         working_dir: null
                       ],
                     run_if    : []
                   ]],
                tabs                 : [],
                artifacts            : [],
                properties           : []
              ]]
         ]],
      mingle               :
        [
          base_url               : null,
          project_identifier     : null,
          mql_grouping_conditions: null
        ]
    ]
  }
}
