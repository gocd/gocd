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
package com.thoughtworks.go.api.spring

import com.thoughtworks.go.config.exceptions.HttpException
import com.thoughtworks.go.spark.SparkController
import com.thoughtworks.go.spark.mocks.TestApplication
import com.thoughtworks.go.spark.mocks.TestSparkPreFilter
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mock
import spark.ExceptionHandler
import spark.Filter
import spark.Route
import spark.servlet.SparkFilter

import javax.servlet.FilterConfig

import static org.assertj.core.api.Assertions.assertThat
import static org.mockito.Mockito.mock
import static org.mockito.Mockito.when
import static org.mockito.MockitoAnnotations.initMocks
import static spark.Spark.*

class RerouteLatestApisImplTest {

  @Mock
  Filter apiv11BeforeFilter1
  @Mock
  Filter apiv11BeforeFilter2
  @Mock
  Filter apiv11AfterFilter1
  @Mock
  Filter apiv11AfterFilter2

  @Mock
  ExceptionHandler<HttpException> apiv11ExceptionHandler1
  @Mock
  Route apiv11GetMethod

  @Mock
  Filter apiv10BeforeFilter1
  @Mock
  Filter apiv10BeforeFilter2
  @Mock
  Filter apiv10AfterFilter1
  @Mock
  Filter apiv10AfterFilter2
  @Mock
  ExceptionHandler<HttpException> apiv10ExceptionHandler1
  @Mock
  Route apiv10GetMethod

  @Mock
  Filter apiv1BeforeFilter1
  @Mock
  Filter apiv1BeforeFilter2
  @Mock
  Route apiv1GetMethod

  @Mock
  ExceptionHandler<Exception> apiv1ExceptionHandler1

  private SparkController controller1
  private SparkController controller2
  private SparkController controller3

  private TestApplication testApplication
  private TestSparkPreFilter preFilter

  @BeforeEach
  void setUp() {
    initMocks(this)

    controller1 = new SparkController() {
      @Override
      String controllerBasePath() {
        return "/foo"
      }

      @Override
      void setupRoutes() {
        path(controllerBasePath(), { ->
          // some filters
          before("/bar", "application/vnd.go.cd." + "v11" + "+json", apiv11BeforeFilter1)
          before("/bar", "application/vnd.go.cd." + "v11" + "+json", apiv11BeforeFilter2)

          get("/bar", "application/vnd.go.cd." + "v11" + "+json", apiv11GetMethod)

          after("/bar", "application/vnd.go.cd." + "v11" + "+json", apiv11AfterFilter1)
          after("/bar", "application/vnd.go.cd." + "v11" + "+json", apiv11AfterFilter2)

          // some exception handlers
          exception(HttpException.class, apiv11ExceptionHandler1)
        })
      }
    }

    controller2 = new SparkController() {
      @Override
      String controllerBasePath() {
        return "/foo"
      }

      @Override
      void setupRoutes() {
        path(controllerBasePath(), { ->
          // some filters
          before("/bar", "application/vnd.go.cd." + "v10" + "+json", apiv10BeforeFilter1)
          before("/bar", "application/vnd.go.cd." + "v10" + "+json", apiv10BeforeFilter2)

          get("/bar", "application/vnd.go.cd." + "v10" + "+json", apiv10GetMethod)

          after("/bar", "application/vnd.go.cd." + "v10" + "+json", apiv10AfterFilter2)
          after("/bar", "application/vnd.go.cd." + "v10" + "+json", apiv10AfterFilter1)

          // some exception handlers
          exception(HttpException.class, apiv10ExceptionHandler1)
        })
      }
    }

    controller3 = new SparkController() {
      @Override
      String controllerBasePath() {
        return "/something-else"
      }

      @Override
      void setupRoutes() {
        path(controllerBasePath(), { ->
          // some filters
          before("/bar", "application/vnd.go.cd." + "v1" + "+json", apiv1BeforeFilter1)
          before("/bar", "application/vnd.go.cd." + "v1" + "+json", apiv1BeforeFilter2)

          get("/bar", "application/vnd.go.cd." + "v1" + "+json", apiv1GetMethod)

          // some exception handlers
          exception(Exception.class, apiv1ExceptionHandler1)
        })
      }
    }

    testApplication = new TestApplication(controller1, controller2, controller3)

    def filterConfig = mock(FilterConfig.class)
    when(filterConfig.getInitParameter(SparkFilter.APPLICATION_CLASS_PARAM)).thenReturn(TestApplication.class.getName())

    preFilter = new TestSparkPreFilter(testApplication)
    preFilter.init(filterConfig)
  }

  @AfterEach
  void tearDown() {
    preFilter.destroy()
  }

  @Test
  void routeAssertions() {
    assertThat(new RerouteLatestApisImpl().routeList()).hasSize(16)
  }

  @Test
  void shouldRegisterLatestRoutes() {
    // original route list
    RerouteLatestApisImpl rerouteLatestApis = new RerouteLatestApisImpl()
    assertThat(rerouteLatestApis.routeList()).hasSize(16)

    rerouteLatestApis.registerLatest()

    // add 5 routes for `/foo/bar`
    // add 3 routes for `/something-else/bar`
    assertThat(rerouteLatestApis.routeList()).hasSize(24)
  }
}
