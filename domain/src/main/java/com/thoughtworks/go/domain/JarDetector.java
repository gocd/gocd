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
import java.nio.file.Paths;

public abstract class JarDetector {

    private static final String JAR_STORAGE_PREFIX = "defaultFiles";

    public static InputStreamSrc createFromRelativeDefaultFile(SystemEnvironment env, String file) throws IOException {
        if (!env.useCompressedJs()) {
            return new DefaultFilesFileInputStreamSource(file);
        } else {
            return new DefaultFilesClasspathInputStreamSource(file);
        }
    }

    public static InputStreamSrc createRaw(String file) throws IOException {
        return new FileInputStreamSource(file);
    }

    public static InputStreamSrc tfsJar(SystemEnvironment env) throws IOException {
        if (runningOnAgent()) {
            return createRaw("tfs-impl.jar");
        }

        return createFromRelativeDefaultFile(env, "tfs-impl-14.jar");
    }

    private static boolean runningOnAgent() {
        return "agent".equals(System.getProperty("go.process.type"));
    }

    private static class DefaultFilesClasspathInputStreamSource extends InputStreamSrc {
        DefaultFilesClasspathInputStreamSource(String file) {
            super(JarDetector.class.getClassLoader().getResource(JAR_STORAGE_PREFIX + '/' + file));
        }
    }

    private static class DefaultFilesFileInputStreamSource extends InputStreamSrc {
        DefaultFilesFileInputStreamSource(String file) throws IOException {
            super(Paths.get(JAR_STORAGE_PREFIX, file).toFile().toURI().toURL());
        }
    }

    private static class FileInputStreamSource extends InputStreamSrc {
        FileInputStreamSource(String file) throws IOException {
            super(new File(file).toURI().toURL());
        }
    }
}
