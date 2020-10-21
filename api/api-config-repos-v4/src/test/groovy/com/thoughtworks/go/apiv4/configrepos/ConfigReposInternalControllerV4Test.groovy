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
package com.thoughtworks.go.apiv4.configrepos

import com.thoughtworks.go.api.SecurityTestTrait
import com.thoughtworks.go.api.spring.ApiAuthenticationHelper
import com.thoughtworks.go.config.*
import com.thoughtworks.go.config.materials.PasswordDeserializer
import com.thoughtworks.go.config.materials.git.GitMaterial
import com.thoughtworks.go.config.materials.mercurial.HgMaterialConfig
import com.thoughtworks.go.config.policy.Allow
import com.thoughtworks.go.config.policy.Policy
import com.thoughtworks.go.config.remote.ConfigRepoConfig
import com.thoughtworks.go.config.remote.ConfigReposConfig
import com.thoughtworks.go.config.remote.PartialConfig
import com.thoughtworks.go.domain.materials.MaterialConfig
import com.thoughtworks.go.domain.materials.Modification
import com.thoughtworks.go.domain.materials.ValidationBean
import com.thoughtworks.go.helper.PipelineConfigMother
import com.thoughtworks.go.server.domain.Username
import com.thoughtworks.go.server.materials.MaterialUpdateService
import com.thoughtworks.go.server.service.*
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.NormalUserSecurity
import com.thoughtworks.go.spark.Routes
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.util.SystemEnvironment
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.invocation.InvocationOnMock

