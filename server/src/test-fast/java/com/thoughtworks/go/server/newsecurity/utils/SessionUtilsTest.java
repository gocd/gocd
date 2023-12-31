/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.server.newsecurity.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SessionUtilsTest {

    @Nested
    class PluginAuthSessionTests {

        @Mock
        private HttpServletRequest mockRequest;

        @Mock
        private HttpSession mockSession;

        @BeforeEach
        void beforeEach() {
            when(mockRequest.getSession()).thenReturn(mockSession);
        }

        @Test
        void shouldSetAuthSessionContext_CorrectlyBuildingKeyUsingThePluginsId() {
            String pluginId = "my-brilliant-plugin";
            Map<String, String> authSessionContext = Map.of(
                "foo", "bar",
                "apple", "banana"
            );

            SessionUtils.setPluginAuthSessionContext(mockRequest, pluginId, authSessionContext);

            verify(mockSession).setAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin", authSessionContext);
        }

        @Test
        void shouldRemoveSessionContext_CorrectlyBuildingKeyUsingThePluginsId() {
            String pluginId = "my-brilliant-plugin";

            SessionUtils.removePluginAuthSessionContext(mockRequest, pluginId);

            verify(mockSession).removeAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin");
        }

        @Nested
        class GetPluginAuthSessionContextTests {

            @Test
            void shouldGetAuthSessionContext_CorrectlyBuildingKeyUsingThePluginsId() {
                String pluginId = "my-brilliant-plugin";
                Map<String, String> authSessionContext = Map.of(
                    "foo", "bar",
                    "apple", "banana"
                );

                when(mockSession.getAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin")).thenReturn(authSessionContext);

                Map<String, String> result = SessionUtils.getPluginAuthSessionContext(mockRequest, pluginId);

                assertThat(result).isEqualTo(authSessionContext);
            }

            @Test
            void shouldGetAuthSessionContext_ReturningAnEmptyMap_WhenSerialisedContextIsNull() {
                String pluginId = "my-brilliant-plugin";

                when(mockSession.getAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin")).thenReturn(null);

                Map<String, String> result = SessionUtils.getPluginAuthSessionContext(mockRequest, pluginId);

                assertThat(result).isEmpty();
            }

            @Test
            void shouldGetAuthSessionContext_ReturningAnEmptyMap_WhenSerialisedContextIsNotAMap() {
                String pluginId = "my-brilliant-plugin";

                when(mockSession.getAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin")).thenReturn(List.of("I am not a map!"));

                Map<String, String> result = SessionUtils.getPluginAuthSessionContext(mockRequest, pluginId);

                assertThat(result).isEmpty();
            }

            @Test
            void shouldGetAuthSessionContext_SkippingEntries_WhereTheKeysAreNotStrings() {
                String pluginId = "my-brilliant-plugin";

                when(mockSession.getAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin")).thenReturn(Map.of(
                    "foo", "bar",
                    List.of(), "ignored"
                ));

                Map<String, String> result = SessionUtils.getPluginAuthSessionContext(mockRequest, pluginId);

                assertThat(result).isEqualTo(Map.of("foo", "bar"));
            }

            @Test
            void shouldGetAuthSessionContext_SkippingEntries_WhereTheValuesAreNotStrings() {
                String pluginId = "my-brilliant-plugin";

                when(mockSession.getAttribute("GOCD_PLUGIN_AUTH_CONTEXT:my-brilliant-plugin")).thenReturn(Map.of("foo", "bar",
                    "ignored", List.of("I am not a string!")
                ));

                Map<String, String> result = SessionUtils.getPluginAuthSessionContext(mockRequest, pluginId);

                assertThat(result).isEqualTo(Map.of("foo", "bar"));
            }
        }
    }
}
