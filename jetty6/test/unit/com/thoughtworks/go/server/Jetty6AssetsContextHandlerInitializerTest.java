/*************************GO-LICENSE-START*********************************
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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.server;

import org.junit.Test;
import org.mortbay.jetty.webapp.WebAppContext;

import java.io.IOException;

import static org.mockito.Mockito.*;

public class Jetty6AssetsContextHandlerInitializerTest {
    @Test
    public void shouldInitializeHandlerOnWebappContextLifeCycleStarted() throws IOException {
        Jetty6AssetsContextHandler handler = mock(Jetty6AssetsContextHandler.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        Jetty6AssetsContextHandlerInitializer initializer = new Jetty6AssetsContextHandlerInitializer(handler, webAppContext);
        initializer.lifeCycleStarted(null);
        verify(handler, times(1)).init(webAppContext);
    }

    @Test
    public void shouldNotInitializeHandlerOnOtherWebappContextLifeCycleEvents() throws IOException {
        Jetty6AssetsContextHandler handler = mock(Jetty6AssetsContextHandler.class);
        WebAppContext webAppContext = mock(WebAppContext.class);
        Jetty6AssetsContextHandlerInitializer initializer = new Jetty6AssetsContextHandlerInitializer(handler, webAppContext);
        initializer.lifeCycleStarting(null);
        verify(handler, never()).init(webAppContext);
    }


}