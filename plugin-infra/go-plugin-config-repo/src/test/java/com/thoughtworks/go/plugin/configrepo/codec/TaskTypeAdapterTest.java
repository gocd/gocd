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
package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.thoughtworks.go.plugin.configrepo.contract.tasks.*;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Type;

public class TaskTypeAdapterTest {

    private TaskTypeAdapter taskTypeAdapter;

    @Mock
    private JsonDeserializationContext jsonDeserializationContext;

    @Mock
    private Type type;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        taskTypeAdapter = new TaskTypeAdapter();
    }

    @Test
    public void shouldInstantiateATaskOfTypeExec() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "exec");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRExecTask.class);
    }

    @Test
    public void shouldInstantiateATaskOfTypeAnt() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "ant");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRBuildTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeNant() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "nant");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRNantTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeRake() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "rake");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRBuildTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeFetch() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "fetch");
        jsonObject.addProperty(TypeAdapter.ARTIFACT_ORIGIN, "gocd");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRFetchArtifactTask.class);
    }

    @Test
    public void shouldInstantiateATaskForTypeFetchPluggableArtifact() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "fetch");
        jsonObject.addProperty(TypeAdapter.ARTIFACT_ORIGIN, "external");
        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);

        Mockito.verify(jsonDeserializationContext).deserialize(jsonObject, CRFetchPluggableArtifactTask.class);
    }

    @Test
    public void shouldThrowExceptionForFetchIfOriginIsInvalid() throws Exception {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("type", "fetch");
        jsonObject.addProperty(TypeAdapter.ARTIFACT_ORIGIN, "fsg");

        thrown.expectMessage("Invalid artifact origin 'fsg' for fetch task.");
        thrown.expect(JsonParseException.class);

        taskTypeAdapter.deserialize(jsonObject, type, jsonDeserializationContext);
    }
}
