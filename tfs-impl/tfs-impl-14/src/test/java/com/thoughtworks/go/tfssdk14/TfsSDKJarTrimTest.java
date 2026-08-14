/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.tfssdk14;

import com.microsoft.tfs.core.TFSTeamProjectCollection;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.assertj.core.api.Assertions.assertThat;

public class TfsSDKJarTrimTest {

    /**
     * NestedJarClassLoader relies on the SDK jar NOT bundling its own commons-logging: since the classes are
     * absent from the jar, the SDK's logging via the commons-logging API resolves against the parent
     * classloader's jcl-over-slf4j and flows into GoCD's slf4j/logback setup. The SDK's bundled log4j2 and
     * TEE logging adapter are unreachable as a result and are also removed (see trimTfsSdkJar).
     */
    @Test
    public void trimmedSdkJarShouldNotBundleAnyLoggingImplementation() throws Exception {
        File sdkJar = new File(TFSTeamProjectCollection.class.getProtectionDomain().getCodeSource().getLocation().toURI());

        try (JarFile jar = new JarFile(sdkJar)) {
            List<String> loggingEntries = jar.stream()
                .map(JarEntry::getName)
                .filter(name -> name.startsWith("org/apache/commons/logging/")
                    || name.startsWith("org/apache/logging/log4j/")
                    || name.startsWith("com/microsoft/tfs/logging/"))
                .toList();

            assertThat(loggingEntries)
                .describedAs("SDK jar %s should not bundle any logging classes; see trimTfsSdkJar in build.gradle", sdkJar)
                .isEmpty();
        }
    }

    /**
     * Guards the trimTfsSdkJar exclusions: every class remaining in the trimmed jar must still be loadable,
     * i.e. no remaining class may have a load-time dependency (superclass/interface hierarchy) on an excluded
     * one. References from method bodies to excluded classes are fine - the JVM resolves those lazily, and
     * they are only a problem if actually executed, which GoCD's version-control-only usage does not do.
     */
    @Test
    public void everyClassRemainingInTrimmedSdkJarShouldBeLoadable() throws Exception {
        File sdkJar = new File(TFSTeamProjectCollection.class.getProtectionDomain().getCodeSource().getLocation().toURI());
        List<String> classNames;

        try (JarFile jar = new JarFile(sdkJar)) {
            classNames = jar.stream()
                .map(JarEntry::getName)
                .filter(name -> name.endsWith(".class") && !name.endsWith("module-info.class"))
                .map(name -> name.substring(0, name.length() - ".class".length()).replace('/', '.'))
                .toList();
        }

        Map<String, Throwable> unloadable = new HashMap<>();
        for (String className : classNames) {
            try {
                Class.forName(className, false, TFSTeamProjectCollection.class.getClassLoader());
            } catch (Throwable e) {
                unloadable.put(className, e);
            }
        }

        assertThat(classNames).isNotEmpty();
        assertThat(unloadable)
            .describedAs("all %d classes in %s should be loadable; see trimTfsSdkJar in build.gradle", classNames.size(), sdkJar)
            .isEmpty();
    }
}
