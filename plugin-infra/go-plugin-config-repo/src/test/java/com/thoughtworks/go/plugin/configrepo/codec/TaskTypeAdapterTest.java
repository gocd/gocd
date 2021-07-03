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
package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Type;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
public class TaskTypeAdapterTest {

    private TaskTypeAdapter taskTypeAdapter;

    @Mock
    private JsonDeserializationContext jsonDeserializationContext;

    @Mock
    private Type type;

    @BeforeEach
    public void setUp() {
        taskTypeAdapter = new TaskTypeAdapter();
    }

    @Test
    public void shouldInstantiateATaskOfTypeExec() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "exec");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRExecTask.class);
    }

    @Test
    public void shouldInstantiateATaskOfTypeAnt() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "ant");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRBuildTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeNant() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "nant");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRNantTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeRake() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "rake");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRBuildTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeFetch() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "fetch");
        jsonObject.addProperty(TypeAdapter.ARTIFACT_ORIGIN, "gocd");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRFetchArtifactTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeFetchPluggableArtifact() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "fetch");
        jsonObject.addProperty(TypeAdapter.ARTIFACT_ORIGIN, "external");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRFetchPluggableArtifactTask.class);
    }

    @Test
    public void shouldThrowExceptionForFetchIfOriginIsInvalid() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "fetch");
        jsonObject.addProperty(TypeAdapter.ARTIFACT_ORIGIN, "fsg");

        assertThatThrownBy(() -> taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext))
                .isInstanceOf(JsonParseException.class)
                .hasMessageContaining("Invalid artifact origin 'fsg' for fetch task.");
    }
}
