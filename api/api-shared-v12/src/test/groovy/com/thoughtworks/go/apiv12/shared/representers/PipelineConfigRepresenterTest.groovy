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
package com.thoughtworks.go.apiv12.shared.representers

import com.thoughtworks.go.api.representers.EnvironmentVariableRepresenter
import com.thoughtworks.go.api.util.GsonTransformer
import com.thoughtworks.go.apiv12.admin.shared.representers.ParamRepresenter
import com.thoughtworks.go.apiv12.admin.shared.representers.PipelineConfigRepresenter
import com.thoughtworks.go.apiv12.admin.shared.representers.TimerRepresenter
import com.thoughtworks.go.apiv12.admin.shared.representers.materials.MaterialsRepresenter
import com.thoughtworks.go.apiv12.admin.shared.representers.stages.ConfigHelperOptions
import com.thoughtworks.go.apiv12.admin.shared.representers.stages.StageRepresenter
import com.thoughtworks.go.apiv12.admin.shared.representers.trackingtool.TrackingToolRepresenter
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.exceptions.UnprocessableEntityException
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.remote.FileConfigOrigin
import com.thoughtworks.go.helper.EnvironmentVariablesConfigMother
import com.thoughtworks.go.helper.MaterialConfigsMother
import com.thoughtworks.go.helper.StageConfigMother
import com.thoughtworks.go.security.GoCipher
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonUtils.toObject
import static com.thoughtworks.go.helper.MaterialConfigsMother.git
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.junit.jupiter.api.Assertions.*
import static org.mockito.Mockito.mock
import static org.mockito.MockitoAnnotations.initMocks

class PipelineConfigRepresenterTest {

  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  private PasswordDeserializer passwordDeserializer

  @Nested
  class Serialize {

    @Test
    void 'renders a pipeline with hal representation'() {
      def actualJson = toObject({ PipelineConfigRepresenter.toJSON(it, getPipelineConfig(), 'default') })

      assertEquals("http://test.host/go/api/admin/pipelines/wunderbar", actualJson['_links']['self']['href'])
      assertEquals("http://test.host/go/api/admin/pipelines/:pipeline_name", actualJson['_links']['find']['href'])
      assertEquals(apiDocsUrl("#pipeline-config"), actualJson['_links']['doc']['href'])

      assertThatJson(actualJson).isEqualTo(pipelineHash)
    }

    @Test
    void 'should serialize pipeline with template'() {
      def actualJson = toObject({ PipelineConfigRepresenter.toJSON(it, pipelineWithTemplate(), 'default') })
      assertThatJson(actualJson).isEqualTo(pipelineWithTemplateHash)
    }

    def pipelineWithTemplateHash =
      [
        _links               : [
          self: [
            href: 'http://test.host/go/api/admin/pipelines/wunderbar'
          ],
          doc : [
            href: apiDocsUrl('#pipeline-config')
          ],
          find: [
            href: 'http://test.host/go/api/admin/pipelines/:pipeline_name'
          ]
        ],
        label_template       : '${COUNT}',
        lock_behavior        : 'none',
        name                 : 'wunderbar',
        template             : 'template1',
        group                : 'default',
        origin               : [
          _links: [
            self: [
              href: 'http://test.host/go/admin/config_xml'
            ],
            doc : [
              href: apiDocsUrl('#get-configuration')
            ]
          ],
          type  : 'gocd'
        ],
        parameters           : [],
        environment_variables: [],
        materials            : pipelineWithTemplate().materialConfigs().collect { eachItem ->
          toObject({
            MaterialsRepresenter.toJSON(it, eachItem)
          })
        },
        stages               : null,
        tracking_tool        : null,
        timer                : null
      ]

    static def pipelineWithTemplate() {
      def pipelineConfig = new PipelineConfig(new CaseInsensitiveString('wunderbar'), '${COUNT}', null, true, MaterialConfigsMother.defaultMaterialConfigs(), new ArrayList())
      pipelineConfig.setOrigin(new FileConfigOrigin())
      pipelineConfig.setTemplateName(new CaseInsensitiveString('template1'))
      return pipelineConfig
    }
  }

  @Nested
  class Deserialize {

