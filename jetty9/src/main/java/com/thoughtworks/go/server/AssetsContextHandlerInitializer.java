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
package com.thoughtworks.go.server;


import org.eclipse.jetty.util.component.LifeCycle;
import org.eclipse.jetty.webapp.WebAppContext;

import java.io.IOException;

public class AssetsContextHandlerInitializer implements LifeCycle.Listener{
    private AssetsContextHandler assetsContextHandler;
    private WebAppContext webAppContext;

    public AssetsContextHandlerInitializer(AssetsContextHandler assetsContextHandler, WebAppContext webAppContext){
        this.assetsContextHandler = assetsContextHandler;
        this.webAppContext = webAppContext;
    }

    @Override
    public void lifeCycleStarting(LifeCycle event) {
    }

    @Override
    public void lifeCycleStarted(LifeCycle event) {
        try {
            assetsContextHandler.init(webAppContext);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void lifeCycleFailure(LifeCycle event, Throwable cause) {
    }

    @Override
    public void lifeCycleStopping(LifeCycle event) {
    }

    @Override
    public void lifeCycleStopped(LifeCycle event) {
    }
}
