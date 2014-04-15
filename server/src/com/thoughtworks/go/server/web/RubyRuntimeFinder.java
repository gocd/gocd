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


import javax.servlet.ServletContext;

import org.jruby.Ruby;
import org.jruby.rack.RackApplicationFactory;
import org.jruby.rack.RackInitializationException;
import org.jruby.rack.RackServletContextListener;
import org.springframework.web.context.ServletContextAware;

public class RubyRuntimeFinder implements ServletContextAware {
    private ServletContext servletContext;


    public void setServletContext(ServletContext servletContext) {
        this.servletContext = servletContext;
    }

    public Ruby getRuntime() {
        if (servletContext == null) {
            return null;
        }

        RackApplicationFactory rackAppFactory = (RackApplicationFactory) servletContext.getAttribute(RackServletContextListener.FACTORY_KEY);

        if (rackAppFactory == null) {
            return null;
        }

        try {
            return rackAppFactory.getApplication().getRuntime();
        } catch (RackInitializationException e) {
            return null;
        }
    }
}