    @Test
    void 'should convert from minimal json to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom(pipelineHashBasic)
      def map = new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer)
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, map)

      assertEquals('wunderbar', pipelineConfig.getName().toString())
      assertTrue(pipelineConfig.getParams().isEmpty())
      assertTrue(pipelineConfig.getVariables().isEmpty())
    }

    def pipelineHashBasic =
      [
        label_template: 'foo-1.0.${COUNT}-${svn}',
        lock_behavior : 'none',
        name          : 'wunderbar',
        materials     : [
          [
            type       : 'svn',
            attributes : [
              url            : 'http://some/svn/url',
              destination    : 'svnDir',
              check_externals: false
            ],
            name       : 'http___some_svn_url',
            auto_update: true
          ]
        ],
        stages        : [
          [
            name                   : 'stage1',
            fetch_materials        : true,
            clean_working_directory: false,
            never_cleanup_artifacts: false,
            jobs                   : [
              [
                name : 'defaultJob',
                tasks: [
                  [
                    type      : 'ant',
                    attributes: [
                      working_dir: 'working-directory',
                      build_file : 'build-file',
                      target     : 'target'
                    ]
                  ]
                ]
              ]
            ]
          ]
        ]
      ]

    @Test
    void 'should convert pipeline hash with environment variables to PipelineConfig'() {

      def environmentVariables = [
        [
          name  : 'plain',
          value : 'plain',
          secure: false
        ],
        [
          secure         : true,
          name           : 'secure',
          encrypted_value: new GoCipher().encrypt('confidential')
        ]
      ]

      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        environment_variables: environmentVariables
      ])

      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      assertEquals('plain', pipelineConfig.getVariables().get(0).name)
      assertEquals('secure', pipelineConfig.getVariables().get(1).name)
    }

    @Test
    void 'should convert pipeline hash with empty environment variables to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        environment_variables: []
      ])

      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      assertEquals(0, pipelineConfig.getVariables().size())
    }

    @Test
    void 'should convert pipeline hash with parameters to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        parameters:
          [[
             name : 'command',
             value: 'echo'
           ], [
             name : 'command',
             value: 'sleep'
           ]]
      ])

      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      assertEquals('command', pipelineConfig.getParams().get(0).name)
      assertEquals('command', pipelineConfig.getParams().get(1).name)
    }

    @Test
    void 'should convert pipeline hash with empty parmas to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        parameters: null
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))

      assertEquals(0, pipelineConfig.getParams().size())
    }

    @Test
    void 'should convert pipeline hash with materials  to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        materials:
          [
            [
              type      : 'git',
              attributes:
                [
                  url             : 'http://user:password@funk.com/blank',
                  destination     : 'destination',
                  filter          :
                    [
                      ignore: ['**/*.html', '**/', 'foobar', '/']
                    ],
                  branch          : 'branch',
                  submodule_folder: 'sub_module_folder',
                  name            : 'AwesomeGitMaterial',
                  auto_update     : false
                ]
            ],
            [
              type      : 'svn',
              attributes:
                [
                  url               : 'url',
                  destination       : 'svnDir',
                  filter            :
                    [
                      ignore:
                        [
                          '*.doc'
                        ]
                    ],
                  name              : 'svn-material',
                  auto_update       : false,
                  check_externals   : true,
                  username          : 'user',
                  encrypted_password: new GoCipher().encrypt('pass')
                ]
            ]
          ]
      ])
      def map = new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer)
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, map)

      assertEquals('GitMaterial', pipelineConfig.materialConfigs().get(0).type)
      assertEquals('SvnMaterial', pipelineConfig.materialConfigs().get(1).type)
    }

    @Test
    void 'should convert pipeline hash with empty materials  to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        materials: null
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))

      assertEquals(0, pipelineConfig.materialConfigs().size())
    }

    @Test
    void 'should raise exception when passing invalid material type'() {
      def hash = pipelineHashBasic
      hash['materials'] = [[
                             type      : 'bad-material-type',
                             attributes:
                               [
                                 foo: 'bar'
                               ]
                           ]]
      def jsonReader = GsonTransformer.instance.jsonReaderFrom(hash)


      def unprocessableEntityException = assertThrows(UnprocessableEntityException.class, {
        PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      })
      assertEquals("Invalid material type bad-material-type. It has to be one of 'git, svn, hg, p4, tfs, dependency, package, plugin'.", unprocessableEntityException.getMessage())
    }

    @Test
    void 'should convert pipeline hash with stages  to PipelineConfig'() {
      def stages = [[
                      name                   : 'stage1',
                      fetch_materials        : true,
                      clean_working_directory: false,
                      never_cleanup_artifacts: false,
                      approval               :
                        [
                          type         : 'success',
                          authorization:
                            [
                              roles:
                                [],
                              users:
                                []
                            ]
                        ],
                      environment_variables  :
                        [
                          [
                            name  : 'plain',
                            value : 'plain',
                            secure: false
                          ],
                          [
                            secure         : true,
                            name           : 'secure',
                            encrypted_value: new GoCipher().encrypt('confidential')
                          ]
                        ],
                      jobs                   :
                        [[
                           name              : 'some-job',
                           run_on_all_agents : true,
                           run_instance_count: '3',
                           timeout           : '100',
                         ]]
                    ]]

      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        stages: stages
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))

      assertEquals('stage1', pipelineConfig.getStages().get(0).name().toString())
      assertEquals('some-job', pipelineConfig.getStages().first().getJobs().get(0).name().toString())
      assertEquals('plain', pipelineConfig.getStages().first().getVariables().get(0).name)
      assertEquals('secure', pipelineConfig.getStages().first().getVariables().get(1).name)
    }

    @Test
    void 'should convert pipeline hash with empty stages  to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        stages: null
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))

      assertEquals(0, pipelineConfig.getStages().size())
    }

    @Test
    void 'should convert pipeline hash with tracking tool  to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        tracking_tool:
          [
            type      : 'generic',
            attributes:
              [
                url_pattern: 'link',
                regex      : 'regex'
              ]
          ]
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      assertEquals('link', pipelineConfig.getTrackingTool().getLink())
      assertEquals('regex', pipelineConfig.getTrackingTool().getRegex())
    }

    @Test
    void 'should raise exception when passing invalid tracking tool type'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        tracking_tool:
          [
            type      : 'bad-tracking-tool',
            attributes:
              [
                link : 'link',
                regex: 'regex'
              ]
          ]
      ])
      def exception = assertThrows(UnprocessableEntityException.class, {
        PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      })
      assertEquals("Invalid Tracking tool type 'bad-tracking-tool'. It has to be one of 'generic'.", exception.getMessage())
    }

    @Test
    void 'should convert from full blown document to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom(pipelineHash)
      def map = new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer)
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, map)

      assertEquals(getPipelineConfig(), pipelineConfig)
    }

    @Test
    void 'should convert pipeline hash with timer  to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        timer:
          [
            spec           : '0 0 22 ? * MON-FRI',
            only_on_changes: true
          ]
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))

      assertTrue(pipelineConfig.getTimer().getOnlyOnChanges())
    }

    @Test
    void 'should convert pipeline hash with lock to PipelineConfig'() {
      def jsonReader = GsonTransformer.instance.jsonReaderFrom([
        lock_behavior: PipelineConfig.LOCK_VALUE_LOCK_ON_FAILURE
      ])
      def pipelineConfig = PipelineConfigRepresenter.fromJSON(jsonReader, new ConfigHelperOptions(mock(BasicCruiseConfig.class), passwordDeserializer))
      assertTrue(pipelineConfig.isLockableOnFailure())
    }

    @Test
    void 'should convert a pipeline config with a lock to a hash'() {
      def pipelineConfig = new PipelineConfig(new CaseInsensitiveString('wunderbar'), '${COUNT}', null, true, MaterialConfigsMother.defaultMaterialConfigs(), new ArrayList())
      pipelineConfig.setLockBehaviorIfNecessary(PipelineConfig.LOCK_VALUE_UNLOCK_WHEN_FINISHED)
      pipelineConfig.setOrigin(new FileConfigOrigin())
      def actualJson = toObject({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, 'default') })

      assertEquals(PipelineConfig.LOCK_VALUE_UNLOCK_WHEN_FINISHED, actualJson['lock_behavior'])
    }


    @Test
    void 'should render errors'() {
      def pipelineConfig = new PipelineConfig(new CaseInsensitiveString('wunderbar'), '', '', true, null, new ArrayList())
      pipelineConfig.setOrigin(new FileConfigOrigin())
      def config = new BasicCruiseConfig(new BasicPipelineConfigs('grp', new Authorization(), pipelineConfig))

      pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, 'grp', config, pipelineConfig))

      def actualJson = toObject({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, 'default') })
      assertThatJson(actualJson).isEqualTo(expectedHashWithErrors)
    }

    @Test
    void 'should render errors on nested objects'() {
      def pipelineConfig = getInvalidPipelineConfig()
      def config = new BasicCruiseConfig(new BasicPipelineConfigs('grp', new Authorization(), getPipelineConfig()))
      pipelineConfig.validateTree(PipelineConfigSaveValidationContext.forChain(true, 'grp', config, pipelineConfig))

      def actualJson = toObject({ PipelineConfigRepresenter.toJSON(it, pipelineConfig, 'default') })

      assertThatJson(actualJson).isEqualTo(expectedHashWithNestedErrors)
    }
  }

  def expectedHashWithErrors =
    [
      _links               : [
        self: [
          href: 'http://test.host/go/api/admin/pipelines/wunderbar'
        ],
        doc : [
          href: apiDocsUrl('#pipeline-config')
        ],
        find: [
          href: 'http://test.host/go/api/admin/pipelines/:pipeline_name'
        ]
      ],
      label_template       : '',
      lock_behavior        : 'none',
      name                 : 'wunderbar',
      template             : null,
      group                : 'default',
      origin               : [
        _links: [
          self: [
            href: 'http://test.host/go/admin/config_xml'
          ],
          doc : [
            href: apiDocsUrl('#get-configuration')
          ]
        ],
        type  : 'gocd'
      ],
      parameters           : [],
      environment_variables: [],
      materials            : [],
      stages               : null,
      tracking_tool        : null,
      timer                : [spec: '', only_on_changes: true, errors: [spec: ['Invalid cron syntax: Unexpected end of expression.']]],
      errors               : [
        materials     : ['A pipeline must have at least one material'],
        pipeline      : ["Pipeline 'wunderbar' does not have any stages configured. A pipeline must have at least one stage."],
        label_template: ["Label cannot be blank. Label should be composed of alphanumeric text, it can contain the build number as \${COUNT}, can contain a material revision as \${<material-name>} of \${<material-name>[:<number>]}, or use params as #{<param-name>}."]
      ]
    ]

  def expectedHashWithNestedErrors =
    [
      _links               : [
        self: [
          href: 'http://test.host/go/api/admin/pipelines/wunderbar'
        ],
        doc : [
          href: apiDocsUrl('#pipeline-config')
        ],
        find: [
          href: 'http://test.host/go/api/admin/pipelines/:pipeline_name'
        ]
      ],
      label_template       : 'foo-1.0.${COUNT}-${svn}',
      lock_behavior        : 'none',
      name                 : 'wunderbar',
      origin               : [
        _links: [
          self: [
            href: 'http://test.host/go/admin/config_xml'
          ],
          doc : [
            href: apiDocsUrl('#get-configuration')
          ]
        ],
        type  : 'gocd'
      ],
      template             : null,
      group                : 'default',
      parameters           : [
        [
          name  : null, value: 'echo',
          errors: [
            name: [
              "Parameter cannot have an empty name for pipeline 'wunderbar'.",
              "Invalid parameter name 'null'. This must be alphanumeric and can contain underscores, hyphens and periods (however, it cannot start with a period). The maximum allowed length is 255 characters."
            ]
          ]
        ]
      ],
      environment_variables: [[secure: false, name: '', value: '', errors: [name: ["Environment Variable cannot have an empty name for pipeline 'wunderbar'."]]]],
      materials            : [
        [
          type: 'svn', attributes: [url: 'http://some/svn/url', destination: 'svnDir', filter: null, invert_filter: false, name: 'http___some_svn_url', auto_update: true, check_externals: false]
        ],
        [
          type  : 'git', attributes: [url: null, destination: null, filter: null, invert_filter: false, name: null, auto_update: true, branch: 'master', submodule_folder: null, shallow_clone: false],
          errors: [destination: ['Destination directory is required when a pipeline has multiple SCM materials.'], url: ['URL cannot be blank']]
        ]
      ],
      stages: [[name: 'stage1', fetch_materials: true, clean_working_directory: false, never_cleanup_artifacts: false, approval: [type: 'success', allow_only_on_success: false, authorization: [roles: [], users: []]], environment_variables: [], jobs: []]],
      timer                : [spec: '0 0 22 ? * MON-FRI', only_on_changes: true],
      tracking_tool        : [
        type  : 'generic', attributes: [url_pattern: '', regex: ''],
        errors: [
          regex      : ['Regex should be populated'],
          url_pattern: ['Link should be populated', "Link must be a URL containing '\${ID}'. Go will replace the string '\${ID}' with the first matched group from the regex at run-time.", "Link must be a URL starting with https:// or http://"]

        ]
      ],
      errors               : [
        label_template: ["You have defined a label template in pipeline 'wunderbar' that refers to a material called 'svn', but no material with this name is defined."]
      ]
    ]


  static def getInvalidPipelineConfig() {
    def materialConfigs = MaterialConfigsMother.defaultMaterialConfigs()
    def git = git()
    git.setFolder(null)
    materialConfigs.add(git)

    def pipelineConfig = new PipelineConfig(new CaseInsensitiveString('wunderbar'), 'foo-1.0.${COUNT}-${svn}', '0 0 22 ? * MON-FRI', true, materialConfigs, new ArrayList())
    pipelineConfig.addParam(new ParamConfig(null, 'echo'))
    pipelineConfig.addEnvironmentVariable('', '')
    pipelineConfig.add(StageConfigMother.stageConfig('stage1'))
    pipelineConfig.setTrackingTool(new TrackingTool())
    pipelineConfig.setOrigin(new FileConfigOrigin())

    return pipelineConfig
  }


  static def getPipelineConfig() {
    def materialConfigs = MaterialConfigsMother.defaultMaterialConfigs()
    def pipelineConfig = new PipelineConfig(new CaseInsensitiveString('wunderbar'), 'foo-1.0.${COUNT}-${svn}', '0 0 22 ? * MON-FRI', true, materialConfigs, new ArrayList())
    pipelineConfig.setVariables(EnvironmentVariablesConfigMother.environmentVariables())
    pipelineConfig.addParam(new ParamConfig('COMMAND', 'echo'))
    pipelineConfig.addParam(new ParamConfig('WORKING_DIR', '/repo/branch'))
    pipelineConfig.add(StageConfigMother.stageConfigWithEnvironmentVariable('stage1'))
    pipelineConfig.setTrackingTool(new TrackingTool('link', 'regex'))
    pipelineConfig.setOrigin(new FileConfigOrigin())
    return pipelineConfig
  }

  def pipelineHash =
    [
      _links               : [
        self: [
          href: 'http://test.host/go/api/admin/pipelines/wunderbar'
        ],
        doc : [
          href: apiDocsUrl('#pipeline-config')
        ],
        find: [
          href: 'http://test.host/go/api/admin/pipelines/:pipeline_name'
        ]
      ],
      label_template       : 'foo-1.0.${COUNT}-${svn}',
      lock_behavior        : 'none',
      name                 : 'wunderbar',
      template             : null,
      group                : 'default',
      origin               : [
        _links: [
          self: [
            href: 'http://test.host/go/admin/config_xml'
          ],
          doc : [
            href: apiDocsUrl('#get-configuration')
          ]
        ],
        type  : 'gocd'
      ],
      parameters           : getPipelineConfig().getParams().collect { eachItem ->
        toObject({
          ParamRepresenter.toJSON(it, eachItem)
        })
      },
      environment_variables: getPipelineConfig().getVariables().collect { eachItem ->
        toObject({
          EnvironmentVariableRepresenter.toJSON(it, eachItem)
        })
      },
      materials            : getPipelineConfig().materialConfigs().collect { eachItem ->
        toObject({
          MaterialsRepresenter.toJSON(it, eachItem)
        })
      },
      stages               : getPipelineConfig().getStages().collect { eachItem ->
        toObject({
          StageRepresenter.toJSON(it, eachItem)
        })
      },
      tracking_tool        : toObject({ TrackingToolRepresenter.toJSON(it, getPipelineConfig()) }),
      timer                : toObject({ TimerRepresenter.toJSON(it, getPipelineConfig().getTimer()) })
    ]

}
