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

import com.thoughtworks.go.util.SubprocessLogger;
import com.thoughtworks.go.util.SystemEnvironment;
import com.thoughtworks.go.util.validators.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Constructor;
import java.util.ArrayList;

public class GoServer {

    static {
        System.setProperty("jruby.compile.invokedynamic", "false");
        System.setProperty("jruby.ji.objectProxyCache", "false");
    }

    private static final Logger LOG = LoggerFactory.getLogger(GoServer.class);

    private SystemEnvironment systemEnvironment;
    private AppServer server;
    protected SubprocessLogger subprocessLogger;

    public GoServer() {
        this(new SystemEnvironment());
    }

    protected GoServer(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
        subprocessLogger = new SubprocessLogger();
    }

    public void go() throws Exception {
        Validation validation = validate();
        if (validation.isSuccessful()) {
            subprocessLogger.registerAsExitHook("Following processes were alive at shutdown: ");
            startServer();
        } else {
            validation.logErrors();
        }
    }

    protected void startServer() throws Exception {
        server = configureServer();
        server.start();
        if (!server.hasStarted()) {
            Throwable exceptionAtServerStart = server.getUnavailableException();
            server.stop();
            LOG.error("ERROR: Failed to start GoCD server.", exceptionAtServerStart);
            throw new RuntimeException("Failed to start GoCD server.", exceptionAtServerStart);
        }
    }

    AppServer configureServer() throws Exception {
        Constructor<?> constructor = Class.forName(systemEnvironment.get(SystemEnvironment.APP_SERVER)).getConstructor(SystemEnvironment.class, String.class);
        AppServer server = ((AppServer) constructor.newInstance(systemEnvironment, systemEnvironment.getServerKeyStorePassword()));
        server.configure();
        logMessageIfUsingAddons();
        server.setSessionConfig();
        return server;
    }

    private void logMessageIfUsingAddons() {
        File addonsPath = new File(systemEnvironment.get(SystemEnvironment.ADDONS_PATH));
        if (addonsPath.exists() && addonsPath.canRead()) {
            if (addonsPath.list().length > 0) {
                LOG.info("Looks like you are using GoCD addons: '%s'. Support for GoCD addons is removed in GoCD 20.6.0." +
                        "You no longer need a separate addon, the functionality supported by the addons is now part of GoCD core.", addonsPath.list());
            }
        }
    }

    public void stop() throws Exception {
        server.stop();
    }

    Validation validate() {
        Validation validation = new Validation();
        for (Validator validator : validators()) {
            validator.validate(validation);
        }
        return validation;
    }

    ArrayList<Validator> validators() {
        ArrayList<Validator> validators = new ArrayList<>();
        validators.add(new ServerPortValidator(systemEnvironment.getServerPort()));
        validators.add(FileValidator.defaultFile("cruise.war"));
        validators.add(FileValidator.configFile("cruise-config.xml", systemEnvironment));
        validators.add(FileValidator.configFileAlwaysOverwrite("cruise-config.xsd", systemEnvironment));
        validators.add(FileValidator.configFile("jetty.xml", systemEnvironment));
        validators.add(new JettyWorkDirValidator());
        validators.add(FileValidator.configFile(systemEnvironment.get(systemEnvironment.GO_UPDATE_SERVER_PUBLIC_KEY_FILE_NAME), systemEnvironment));
        return validators;
    }
}
