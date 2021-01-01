/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.SystemEnvironment;

import java.io.File;
import java.io.IOException;

public abstract class JarDetector {

    public static InputStreamSrc create(SystemEnvironment env, String file) throws IOException {
        if (!env.useCompressedJs()) {
            return createFromFile(file);
        } else {
            return new DefaultFilesInputStreamSource(file);
        }
    }

    public static InputStreamSrc createFromFile(String file) throws IOException {
        return new FileInputStreamSource(file);
    }

    public static InputStreamSrc tfsJar(SystemEnvironment env) throws IOException {
        if (runningOnAgent()) {
            return createFromFile("tfs-impl.jar");
        }

        return create(env, "tfs-impl-14.jar");
    }


    private static boolean runningOnAgent() {
        return "agent".equals(System.getProperty("go.process.type"));
    }

    private static class DefaultFilesInputStreamSource extends InputStreamSrc {
        DefaultFilesInputStreamSource(String file) {
            super(JarDetector.class.getClassLoader().getResource("defaultFiles/" + file));
        }
    }

    private static class FileInputStreamSource extends InputStreamSrc {
        FileInputStreamSource(String file) throws IOException {
            super(new File(file).toURI().toURL());
        }

    }
}
