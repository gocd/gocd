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
package com.thoughtworks.go.apiv3.datasharing.usagedata.representers

import com.thoughtworks.go.server.domain.UsageStatistics
import org.junit.jupiter.api.Test

import static com.thoughtworks.go.api.base.JsonUtils.toObjectString
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson

class UsageStatisticsRepresenterTest {

  @Test
  void "should represent usage statistics"() {
    def usageStatistics = UsageStatistics.newUsageStatistics()
      .pipelineCount(100l)
      .configRepoPipelineCount(25l)
      .agentCount(10l)
      .oldestPipelineExecutionTime(1527244129553)
      .serverId("server-id")
      .jobCount(15l)
      .elasticAgentPluginToJobCount([ecs: 10L, docker: 5L])
      .gocdVersion("18.7.0")
      .testDrive(false)
      .addCTA(false)
      .saveAndRunCTA(false)
      .build()

    def actualJson = toObjectString({ UsageStatisticsRepresenter.toJSON(it, usageStatistics) })
    def expectedJson = [
      "server_id"      : "server-id",
      "message_version": 3,
      "data"           : [
        pipeline_count                : 100,
        config_repo_pipeline_count    : 25,
        agent_count                   : 10,
        job_count                     : 15,
        elastic_agent_job_count       : [
          [
            plugin_id: "ecs",
            job_count: 10
          ],
          [
            plugin_id: "docker",
            job_count: 5
          ]
        ],
        oldest_pipeline_execution_time: 1527244129553,
        test_drive_instance           : false,
        test_drive_metrics            : [
          add_pipeline_cta: false,
          save_and_run_cta: false
        ],
        gocd_version                  : "18.7.0"
      ]
    ]
    assertThatJson(actualJson).isEqualTo(expectedJson)
  }
}
