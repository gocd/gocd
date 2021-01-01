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
package com.thoughtworks.go.plugin.access.configrepo.v1;


import com.thoughtworks.go.plugin.access.configrepo.ConfigRepoMigrator;
import com.thoughtworks.go.plugin.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.configrepo.contract.CRParseResult;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.thoughtworks.go.util.TestUtils.contains;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.*;

public class JsonMessageHandler1_0Test {

    private final JsonMessageHandler1_0 handler;
    private final ConfigRepoMigrator configRepoMigrator;

    public JsonMessageHandler1_0Test() {
        configRepoMigrator = mock(ConfigRepoMigrator.class);
        handler = new JsonMessageHandler1_0(new GsonCodec(), configRepoMigrator);
    }

    @Test
    public void shouldErrorWhenMissingTargetVersionInResponse() {
        String json = "{\n" +
                "  \"environments\" : [],\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : []\n" +
                "}";

        CRParseResult result = handler.responseMessageForParseDirectory(json);
        assertThat(result.getErrors().getErrorsAsText(), contains("missing 'target_version' field"));
    }

    @Test
    public void shouldNotErrorWhenTargetVersionInResponse() {
        String json = "{\n" +
                "  \"target_version\" : 1,\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : []\n" +
                "}";

        makeMigratorReturnSameJSON();
        CRParseResult result = handler.responseMessageForParseDirectory(json);

        assertFalse(result.hasErrors());
    }

    @Test
    public void shouldAppendPluginErrorsToAllErrors() {
        String json = "{\n" +
                "  \"target_version\" : 1,\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : [{\"location\" : \"somewhere\", \"message\" : \"failed to parse pipeline.json\"}]\n" +
                "}";
        CRParseResult result = handler.responseMessageForParseDirectory(json);
        assertTrue(result.hasErrors());
    }

    @Test
    public void shouldCallMigratorForEveryVersionFromTheProvidedOneToTheLatest() throws Exception {
        handler.responseMessageForParseDirectory("{ \"target_version\": \"0\", \"something\": \"value\" }");

        verify(configRepoMigrator).migrate(anyString(), eq(1));
        verify(configRepoMigrator).migrate(nullable(String.class), eq(2));
        verify(configRepoMigrator, times(JsonMessageHandler1_0.CURRENT_CONTRACT_VERSION)).migrate(nullable(String.class), anyInt());
    }

    @Test
    public void shouldErrorWhenTargetVersionOfPluginIsHigher() {
        int targetVersion = JsonMessageHandler1_0.CURRENT_CONTRACT_VERSION + 1;
        String json = "{\n" +
                "  \"target_version\" : " + targetVersion + ",\n" +
                "  \"pipelines\" : [],\n" +
                "  \"errors\" : []\n" +
                "}";

        CRParseResult result = handler.responseMessageForParseDirectory(json);
        String errorMessage = String.format("'target_version' is %s but the GoCD Server supports %s", targetVersion, JsonMessageHandler1_0.CURRENT_CONTRACT_VERSION);
        assertThat(result.getErrors().getErrorsAsText(), contains(errorMessage));
    }

    private void makeMigratorReturnSameJSON() {
        when(configRepoMigrator.migrate(anyString(), anyInt())).thenAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocationOnMock) throws Throwable {
                return invocationOnMock.getArguments()[0];
            }
        });
    }
}
