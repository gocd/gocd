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

package com.thoughtworks.go.plugin.configrepo.codec;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TypeAdapterTest {

    @Mock
    private JsonDeserializationContext jsonDeserializationContext;

    @Test
    public void shouldThrowIfTypeElementCannotBeFound() {
        JsonObject jsonObject = new JsonObject();

        assertThatThrownBy(() -> new DummyTypeAdapter().determineJsonElementForDistinguishingImplementers(jsonObject, jsonDeserializationContext, "typeField"))
            .isInstanceOf(NullPointerException.class)
            .hasMessageContaining("JSON element from plugin did not contain [typeField] property for determining its type. Check your syntax, or the plugin logic.");
    }

    @Test
    public void shouldReturnDefaultOriginIfNotSuppliedThrowIfTypeElementCannotBeFound() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("typeField", "someType");

        DummyTypeAdapter adapter = spy(new DummyTypeAdapter());

        adapter.determineJsonElementForDistinguishingImplementers(jsonObject, jsonDeserializationContext, "typeField");

        verify(adapter).classForName("someType", "gocd");
    }

    private static class DummyTypeAdapter extends TypeAdapter {
        @Override
        protected Class<?> classForName(String typeName, String origin) {
            return null;
        }
    }
}