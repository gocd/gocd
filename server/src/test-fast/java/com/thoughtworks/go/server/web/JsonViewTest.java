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
package com.thoughtworks.go.server.web;

import com.thoughtworks.go.util.json.JsonUrl;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.go.server.web.JsonRenderer.render;
import static com.thoughtworks.go.server.web.JsonView.asMap;
import static net.javacrumbs.jsonunit.fluent.JsonFluentAssert.assertThatJson;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonViewTest {

    private GoRequestContext requestContext = mock(GoRequestContext.class);

    @Test
    public void testShouldReturnOutputWithoutWhitespaceThatIsNotAllowedInHeaders() throws Exception {
        JsonView view = new JsonView(requestContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key", "\r\t\n");
        view.renderMergedOutputModel(asMap(map), new MockHttpServletRequest(), response);
        String json = response.getContentAsString();
        assertThatJson("{\n  \"key\": \"\\r\\t\\n\"\n}").isEqualTo(json);
    }

    @Test
    public void testShouldRenderEmptyMap() throws Exception {
        JsonView view = new JsonView();
        String json = view.renderJson(new LinkedHashMap());
        assertThatJson("{}").isEqualTo(json);
    }

    @Test
    public void testShouldRenderAllKeyValuePairsFromMap() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("key1", "value1");
        map.put("key2", "value2");

        String json = new JsonView().renderJson(map);
        assertThatJson("{\"key1\":\"value1\",\"key2\":\"value2\"}").isEqualTo(json);
    }

    @Test
    public void testShouldRenderNestedMaps() throws Exception {
        Map<String, Object> map = new LinkedHashMap<>();
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put("keyA", "valueA");
        map.put("key1", nestedMap);
        JsonView view = new JsonView();

        String json = view.renderJson(map);
        assertThatJson("{\"key1\":{\"keyA\":\"valueA\"}}").isEqualTo(json);
    }

    @Test
    public void testShouldRenderArray() throws Exception {
        List list = new ArrayList();
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put("key1", "value1");
        list.add(nestedMap);
        list.add("value2");
        JsonView view = new JsonView();

        String json = view.renderJson(list);
        assertThatJson("[{\"key1\":\"value1\"},\"value2\"]").isEqualTo(json);
    }

    @Test
    public void testShouldRenderFakeMapsWithoutTheSurroundingMap() throws Exception {
        List list = new ArrayList();
        Map<String, Object> nestedMap = new LinkedHashMap<>();
        nestedMap.put("key1", "value1");
        list.add(nestedMap);
        list.add("value2");
        JsonView view = new JsonView();

        String json = view.renderJson(asMap(list));
        assertThatJson("[{\"key1\":\"value1\"},\"value2\"]").isEqualTo(json);
    }

    @Test
    public void testShouldRenderUrlsWithContext() {
        JsonUrl url = new JsonUrl("/foo/bar/baz");

        when(requestContext.getFullRequestPath()).thenReturn("http://something/context");

        String json = render(url, requestContext);
        assertThat(json, is("\"http://something/context/foo/bar/baz\""));
    }


}
