/*
 * Copyright 2016 ThoughtWorks, Inc.
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

package com.thoughtworks.go.domain.materials.tfs;

import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public abstract class TFSJarDetector {

    protected final SystemEnvironment systemEnvironment;

    public TFSJarDetector(SystemEnvironment systemEnvironment) {
        this.systemEnvironment = systemEnvironment;
    }

    public abstract URL getJarURL() throws IOException;

    public static TFSJarDetector create(SystemEnvironment env) {
        if (runningOnAgent()) {
            return new AgentTFSJarDetector(env);
        }

        if (!env.useCompressedJs()){
            return new DevelopmentServerTFSJarDetector(env);
        }
        return new ServerTFSJarDetector(env);
    }

    private static class AgentTFSJarDetector extends TFSJarDetector {

        AgentTFSJarDetector(SystemEnvironment env) {
            super(env);
        }

        @Override
        public URL getJarURL() throws IOException {
            // we assume this is downloaded via some other means
            return new File("tfs-impl.jar").toURI().toURL();
        }
    }

    private static class ServerTFSJarDetector extends TFSJarDetector {
        ServerTFSJarDetector(SystemEnvironment env) {
            super(env);
        }

        @Override
        public URL getJarURL() throws IOException {
            return TFSJarDetector.class.getClassLoader().getResource("defaultFiles/tfs-impl-14.jar");
        }
    }

    private static boolean runningOnAgent() {
        return "agent".equals(System.getProperty("go.process.type"));
    }

    public static class DevelopmentServerTFSJarDetector extends TFSJarDetector {
        public DevelopmentServerTFSJarDetector(SystemEnvironment env) {
            super(env);
        }

        @Override
        public URL getJarURL() throws IOException {
            return new File("tfs-impl-14.jar").toURI().toURL();
        }
    }
}
