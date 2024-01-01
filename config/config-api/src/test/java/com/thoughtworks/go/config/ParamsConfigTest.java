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
package com.thoughtworks.go.config;

import com.thoughtworks.go.helper.PipelineConfigMother;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ParamsConfigTest {

    private ParamsConfig paramsConfig;
    private final ValidationContext context = mock(ValidationContext.class);

    @BeforeEach
    public void setUp() {
        when(context.getPipeline()).thenReturn(PipelineConfigMother.createPipelineConfig("some-pipeline", "stage-name", "job-name"));
    }

    @Test
    public void shouldPopulateParamFromMapIgnoringEmptyPairs() {
        paramsConfig = new ParamsConfig();
        List<Map<String, String>> paramsMap = new ArrayList<>();

        paramsMap.add(createParamMap("param-name", "param-value"));
        paramsMap.add(createParamMap("", ""));
        paramsMap.add(createParamMap("", "bar"));

        paramsConfig.setConfigAttributes(paramsMap);

        assertThat(paramsConfig.size()).isEqualTo((2));
        assertThat(paramsConfig.getParamNamed("param-name").getValue()).isEqualTo(("param-value"));
    }

    @Test
    public void getIndex() {
        paramsConfig = new ParamsConfig();
        ParamConfig one = new ParamConfig("name", "value");
        paramsConfig.add(one);
        ParamConfig two = new ParamConfig("other", "other-value");
        paramsConfig.add(two);

        assertThat(paramsConfig.getIndex("other")).isEqualTo((1));
        assertThat(paramsConfig.getIndex("name")).isEqualTo((0));
    }

    @Test
    public void getIndex_shouldThrowExceptionIfNameNotFound() {

        assertThatThrownBy(() -> new ParamsConfig().getIndex("foo"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("param 'foo' not found");
    }

    private Map<String, String> createParamMap(final String name, final String value) {
        Map<String, String> map = new HashMap<>();
        map.put(ParamConfig.NAME, name);
        map.put(ParamConfig.VALUE, value);
        return map;
    }

    @Test
    public void shouldThrowAnErrorWhenDuplicateNameIsInserted() {
        paramsConfig = new ParamsConfig();
        ParamConfig one = new ParamConfig("name", "value");
        paramsConfig.add(one);
        ParamConfig two = new ParamConfig("name", "other-value");
        paramsConfig.add(two);

        paramsConfig.validate(context);

        assertThat(one.errors().isEmpty()).isEqualTo((false));
        assertThat(one.errors().firstError()).contains("Param name 'name' is not unique for pipeline 'some-pipeline'.");
        assertThat(two.errors().isEmpty()).isEqualTo((false));
        assertThat(two.errors().firstError()).contains("Param name 'name' is not unique for pipeline 'some-pipeline'.");
    }

    @Test
    public void shouldThrowAnErrorWhenNameIsEmpty() {
        paramsConfig = new ParamsConfig();
        ParamConfig empty = new ParamConfig("", "value");
        paramsConfig.add(empty);

        paramsConfig.validate(context);

        assertThat(empty.errors().isEmpty()).isEqualTo((false));
        assertThat(empty.errors().firstError()).contains("Parameter cannot have an empty name for pipeline 'some-pipeline'.");
    }

}
