/* **********************GO-LICENSE-START*********************************
 * Copyright 2015 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server.view.velocity;

import com.thoughtworks.go.i18n.Localizer;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Test;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class HeaderTemplateTest {
    public static final String TEMPLATE_PATH = "/WEB-INF/vm/shared/_header.vm";

    @Test
    public void shouldHaveNavigationElementsWhenLocalizerIsPresentInContext() throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("l", mock(Localizer.class));

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);

        Document actualOutput = Jsoup.parse(view.render());

        assertThat(actualOutput.select("#header .application_nav").isEmpty(), is(false));
    }

    @Test
    public void should_NOT_HaveHeaderWhenLocalizerIs_NULL_InContext() throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("l", null);

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);

        Document actualOutput = Jsoup.parse(view.render());

        assertThat(actualOutput.select("#header .application_nav").isEmpty(), is(true));
    }

    @Test
    public void should_NOT_HaveHeaderWhenLocalizerIs_NOT_PresentInContext() throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);

        Document actualOutput = Jsoup.parse(view.render());

        assertThat(actualOutput.select("#header .application_nav").isEmpty(), is(true));
    }

    @Test
    public void shouldHaveLogoEvenWhenNavigationElementsAreNotPresent() throws Exception {
        HashMap<String, Object> data = new HashMap<String, Object>();
        data.put("l", null);

        TestVelocityView view = new TestVelocityView(TEMPLATE_PATH, data);

        Document actualOutput = Jsoup.parse(view.render());

        assertThat(actualOutput.select("#header .header #application_logo").isEmpty(), is(false));
    }
}