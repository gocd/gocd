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

package com.thoughtworks.go.server.util;

import java.util.Locale;

import com.thoughtworks.go.server.web.GoVelocityView;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.servlet.ViewResolver;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ServerVersionTest {

    @Before
    public void setUp() {
        ServerVersion.resetCachedGoVersion();
    }

    @Test
    public void shouldReturnServerVersion_2() throws Exception {
        final GoVelocityView view = mock(GoVelocityView.class);
        String goVersion = "Some version of Go";
        when(view.getContentAsString()).thenReturn(goVersion);
        ViewResolver resolver = mock(ViewResolver.class);
        when(resolver.resolveViewName("admin/admin_version.txt", Locale.getDefault())).thenReturn(view);
        ServerVersion serverVersion = new ServerVersion(resolver);
        assertThat(serverVersion.version(), is(goVersion));
    }

    @Test
    public void shouldReturnNAWhenNoResolversArePresent() {
        ServerVersion serverVersion = new ServerVersion();
        assertThat(serverVersion.version(), is("N/A"));
    }

    @Test
    public void shouldReturnNAWhenResolverUnableToResolveViewName() throws Exception {
        ViewResolver resolver = mock(ViewResolver.class);
        when(resolver.resolveViewName("admin/admin_version.txt", Locale.getDefault())).thenThrow(new RuntimeException("Not found..."));
        ServerVersion serverVersion = new ServerVersion(resolver);
        assertThat(serverVersion.version(), is("N/A"));
    }
}
