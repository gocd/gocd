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
package com.thoughtworks.go.spark.spa

import com.thoughtworks.go.config.exceptions.RecordNotFoundException
import com.thoughtworks.go.domain.JobInstance
import com.thoughtworks.go.server.exceptions.RulesViolationException
import com.thoughtworks.go.server.service.ElasticAgentPluginService
import com.thoughtworks.go.server.service.JobInstanceService
import com.thoughtworks.go.spark.AdminUserSecurity
import com.thoughtworks.go.spark.ControllerTrait
import com.thoughtworks.go.spark.SecurityServiceTrait
import com.thoughtworks.go.spark.mocks.StubTemplateEngine
import com.thoughtworks.go.spark.spring.SPAAuthenticationHelper
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.mockito.junit.jupiter.MockitoSettings
import org.mockito.quality.Strictness
import spark.ModelAndView

import static org.mockito.Mockito.when

@MockitoSettings(strictness = Strictness.LENIENT)
class StatusReportsControllerTest implements ControllerTrait<StatusReportsController>, SecurityServiceTrait {
  @Mock
  ElasticAgentPluginService elasticAgentPluginService

  @Mock
  JobInstanceService jobInstanceService


  @Override
  StatusReportsController createControllerInstance() {
    return new StatusReportsController(new SPAAuthenticationHelper(securityService, goConfigService), templateEngine,
      elasticAgentPluginService, jobInstanceService)
  }

