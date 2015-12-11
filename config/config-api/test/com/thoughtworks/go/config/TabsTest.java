/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import org.junit.Test;

import static com.thoughtworks.go.util.DataStructureUtils.a;
import static com.thoughtworks.go.util.DataStructureUtils.m;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class TabsTest {

    @Test
    public void shouldSetAttributedOfTabs() throws Exception {
        Tabs tabs = new Tabs();
        tabs.setConfigAttributes(a(m(Tab.NAME, "tab1", Tab.PATH, "path1"), m(Tab.NAME, "tab2", Tab.PATH, "path2")));
        assertThat(tabs.get(0).getName(), is("tab1"));
        assertThat(tabs.get(0).getPath(), is("path1"));
        assertThat(tabs.get(1).getName(), is("tab2"));
        assertThat(tabs.get(1).getPath(), is("path2"));
    }

    @Test
    public void shouldAddErrorToTheErroneousTabWithinAllTabs(){
        Tabs tabs = new Tabs();
        tabs.add(new Tab("tab1", "path1"));
        tabs.add(new Tab("tab1", "path2"));
        tabs.validate(null);
        assertThat(tabs.get(0).errors().on(Tab.NAME), is("Tab name 'tab1' is not unique."));
        assertThat(tabs.get(1).errors().on(Tab.NAME), is("Tab name 'tab1' is not unique."));
    }

    @Test
    public void shouldValidateTree(){
        Tab tab1 = new Tab("tab1", "path1");
        Tab tab2 = new Tab("tab1", "path2");
        Tab tab3 = new Tab("extremely_long_name_that_is_not_allowed", "path");
        Tabs tabs = new Tabs(tab1, tab2, tab3);
        tabs.validateTree(null);
        assertThat(tab1.errors().on(Tab.NAME), is("Tab name 'tab1' is not unique."));
        assertThat(tab2.errors().on(Tab.NAME), is("Tab name 'tab1' is not unique."));
        assertThat(tab3.errors().on(Tab.NAME), is("Tab name should not exceed 15 characters"));
    }

}
