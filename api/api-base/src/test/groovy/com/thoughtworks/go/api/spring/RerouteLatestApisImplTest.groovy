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
package com.thoughtworks.go.api.spring

import com.thoughtworks.go.api.ApiVersion
import com.thoughtworks.go.config.exceptions.HttpException
import com.thoughtworks.go.http.mocks.HttpRequestBuilder
import com.thoughtworks.go.http.mocks.MockHttpServletResponse
import com.thoughtworks.go.server.service.support.toggle.FeatureToggleService
import com.thoughtworks.go.spark.SparkController
import com.thoughtworks.go.spark.mocks.TestApplication
import com.thoughtworks.go.spark.mocks.TestSparkPreFilter
import com.thoughtworks.go.spark.spring.RouteEntry
import com.thoughtworks.go.spark.spring.RouteInformationProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.mockito.Mock
import org.springframework.context.ApplicationContext
import spark.*
import spark.servlet.SparkFilter

import javax.servlet.FilterConfig
import java.util.stream.Collectors

import static org.assertj.core.api.Assertions.assertThat
import static org.junit.jupiter.api.Assertions.assertEquals
import static org.mockito.Mockito.*
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

    @Mock
    FeatureToggleService features

    @Mock
    ApplicationContext context

    private SparkController controller1
    private SparkController controller2
    private SparkController controller3

    private TestApplication testApplication
    private TestSparkPreFilter preFilter
    private RouteInformationProvider routeInformationProvider

    @BeforeEach
    void setUp() {
        initMocks(this)
        routeInformationProvider = new RouteInformationProvider()

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
    }


    @AfterEach
    void tearDown() {
        if (preFilter != null) {
            preFilter.destroy()
        }
        testApplication = null
        preFilter = null
    }

    private void createApplication(SparkController... controllers) {
        testApplication = new TestApplication(controllers)
        def filterConfig = mock(FilterConfig.class)
        when(filterConfig.getInitParameter(SparkFilter.APPLICATION_CLASS_PARAM)).thenReturn(TestApplication.class.getName())

        preFilter = new TestSparkPreFilter(testApplication)
        preFilter.init(filterConfig)
    }

    @Nested
    class Standard {
        @BeforeEach
        void setup() {
            createApplication(controller1, controller2, controller3)
        }

        @Test
        void routeAssertions() {
            routeInformationProvider.cacheRouteInformation()
            assertThat(routeInformationProvider.routes).hasSize(16)
        }

        @Test
        void shouldRegisterLatestRoutes() {
            // original route list
            RerouteLatestApisImpl rerouteLatestApis = new RerouteLatestApisImpl(routeInformationProvider, features)
            rerouteLatestApis.setApplicationContext(context)
            when(context.getBeansWithAnnotation()).thenReturn(Collections.emptyMap())

            routeInformationProvider.cacheRouteInformation()
            assertThat(routeInformationProvider.routes).hasSize(16)

            rerouteLatestApis.registerLatest()
            // add 5 routes for `/foo/bar`
            // add 3 routes for `/something-else/bar`
            assertThat(routeInformationProvider.routes).hasSize(24)
        }
    }

    @Nested
    class RouteToggles {
        private builder = new HttpRequestBuilder()

        @BeforeEach
        void setup() {
            controller1 = new TestControllerV1()
            controller2 = new TestControllerV2()
            controller3 = new Controller(ApiVersion.v5) {
                @Override
                String controllerBasePath() {
                    return "/foo"
                }

                @Override
                void setupRoutes() {
                    path(controllerPath(), { ->
                        get("/bar", v.mimeType(), route)
                    })
                }
            }
            createApplication(controller1, controller2, controller3)
        }

        @Test
        void 'ignores annotated controllers when calculating the latest version route alias if matching toggle is off'() {
            RerouteLatestApisImpl rerouteLatestApis = new RerouteLatestApisImpl(routeInformationProvider, features)
            rerouteLatestApis.setApplicationContext(context)
            when(context.getBeansWithAnnotation(ToggleRegisterLatest.class)).thenReturn(
                    Collections.singletonMap(TestControllerV2.class.name, controller2)
            )

            when(features.isToggleOn("testv2")).thenReturn(false)

            routeInformationProvider.cacheRouteInformation()

            List<RouteEntry> routes = routeInformationProvider.routes

            // 3 of these are boilerplate routes
            // 2 from TestController v1
            // 2 from TestController v2
            // 1 from the inlined controller
            assertEquals(8, routes.size())

            // All the routes are registered
            assertEquals(1, find(routes, "/foo/bar", ApiVersion.v5).size())
            assertEquals(1, find(routes, "/in_development", ApiVersion.v1).size())
            assertEquals(1, find(routes, "/in_development/show/:id", ApiVersion.v1).size())
            assertEquals(1, find(routes, "/in_development", ApiVersion.v2).size())
            assertEquals(1, find(routes, "/in_development/show/:id", ApiVersion.v2).size())

            rerouteLatestApis.registerLatest()

            // add latest aliases 1 route for inlined
            // add latest aliases 2 routes for TestController v1
            assertEquals(11, routes.size())

            verify(features, times(4)).isToggleOn("testv2")

            // Verify we have latest entries
            assertEquals(1, find(routes, "/foo/bar", ApiVersion.LATEST_VERSION_MIMETYPE).size())
            assertEquals(1, find(routes, "/in_development", ApiVersion.LATEST_VERSION_MIMETYPE).size())
            assertEquals(1, find(routes, "/in_development/show/:id", ApiVersion.LATEST_VERSION_MIMETYPE).size())

            // Assert the aliases point to expected versions
            assertEquals("v5", sendGet("/foo/bar", ApiVersion.LATEST_VERSION_MIMETYPE))
            assertEquals("v1", sendGet("/in_development", ApiVersion.LATEST_VERSION_MIMETYPE))
            assertEquals("v1", sendGet("/in_development/show/:id", ApiVersion.LATEST_VERSION_MIMETYPE))
        }

        @Test
        void 'considers annotated controllers when calculating the latest version route alias if matching toggle is on'() {
            RerouteLatestApisImpl rerouteLatestApis = new RerouteLatestApisImpl(routeInformationProvider, features)
            rerouteLatestApis.setApplicationContext(context)
            when(context.getBeansWithAnnotation(ToggleRegisterLatest.class)).thenReturn(
                    Collections.singletonMap(TestControllerV2.class.name, controller2)
            )

            when(features.isToggleOn("testv2")).thenReturn(true)

            routeInformationProvider.cacheRouteInformation()

            List<RouteEntry> routes = routeInformationProvider.routes

            // 3 of these are boilerplate routes
            // 2 from TestController v1
            // 2 from TestController v2
            // 1 from the inlined controller
            assertEquals(8, routes.size())

            // All the routes are registered
            assertEquals(1, find(routes, "/foo/bar", ApiVersion.v5).size())
            assertEquals(1, find(routes, "/in_development", ApiVersion.v1).size())
            assertEquals(1, find(routes, "/in_development/show/:id", ApiVersion.v1).size())
            assertEquals(1, find(routes, "/in_development", ApiVersion.v2).size())
            assertEquals(1, find(routes, "/in_development/show/:id", ApiVersion.v2).size())

            rerouteLatestApis.registerLatest()

            // add latest aliases 1 route for inlined
            // add latest aliases 2 routes for TestController v1
            assertEquals(11, routes.size())

            verify(features, times(4)).isToggleOn("testv2")

            // Verify we have latest entries
            assertEquals(1, find(routes, "/foo/bar", ApiVersion.LATEST_VERSION_MIMETYPE).size())
            assertEquals(1, find(routes, "/in_development", ApiVersion.LATEST_VERSION_MIMETYPE).size())
            assertEquals(1, find(routes, "/in_development/show/:id", ApiVersion.LATEST_VERSION_MIMETYPE).size())

            // Assert the aliases point to expected versions
            assertEquals("v5", sendGet("/foo/bar", ApiVersion.LATEST_VERSION_MIMETYPE))
            assertEquals("v2", sendGet("/in_development", ApiVersion.LATEST_VERSION_MIMETYPE))
            assertEquals("v2", sendGet("/in_development/show/:id", ApiVersion.LATEST_VERSION_MIMETYPE))
        }

        private List<RouteEntry> find(List<RouteEntry> all, String path, ApiVersion v) {
            return find(all, path, v.mimeType())
        }

        private List<RouteEntry> find(List<RouteEntry> all, String path, String mime) {
            return all.stream().filter({ e ->
                (path == e.path && mime == e.acceptedType)
            }).collect(Collectors.toList())
        }

        private String sendGet(String path, String mime) {
            def req = builder.withMethod("get").withHeader("accept", mime).withPath(path).build()
            def res = new MockHttpServletResponse()
            preFilter.doFilter(req, res, null)
            return res.contentAsString
        }
    }

    private static abstract class Controller implements SparkController {
        ApiVersion v
        /** Do NOT use a groovy closure here. It will fail to invoke. */
        final Route route = new Route() {
            @Override
            Object handle(Request request, Response response) throws Exception {
                return v.name()
            }
        }

        Controller(ApiVersion v) {
            this.v = v
        }
    }

    private static class TestControllerV1 extends Controller {
        TestControllerV1() {
            this(ApiVersion.v1)
        }

        protected TestControllerV1(ApiVersion v) {
            super(v)
        }

        @Override
        String controllerBasePath() {
            return "/in_development"
        }

        @Override
        void setupRoutes() {
            path(controllerPath(), { ->
                get("", v.mimeType(), this.route)
                get("/show/:id", v.mimeType(), this.route)
            })
        }
    }

    @ToggleRegisterLatest(controllerPath = "/in_development", apiVersion = ApiVersion.v2, as = "testv2")
    private static class TestControllerV2 extends TestControllerV1 {
        TestControllerV2() {
            super(ApiVersion.v2)
        }
    }
}
