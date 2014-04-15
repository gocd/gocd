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

package com.thoughtworks.go.server.controller;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Properties;

import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.apache.velocity.app.VelocityEngine;

public class VelocityFixture {
    private String contextPath = "/context-path";

    public String renderMacro(String template, HashMap model) throws Exception {
        String path = new File("../server/webapp/WEB-INF/vm").getCanonicalPath();

        Properties properties = new Properties();
        properties.setProperty(Velocity.RESOURCE_LOADER, "file");
        properties.setProperty(Velocity.FILE_RESOURCE_LOADER_PATH, path);
        properties.setProperty(Velocity.FILE_RESOURCE_LOADER_CACHE, "false");

        VelocityEngine engine = new VelocityEngine();
        engine.init(properties);

        Template t = engine.getTemplate(template + ".vm");

        VelocityContext ctx = new VelocityContext();
        for (Object key : model.keySet()) {
            ctx.put((String) key, model.get(key));
        }
        ctx.put("req", new FakeRequest());

        Writer writer = new StringWriter();
        t.merge(ctx, writer);

        return writer.toString();

    }

    public class FakeRequest {
        public String getContextPath() {
            return contextPath;
        }
    }
}
