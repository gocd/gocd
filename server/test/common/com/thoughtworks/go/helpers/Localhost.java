/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helpers;

import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;

import java.io.File;
import java.util.Arrays;
import java.util.List;

public class Localhost {

    private static final int PORT = 7493;

    private final Server server;
    protected final List<String> baseStageNames;
    protected final List<String> baseBuildNames;
    protected final List<String> pipelineNames;

    Localhost(int port, String overrideConfigFilePath, List<String> pipelineNames, List<String> baseStageNames,
              List<String> baseBuildNames) throws Exception {
        this.pipelineNames = pipelineNames;
        this.baseStageNames = baseStageNames;
        this.baseBuildNames = baseBuildNames;

        File configXml = DataUtils.getConfigXmlOfWebApp();
        File srcFile;
        if (overrideConfigFilePath == null) {
            srcFile = DataUtils.getConfigXmlAsFile();
        } else {
            srcFile = new File(overrideConfigFilePath);
        }
        FileUtils.copyFile(srcFile, configXml);
        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configXml.getAbsolutePath());
        new SystemEnvironment().setProperty("jdbc.port", "9003");

        server = new Server(port);
        WebAppContext context = new WebAppContext("webapp", "/go");

		context.setConfigurationClasses(new String[]{
				WebInfConfiguration.class.getCanonicalName(),
				WebXmlConfiguration.class.getCanonicalName(),
				JettyWebXmlConfiguration.class.getCanonicalName()
		});

        context.setDefaultsDescriptor("webapp/WEB-INF/webdefault.xml");
        server.setHandler(context);
        this.setCookieExpireIn6Months(context);
    }

    private void setCookieExpireIn6Months(WebAppContext wac) {
        int sixMonths = 60 * 60 * 24 * 180;
		wac.getSessionHandler().getSessionManager().getSessionCookieConfig().setMaxAge(sixMonths);
    }

    public static void main(String[] args) throws Exception {
        DataUtils.cloneCCHome();

        int port = PORT;
        String sourceConfigFilePath = null;
        int numberOfPipelines = 10;

        Localhost localhost = new Localhost(port, sourceConfigFilePath,
                Arrays.asList("studios", "evolve"),
                Arrays.asList("mingle", "cruise", "stage3", "stage4"),
                Arrays.asList("functional", "unit", "build3", "build4", "build5"));
        mainAction(localhost, numberOfPipelines);
    }

    protected static void mainAction(Localhost localhost, int numberOfPipelines) throws Exception {
        startGoServer(localhost);
//        localhost.prepareSampleData(numberOfPipelines);
    }

    private static void startGoServer(final Localhost localhost) {
        new Thread(new Runnable() {
            public void run() {
                try {
                    localhost.server.start();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }


}