import static com.thoughtworks.go.helper.MaterialConfigsMother.hg
import static com.thoughtworks.go.server.util.DigestMixin.digestMany
import static org.mockito.ArgumentMatchers.any
import static org.mockito.ArgumentMatchers.anyString
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class ConfigReposInternalControllerV4Test implements SecurityServiceTrait, ControllerTrait<ConfigReposInternalControllerV4> {
    private static final String TEST_PLUGIN_ID = "test.configrepo.plugin"
    private static final String TEST_REPO_URL = "https://fakeurl.com"
    private static final String ID_1 = "repo-01"
    private static final String ID_2 = "repo-02"

    @Mock
    ConfigRepoService service

    @Mock
    GoConfigRepoConfigDataSource dataSource

    @Mock
    MaterialUpdateService materialUpdateService

    @Mock
    MaterialConfigConverter converter

    @Mock
    private EnvironmentConfigService environmentConfigService

    @Mock
    private PipelineConfigsService pipelineConfigsService

    @Mock
    private PasswordDeserializer passwordDeserializer

    @Mock
    private MaterialConfigConverter materialConfigConverter

    @Mock
    private SystemEnvironment systemEnvironment

    @Mock
    private SecretParamResolver secretParamResolver

    @Mock
    private EntityHashingService entityHashingService

    @BeforeEach
    void setUp() {
        initMocks(this)
        Policy directives = new Policy()
        directives.add(new Allow("administer", "config_repo", "repo-*"))
        RoleConfig roleConfig = new RoleConfig(new CaseInsensitiveString("role"), new Users(), directives)

        when(goConfigService.rolesForUser(any())).then({ InvocationOnMock invocation ->
            CaseInsensitiveString username = invocation.getArguments()[0] as CaseInsensitiveString
            if (username == Username.ANONYMOUS.username) {
                return []
            }
            return [roleConfig]
        })
        when(environmentConfigService.getEnvironmentNames()).thenReturn([])
        when(pipelineConfigsService.getGroupsForUser(anyString())).thenReturn([])
    }

    @Override
    ConfigReposInternalControllerV4 createControllerInstance() {
        new ConfigReposInternalControllerV4(new ApiAuthenticationHelper(securityService, goConfigService), service, dataSource, materialUpdateService, converter, environmentConfigService, pipelineConfigsService, goConfigService, passwordDeserializer, materialConfigConverter, systemEnvironment, secretParamResolver, entityHashingService)
    }

    @Nested
    class IndexSecurity implements SecurityTestTrait, NormalUserSecurity {
        @Override
        String getControllerMethodUnderTest() {
            return "listRepos"
        }

        @Override
        void makeHttpCall() {
            getWithApiHeader(controller.controllerBasePath(), [:])
        }
    }

    @Nested
    class Index {

        @BeforeEach
        void setup() {
            loginAsUser()
        }

        @Test
        void 'should list only those existing config repos, with associated parse results, for which the user has permission'() {
            Modification modification = new Modification()
            //noinspection GrDeprecatedAPIUsage
            modification.setRevision("abc")

            PartialConfig partialConfig = new PartialConfig()
            PartialConfigParseResult result = PartialConfigParseResult.parseSuccess(modification, partialConfig)

            ConfigReposConfig repos = new ConfigReposConfig(repo(ID_1), repo(ID_2), repo("test-id"))
            when(service.getConfigRepos()).thenReturn(repos)
            when(dataSource.getLastParseResult(repos.get(0).getRepo())).thenReturn(null)
            when(dataSource.getLastParseResult(repos.get(1).getRepo())).thenReturn(result)

            getWithApiHeader(controller.controllerBasePath())

            assertThatResponse()
                    .isOk()
                    .hasJsonBody([
                            _links         : [
                                    self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
                            ],
                            _embedded      : [
                                    config_repos: [
                                            expectedRepoJson(ID_1, null, null, false),
                                            expectedRepoJson(ID_2, "abc", null, false)
                                    ]
                            ],
                            auto_completion: [
                                    [key: "pipeline", value: []],
                                    [key: "environment", value: []],
                                    [key: "pipeline_group", value: []]
                            ]
                    ])
        }

        @Test
        void 'should return empty list if the user does not have permission to view any config repos'() {
            ConfigReposConfig repos = new ConfigReposConfig(repo("test-id"), repo("another-id"))
            when(service.getConfigRepos()).thenReturn(repos)

            getWithApiHeader(controller.controllerBasePath())

            def expectedJson = [
                    _links         : [
                            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
                    ],
                    _embedded      : [
                            config_repos: []
                    ],
                    auto_completion: [
                            [key: "pipeline", value: []],
                            [key: "environment", value: []],
                            [key: "pipeline_group", value: []]
                    ]
            ]
            assertThatResponse()
                    .isOk()
                    .hasEtag("\"${digestMany([] as String[])}\"")
                    .hasJsonBody(expectedJson)
        }

        @Test
        void 'should set autocompletion values'() {
            ConfigReposConfig repos = new ConfigReposConfig(repo("test-id"), repo("another-id"))

            when(service.getConfigRepos()).thenReturn(repos)
            when(environmentConfigService.getEnvironmentNames()).thenReturn(Arrays.asList("env1", "env2"))
            when(pipelineConfigsService.getGroupsForUser(anyString())).thenReturn(Arrays.asList(new BasicPipelineConfigs("grp1", new Authorization(), PipelineConfigMother.pipelineConfig("pipeline"))))

            getWithApiHeader(controller.controllerBasePath())

            def expectedJson = [
                    _links         : [
                            self: [href: "http://test.host/go$Routes.ConfigRepos.BASE".toString()]
                    ],
                    _embedded      : [
                            config_repos: []
                    ],
                    auto_completion: [
                            [
                                    key  : "pipeline",
                                    value: ["pipeline"]
                            ],
                            [
                                    key  : "environment",
                                    value: ["env1", "env2"]
                            ],
                            [
                                    key  : "pipeline_group",
                                    value: ["grp1"]
                            ]
                    ]
            ]

            assertThatResponse()
                    .isOk()
                    .hasJsonBody(expectedJson)
        }
    }

    static Map expectedRepoJson(String id, String revision, String error, boolean isInProgress) {
        return [
                _links                     : [
                        self: [href: "http://test.host/go${Routes.ConfigRepos.id(id)}".toString()],
                        doc : [href: Routes.ConfigRepos.DOC],
                        find: [href: "http://test.host/go${Routes.ConfigRepos.find()}".toString()],
                ],

                id                         : id,
                plugin_id                  : TEST_PLUGIN_ID,
                material                   : [
                        type      : "hg",
                        attributes: [
                                name       : null,
                                url        : "${TEST_REPO_URL}/$id".toString(),
                                auto_update: true
                        ]
                ],
                configuration              : [],
                rules                      : [],
                material_update_in_progress: isInProgress,
                parse_info                 : null == revision ? [:] : [
                        error                     : error,
                        good_modification         : [
                                "username"     : null,
                                "email_address": null,
                                "revision"     : revision,
                                "comment"      : null,
                                "modified_time": null
                        ],
                        latest_parsed_modification: [
                                "username"     : null,
                                "email_address": null,
                                "revision"     : revision,
                                "comment"      : null,
                                "modified_time": null
                        ]
                ]
        ]
    }


    @Nested
    class CheckConnection {
        @BeforeEach
        void setUp() {
            loginAsAdmin()
        }

        @Test
        void 'should render error if material type is invalid'() {
            postWithApiHeader(controller.controllerPath(ID_1, 'material_test'), ["type": "some-random-type"])
            assertThatResponse()
                    .isUnprocessableEntity()
                    .hasJsonMessage("Your request could not be processed. Invalid material type 'some-random-type'. It has to be one of [git, hg, svn, p4, tfs, dependency, package, plugin].")
        }

        @Test
        void 'should render error if material type does not support check connection functionality'() {
            postWithApiHeader(controller.controllerPath(ID_1, 'material_test'), ["type": "dependency"])
            assertThatResponse()
                    .isUnprocessableEntity()
                    .hasJsonMessage("Your request could not be processed. The material of type 'dependency' does not support connection testing.")
        }

        @Test
        void 'should render error if material is not valid'() {
            postWithApiHeader(controller.controllerPath(ID_1, 'material_test'), [
                    "type"      : "git",
                    "attributes": [
                            "url"               : "",
                            "encrypted_password": "encrypted-password",
                            "password"          : "password"
                    ]
            ])

            def expectedJSON = [
                    "message": "There was an error with the material configuration.\n- url: URL cannot be blank",
                    "data"   : [
                            "errors"    : [
                                    "url": ["URL cannot be blank"]
                            ],
                            "type"      : "git",
                            "attributes": [
                                    "url"             : "",
                                    "destination"     : null,
                                    "filter"          : null,
                                    "invert_filter"   : false,
                                    "name"            : null,
                                    "auto_update"     : true,
                                    "branch"          : "master",
                                    "submodule_folder": null,
                                    "shallow_clone"   : false
                            ]
                    ]
            ]

            assertThatResponse()
                    .isUnprocessableEntity()
                    .hasJsonMessage("There was an error with the material configuration.\\n- url: URL cannot be blank")
                    .hasJsonBody(expectedJSON)
        }

        @Test
        void 'should render error if cannot connect to material'() {
            def material = mock(GitMaterial.class)
            def validationBean = ValidationBean.notValid("some-error")
            when(material.checkConnection(any())).thenReturn(validationBean)
            when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

            postWithApiHeader(controller.controllerPath(ID_1, 'material_test'), [
                    "type"      : "git",
                    "attributes": [
                            "url": "some-url"
                    ]
            ])

            assertThatResponse()
                    .isUnprocessableEntity()
                    .hasJsonMessage("some-error")
        }

        @Test
        void 'should render success response if can connect to material'() {
            def material = mock(GitMaterial.class)
            def validationBean = ValidationBean.valid()
            when(material.checkConnection(any())).thenReturn(validationBean)
            when(materialConfigConverter.toMaterial(any(MaterialConfig.class))).thenReturn(material)

            postWithApiHeader(controller.controllerPath(ID_1, 'material_test'), [
                    "type"      : "git",
                    "attributes": [
                            "url": "some-url"
                    ]
            ])

            assertThatResponse()
                    .isOk()
                    .hasJsonMessage("Connection OK.")
        }
    }

    static ConfigRepoConfig repo(String id) {
        HgMaterialConfig materialConfig = hg("${TEST_REPO_URL}/$id", "")
        ConfigRepoConfig repo = ConfigRepoConfig.createConfigRepoConfig(materialConfig, TEST_PLUGIN_ID, id)

        return repo
    }

}