  @Nested
  class PluginStatusReport {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "pluginStatusReport"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath("/some_plugin_id"))
      }
    }

    @Test
    void 'should return 404 for invalid plugin id'() {
      loginAsAdmin()
      def errorMessage = "Plugin with id 'pluginId' is not found."
      when(elasticAgentPluginService.getPluginStatusReport("pluginId"))
        .thenThrow(new RecordNotFoundException(errorMessage))

      get(controller.controllerPath("/pluginId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Plugin Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should return the plugin status report for an available plugin'() {
      loginAsAdmin()
      when(elasticAgentPluginService.getPluginStatusReport("pluginId"))
        .thenReturn("Plugin Status Report View")

      get(controller.controllerPath("/pluginId"))

      assertThatResponse()
        .isOk()
        .hasContentType("text/html; charset=utf-8")
        .hasBodyContaining("Plugin Status Report View")
    }

    @Test
    void 'should return 404 if plugin does not support plugin status_report endpoint'() {
      loginAsAdmin()
      def errorMessage = "Status Report for plugin with id: 'pluginId' is not found."
      when(elasticAgentPluginService.getPluginStatusReport("pluginId"))
        .thenThrow(new UnsupportedOperationException("Plugin does not plugin support status report."))

      get(controller.controllerPath("/pluginId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Plugin Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should return 500 in case of rules violation exception'() {
      loginAsAdmin()
      def errorMessage = "Some rules violation message"
      when(elasticAgentPluginService.getPluginStatusReport("pluginId")).thenThrow(new RulesViolationException(errorMessage))

      get(controller.controllerPath("/pluginId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Plugin Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isInternalServerError()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }
  }

  @Nested
  class AgentStatusReport {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "agentStatusReport"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath("/plugin_id/agent/elastic_agent_id"))
      }
    }

    @Test
    void 'should return 404 when plugin id is not found'() {
      loginAsAdmin()
      def errorMessage = "Plugin with id: 'pluginId' is not found."
      def jobInstance = new JobInstance()
      when(jobInstanceService.buildById(1)).thenReturn(jobInstance)
      when(elasticAgentPluginService.getAgentStatusReport("pluginId", jobInstance.getIdentifier(), "elasticAgentId"))
        .thenThrow(new RecordNotFoundException(errorMessage))

      get(controller.controllerPath("/pluginId/agent/elasticAgentId?job_id=1"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message  : errorMessage,
                                                                           viewTitle: "Agent Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should be unprocessable entity when job id is not provided'() {
      loginAsAdmin()

      get(controller.controllerPath("/pluginId/agent/elasticAgentId"))

      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType("text/html; charset=utf-8")
        .hasBodyContaining("Please provide a valid job_id for Agent Status Report.")
    }

    @Test
    void 'should be unprocessable entity when job id is not not a number'() {
      loginAsAdmin()

      get(controller.controllerPath("/pluginId/agent/elasticAgentId?job_id=foobar"))

      assertThatResponse()
        .isUnprocessableEntity()
        .hasContentType("text/html; charset=utf-8")
        .hasBodyContaining("Please provide a valid job_id for Agent Status Report.")
    }

    @Test
    void 'should be not found if plugin does not support status_report endpoint'() {
      loginAsAdmin()
      def jobInstance = new JobInstance()
      when(jobInstanceService.buildById(1)).thenReturn(jobInstance)
      when(elasticAgentPluginService.getAgentStatusReport("pluginId", jobInstance.getIdentifier(), "elasticAgentId"))
        .thenThrow(new UnsupportedOperationException(""))

      get(controller.controllerPath("/pluginId/agent/elasticAgentId?job_id=1"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView(
        [
          message  : "Status Report for plugin with id: 'pluginId' for agent 'elasticAgentId' is not found.",
          viewTitle: "Agent Status Report"
        ],
        "status_reports/error.ftlh"))

      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should return agent status report'() {
      loginAsAdmin()
      def jobInstance = new JobInstance()
      when(jobInstanceService.buildById(1)).thenReturn(jobInstance)
      when(elasticAgentPluginService.getAgentStatusReport("pluginId", jobInstance.getIdentifier(), "elasticAgentId"))
        .thenReturn("Agent Status Report View From Plugin")

      get(controller.controllerPath("/pluginId/agent/elasticAgentId?job_id=1"))

      assertThatResponse()
        .isOk()
        .hasContentType("text/html; charset=utf-8")
        .hasBodyContaining("Agent Status Report View From Plugin")
    }

    @Test
    void 'should return agent status report when unassigned'() {
      loginAsAdmin()
      def jobInstance = new JobInstance()
      when(jobInstanceService.buildById(1)).thenReturn(jobInstance)
      when(elasticAgentPluginService.getAgentStatusReport("pluginId", jobInstance.getIdentifier(), null))
        .thenReturn("Agent Status Report View From Plugin")

      get(controller.controllerPath("/pluginId/agent/unassigned?job_id=1"))

      assertThatResponse()
        .isOk()
        .hasContentType("text/html; charset=utf-8")
        .hasBodyContaining("Agent Status Report View From Plugin")
    }

    @Test
    void 'should return 500 when rules related exception occurs'() {
      loginAsAdmin()
      def errorMessage = "Some rules violation message"
      def jobInstance = new JobInstance()
      when(jobInstanceService.buildById(1)).thenReturn(jobInstance)
      when(elasticAgentPluginService.getAgentStatusReport("pluginId", jobInstance.getIdentifier(), "elasticAgentId"))
        .thenThrow(new RulesViolationException(errorMessage))

      get(controller.controllerPath("/pluginId/agent/elasticAgentId?job_id=1"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message  : errorMessage,
                                                                           viewTitle: "Agent Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isInternalServerError()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }
  }

  @Nested
  class ClusterStatusReport {
    @Nested
    class Security implements SecurityTestTrait, AdminUserSecurity {

      @Override
      String getControllerMethodUnderTest() {
        return "clusterStatusReport"
      }

      @Override
      void makeHttpCall() {
        get(controller.controllerPath("/plugin_id/cluster/cluster_profile_id"))
      }
    }

    @Test
    void 'should show cluster status report'() {
      loginAsAdmin()
      when(elasticAgentPluginService.getClusterStatusReport("pluginId", "clusterId")).thenReturn('view from plugin')
      def expectedBody = new StubTemplateEngine().render(new ModelAndView([viewTitle: "Cluster Status Report", viewFromPlugin: 'view from plugin'], "status_reports/index.ftlh"))

      get(controller.controllerPath("/pluginId/cluster/clusterId"))

      assertThatResponse()
        .isOk()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should return 404 when cluster profile is not found'() {
      loginAsAdmin()
      def errorMessage = "Cluster profile with id: 'clusterId' is not found."
      when(elasticAgentPluginService.getClusterStatusReport("pluginId", "clusterId"))
        .thenThrow(new RecordNotFoundException(errorMessage))

      get(controller.controllerPath("/pluginId/cluster/clusterId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Cluster Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should return 404 when plugin id is not found'() {
      loginAsAdmin()
      def errorMessage = "Plugin with id: 'pluginId' is not found."
      when(elasticAgentPluginService.getClusterStatusReport("pluginId", "clusterId"))
        .thenThrow(new RecordNotFoundException(errorMessage))

      get(controller.controllerPath("/pluginId/cluster/clusterId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Cluster Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should show error message when plugin does not support cluster status report'() {
      loginAsAdmin()
      def errorMessage = "Status Report for plugin with id: 'pluginId' for cluster 'clusterId' is not found."
      when(elasticAgentPluginService.getClusterStatusReport("pluginId", "clusterId"))
        .thenThrow(new UnsupportedOperationException("Plugin does not support cluster status report."))

      get(controller.controllerPath("/pluginId/cluster/clusterId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Cluster Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isNotFound()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }

    @Test
    void 'should return 500 when rules related exception occurs'() {
      loginAsAdmin()
      def errorMessage = "Some error message"
      when(elasticAgentPluginService.getClusterStatusReport("pluginId", "clusterId"))
        .thenThrow(new RulesViolationException(errorMessage))

      get(controller.controllerPath("/pluginId/cluster/clusterId"))

      def expectedBody = new StubTemplateEngine().render(new ModelAndView([message: errorMessage, viewTitle: "Cluster Status Report"],
        "status_reports/error.ftlh"))
      assertThatResponse()
        .isInternalServerError()
        .hasContentType("text/html; charset=utf-8")
        .hasBody(expectedBody)
    }
  }
}
