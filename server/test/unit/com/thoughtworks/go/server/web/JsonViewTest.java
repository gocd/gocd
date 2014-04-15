/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.web;

import com.thoughtworks.go.server.web.i18n.ResolvableViewableStatus;
import junit.framework.TestCase;

import com.thoughtworks.go.util.json.JsonList;
import com.thoughtworks.go.util.json.JsonMap;
import com.thoughtworks.go.util.json.JsonUrl;
import com.thoughtworks.go.server.web.i18n.CurrentStatus;
import com.thoughtworks.go.util.JsonTester;
import static org.hamcrest.core.Is.is;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.lib.legacy.ClassImposteriser;
import static org.junit.Assert.assertThat;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class JsonViewTest extends TestCase {

    private Mockery mockContext = new Mockery() {
        {
            setImposteriser(ClassImposteriser.INSTANCE);
        }
    };
    private GoRequestContext requestContext = mockContext.mock(GoRequestContext.class);

    public void testShouldReturnOutputWithoutWhitespaceThatIsNotAllowedInHeaders() throws Exception {
        JsonView view = new JsonView(requestContext);
        MockHttpServletResponse response = new MockHttpServletResponse();
        JsonMap map = new JsonMap();
        map.put("key", "\r\t\n");
        view.renderMergedOutputModel(JsonView.asMap(map), new MockHttpServletRequest(), response);
        String json = response.getContentAsString();
        new JsonTester(json).is(
                "{ 'key' : '\\r\\t\\n' }"
        );
    }

    public void testShouldRenderEmptyMap() throws Exception {
        JsonView view = new JsonView();
        String json = view.renderJson(new JsonMap());
        new JsonTester(json).is(
                "{ }"
        );
    }

    public void testShouldRenderAllKeyValuePairsFromMap() throws Exception {
        JsonMap map = new JsonMap();
        map.put("key1", "value1");
        map.put("key2", "value2");

        String json = new JsonView().renderJson(map);
        new JsonTester(json).is(
                "{ 'key1' : 'value1', 'key2' : 'value2' }"
        );
    }

    public void testShouldRenderNestedMaps() throws Exception {
        JsonMap map = new JsonMap();
        JsonMap nestedMap = new JsonMap();
        nestedMap.put("keyA", "valueA");
        map.put("key1", nestedMap);
        JsonView view = new JsonView();

        String json = view.renderJson(map);
        new JsonTester(json).is(
                  "{ 'key1' : {"
                + "    'keyA' : 'valueA'"
                + "  }"
                + "}"
        );
    }

    public void testShouldRenderArray() throws Exception {
        JsonList list = new JsonList();
        JsonMap nestedMap = new JsonMap();
        nestedMap.put("key1", "value1");
        list.add(nestedMap);
        list.add("value2");
        JsonView view = new JsonView();

        String json = view.renderJson(list);

        new JsonTester(json).is(
                "[ { 'key1' : 'value1' }, 'value2' ]"
        );
    }

    public void testShouldRenderFakeMapsWithoutTheSurroundingMap() throws Exception {
        JsonList list = new JsonList();
        JsonMap nestedMap = new JsonMap();
        nestedMap.put("key1", "value1");
        list.add(nestedMap);
        list.add("value2");
        JsonView view = new JsonView();

        String json = view.renderJson(JsonView.asMap(list));

        new JsonTester(json).is(
                "[ { 'key1' : 'value1' }, 'value2' ]"
        );
    }

    public void testShouldRenderI18n() {
        JsonMap map = new JsonMap();
        final String i18nMessage = "waiting(zh_CN)";
        final ResolvableViewableStatus resolvableViewableStatus = new ResolvableViewableStatus(CurrentStatus.WAITING);
        map.put("key1", resolvableViewableStatus);
        JsonView view = new JsonView(requestContext);

        mockContext.checking(new Expectations() {
            {
                one(requestContext).getMessage(with(equal(resolvableViewableStatus)));
                will(returnValue(i18nMessage));
            }
        });

        String json = view.renderJson(map);
        mockContext.assertIsSatisfied();

        new JsonTester(json).is(
                "{ 'key1' : '" + i18nMessage + "' }"
        );
    }

    public void testShouldRenderUrlsWithContext() {
        JsonUrl url = new JsonUrl("/foo/bar/baz");

        mockContext.checking(new Expectations() {
            {
                one(requestContext).getFullRequestPath();
                will(returnValue("http://something/context"));
            }
        });

        String json =  JsonStringRenderer.render(url, requestContext);
        assertThat(json, is("\"http://something/context/foo/bar/baz\""));
    }


}
