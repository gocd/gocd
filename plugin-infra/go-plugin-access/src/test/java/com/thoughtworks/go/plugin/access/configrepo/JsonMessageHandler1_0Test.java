/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.plugin.access.configrepo;


import com.sdicons.json.validator.impl.predicates.Int;
import com.thoughtworks.go.plugin.access.configrepo.codec.GsonCodec;
import com.thoughtworks.go.plugin.access.configrepo.contract.CRParseResult;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import static com.thoughtworks.go.util.TestUtils.contains;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.core.Is.is;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
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
        verify(configRepoMigrator).migrate(anyString(), eq(2));
        verify(configRepoMigrator, times(JsonMessageHandler1_0.CURRENT_CONTRACT_VERSION)).migrate(anyString(), anyInt());
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
