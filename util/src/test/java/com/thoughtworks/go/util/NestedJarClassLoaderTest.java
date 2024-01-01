/*
 * Copyright 2024 Thoughtworks, Inc.
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

package com.thoughtworks.go.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class NestedJarClassLoaderTest {

    private static final String JAR_CLASS = "HelloWorld";
    private static final String NESTED_JAR_CLASS = "NestedHelloWorld";

    /**
     * src/test/resources/helloworld.jar
     * ├── HelloWorld.class
     * └── nestedhelloworld.jar
     *     └── NestedHelloWorld.class
     */
    private final URL testJar = getClass().getResource("/helloworld.jar");

    private NestedJarClassLoader nestedJarClassLoader;

    @BeforeEach
    void setUp() {
        nestedJarClassLoader = new NestedJarClassLoader(testJar);
    }

    @Test
    public void canLoadClassFromTopLevelOfJar() throws Exception {
        assertThat(nestedJarClassLoader.loadClass(JAR_CLASS))
                .isNotNull()
                .hasPackage("");
    }

    @Test
    public void canLoadClassFromNestedJar() throws Exception {
        assertThat(nestedJarClassLoader.loadClass(NESTED_JAR_CLASS))
                .isNotNull()
                .hasPackage("");
    }

    @Test
    public void canExcludeLoadingResourcesFromJar() {
        // Not sure of the intention of this; this characterises existing behaviour
        assertThat(new NestedJarClassLoader(testJar, "helloworld").getResource("helloworld.jar"))
                .isNotNull();
        assertThat(new NestedJarClassLoader(testJar).getResource("helloworld.jar"))
                .isNull();
    }

    @Test
    public void canNotLoadClassWithResolveSpecifiedFromNested() {
        // No idea why this behaves this way - this is a "characterisation test" that follows the existing behaviour
        assertThatThrownBy(() -> nestedJarClassLoader.loadClass(JAR_CLASS, true))
                .isExactlyInstanceOf(ClassNotFoundException.class)
                .hasMessage(JAR_CLASS);
    }

    @Test
    public void canLoadClassFromParent() throws Exception {
        assertThat(nestedJarClassLoader.loadClass(this.getClass().getCanonicalName()))
                .isNotNull()
                .hasPackage(this.getClass().getPackageName());
        assertThat(nestedJarClassLoader.loadClass(this.getClass().getCanonicalName(), true))
                .isNotNull()
                .hasPackage(this.getClass().getPackageName());
    }
}