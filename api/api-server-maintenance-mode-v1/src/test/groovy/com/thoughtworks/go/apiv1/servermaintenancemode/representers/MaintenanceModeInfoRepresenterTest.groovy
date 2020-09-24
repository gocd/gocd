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
package com.thoughtworks.go.apiv1.servermaintenancemode.representers

import com.thoughtworks.go.helper.JobInstanceMother
import com.thoughtworks.go.helper.MaterialsMother
import com.thoughtworks.go.server.domain.ServerMaintenanceMode
import com.thoughtworks.go.server.service.MaintenanceModeService
import com.thoughtworks.go.util.SystemEnvironment
import com.thoughtworks.go.util.TimeProvider
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock

import java.sql.Timestamp

import static com.thoughtworks.go.CurrentGoCDVersion.apiDocsUrl
import static com.thoughtworks.go.api.base.JsonOutputWriter.jsonDate
import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks

class MaintenanceModeInfoRepresenterTest {
  @BeforeEach
  void setUp() {
    initMocks(this)
  }

  @Mock
  TimeProvider timeProvider

  @Mock
  SystemEnvironment systemEnvironment

  @Test
  void "should represent maintenance mode info"() {
    def maintenanceModeService = new MaintenanceModeService(timeProvider, systemEnvironment)

    def gitMaterial = MaterialsMother.gitMaterial("foo/bar")
    def hgMaterial = MaterialsMother.hgMaterial()
    def svnMaterial = MaterialsMother.svnMaterial()

    def gitMaterialMDUStartTime = 10000000l
    def hgMaterialMDUStartTime = 20000000l
    def svnMaterialMDUStartTime = 30000000l

    when(timeProvider.currentTimeMillis())
      .thenReturn(gitMaterialMDUStartTime)
      .thenReturn(hgMaterialMDUStartTime)
      .thenReturn(svnMaterialMDUStartTime)

    maintenanceModeService.update(new ServerMaintenanceMode(true, "admin", new Date()))

    maintenanceModeService.mduStartedForMaterial(gitMaterial)
    maintenanceModeService.mduStartedForMaterial(hgMaterial)
    maintenanceModeService.mduStartedForMaterial(svnMaterial)

    def runningMDUs = maintenanceModeService.getRunningMDUs()
    def scheduled = JobInstanceMother.scheduled("up42_job_1")
    def building = JobInstanceMother.building("up42_job_2")

    def buildingJobs = Arrays.asList(building)
    def scheduledJobs = Arrays.asList(scheduled)

    def actualJson = toObjectString({
      MaintenanceModeInfoRepresenter.toJSON(it, maintenanceModeService.get(), true, runningMDUs, buildingJobs, scheduledJobs)
    })

    def expectedJson = [
      _links         : [
        self: [href: 'http://test.host/go/api/admin/maintenance_mode/info'],
        doc : [href: apiDocsUrl('#maintenance-mode-info')]
      ],
      "is_maintenance_mode": true,
      "metadata"     : [
        "updated_by": maintenanceModeService.get().updatedBy(),
        "updated_on": jsonDate(maintenanceModeService.get().updatedOn())
      ],
      "attributes"   : [
        "has_running_systems": false,
        "running_systems"      : [
          "material_update_in_progress": [
            [
              "type"          : "git",
              "attributes"    : [
                "url"             : "foo/bar",
                "destination"     : null,
                "filter"          : null,
                "invert_filter"   : false,
                "name"            : null,
                "auto_update"     : true,
                "branch"          : "master",
                "submodule_folder": null,
                "shallow_clone"   : false
              ],
              "mdu_start_time": "1970-01-01T02:46:40Z"
            ],
            [
              "type"          : "hg",
              "attributes"    : [
                "url"          : "hg-url",
                "destination"  : null,
                "filter"       : null,
                "invert_filter": false,
                "name"         : null,
                "auto_update"  : true
              ],
              "mdu_start_time": "1970-01-01T05:33:20Z"
            ],
            [
              "type"          : "svn",
              "attributes"    : [
                "url"               : "url",
                "destination"       : "svnDir",
                "filter"            : [
                  "ignore": ["*.doc"]
                ],
                "invert_filter"     : false,
                "name"              : null,
                "auto_update"       : true,
                "check_externals"   : true,
                "username"          : "user",
                "encrypted_password": svnMaterial.encryptedPassword
              ],
              "mdu_start_time": "1970-01-01T08:20:00Z"
            ]
          ],
          building_jobs                : [
            [
              pipeline_name   : building.pipelineName,
              pipeline_counter: building.pipelineCounter,
              stage_name      : building.stageName,
              stage_counter   : building.stageCounter,
              name            : building.name,
              state           : building.state,
              scheduled_date  : jsonDate(new Timestamp(building.getScheduledDate().getTime())),
              agent_uuid      : building.getAgentUuid()
            ]
          ],
          scheduled_jobs               : [
            [
              pipeline_name   : scheduled.pipelineName,
              pipeline_counter: scheduled.pipelineCounter,
              stage_name      : scheduled.stageName,
              stage_counter   : scheduled.stageCounter,
              name            : scheduled.name,
              state           : scheduled.state,
              scheduled_date  : jsonDate(new Timestamp(scheduled.getScheduledDate().getTime())),
              agent_uuid      : scheduled.getAgentUuid()
            ]
          ]
        ]
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }

  @Test
  void 'should not add attributes if server is not in maintenance mode'() {
    def maintenanceModeService = new MaintenanceModeService(timeProvider, systemEnvironment)

    def actualJson = toObjectString({
      MaintenanceModeInfoRepresenter.toJSON(it, maintenanceModeService.get(), false, null, null, null)
    })

    def expectedJson = [
      _links         : [
        self: [href: 'http://test.host/go/api/admin/maintenance_mode/info'],
        doc : [href: apiDocsUrl('#maintenance-mode-info')]
      ],
      "is_maintenance_mode": false,
      "metadata"     : [
        "updated_by": maintenanceModeService.get().updatedBy(),
        "updated_on": jsonDate(maintenanceModeService.get().updatedOn())
      ]
    ]

    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
