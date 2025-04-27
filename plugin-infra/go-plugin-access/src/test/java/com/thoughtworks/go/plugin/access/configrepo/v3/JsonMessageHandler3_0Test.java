/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.plugin.access.configrepo.v3;

import com.thoughtworks.go.plugin.access.configrepo.ConfigFileList;
import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMigrator;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.mock;

public class JsonMessageHandler3_0Test {
    private final JsonMessageHandler3_0 handler;

    public JsonMessageHandler3_0Test() {
        ConfigRepoMigrator configRepoMigrator = mock(ConfigRepoMigrator.class);
        handler = new JsonMessageHandler3_0(new GsonCodec(), configRepoMigrator);
    }

    @Test
    public void shouldNotHaveErrorsForConfigFilesWhenValidJSON() {
        assertFalse(handler.responseMessageForConfigFiles("{\"files\": []}").hasErrors());
    }

    @Test
    public void shouldReturnErrorForConfigFilesWhenInvalidResponseJSON() {
        assertThat(handler.responseMessageForConfigFiles("{\"files\": null}"))
            .satisfies(JsonMessageHandler3_0Test::doesntCorrectlyImplement);
        assertThat(handler.responseMessageForConfigFiles("{\"blah\": [\"file\"]}"))
            .satisfies(JsonMessageHandler3_0Test::doesntCorrectlyImplement);
        assertThat(handler.responseMessageForConfigFiles("{}"))
            .satisfies(JsonMessageHandler3_0Test::doesntCorrectlyImplement);
    }

    @Test
    public void shouldNotHaveErrorsForParseDirectoryWhenValidJSON() {
        assertFalse(handler.responseMessageForParseDirectory("{\"target_version\": 11}").hasErrors());
    }

    @Test
    public void shouldReturnErrorForParseDirectoryWhenInvalidResponseJSON() {
        assertThat(handler.responseMessageForParseDirectory("{}"))
            .satisfies(result -> doesntCorrectlyImplement(result,
                """
                Plugin response message;
                1. missing 'target_version' field
                """));

        assertThat(handler.responseMessageForParseDirectory("""
            {
              "target_version": 11,
              "pipelines": { "bad": "json" },
            }"""))
            .satisfies(result -> doesntCorrectlyImplement(result,
                """
                Plugin response message;
                1. Unexpected error when handling plugin response
                com.google.gson.JsonSyntaxException"""));
    }

    private static void doesntCorrectlyImplement(ConfigFileList result) {
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().getErrorCount()).isEqualTo(1);
        assertThat(result.getErrors().getErrorsAsText()).contains("""
            Plugin response message;
            1. The plugin returned a response that indicates that it doesn't correctly implement this endpoint
            """);
    }

    private static void doesntCorrectlyImplement(CRParseResult result, String expectedMessage) {
        assertThat(result.hasErrors()).isTrue();
        assertThat(result.getErrors().getErrorCount()).isEqualTo(1);
        assertThat(result.getErrors().getErrorsAsText()).contains(expectedMessage);
    }
}
