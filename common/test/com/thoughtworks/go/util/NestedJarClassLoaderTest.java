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

package com.thoughtworks.go.util;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import javax.xml.parsers.SAXParserFactory;

import org.junit.Ignore;
import org.junit.Test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@Ignore("Sriki nan magne.. What test is this?")
public class NestedJarClassLoaderTest {

    @Test
    public void shouldLoadClassFromGivenJar()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar());
        Object objInst = Class.forName("hsqlServlet", true, nestedJarClassLoader).newInstance();
        assertThat(objInst.getClass().getClassLoader() instanceof URLClassLoader, is(true));

        Class<?> clazz = Class.forName("javax.xml.parsers.SAXParserFactory", true, nestedJarClassLoader);
        assertThat(clazz.getClassLoader() instanceof URLClassLoader, is(true));
        try {
            clazz.getMethod("newInstance").invoke(clazz);
            fail("Should throw class cast exception");
        } catch (Exception e) {
            assertThat(e.getCause() instanceof ClassCastException, is(true));
        }

        clazz = Class.forName("javax.xml.parsers.SAXParserFactory");
        SAXParserFactory saxParserFactory = (SAXParserFactory) clazz.getMethod("newInstance").invoke(clazz);
        assertThat(saxParserFactory.getClass().getClassLoader(), is(this.getClass().getClassLoader()));

    }


    @Test
    public void shouldLoadClassFromParentLoaderUsingProtectedLoadClassMethod()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar(), new ClassLoader() {
            @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new IllegalStateException("NestedJarClassLoader should not be calling me");
            }

            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                return super.loadClass(name, resolve);
            }
        });
        assertNotNull(Class.forName("javax.xml.parsers.SAXParserFactory", true, nestedJarClassLoader));
    }

    @Test
    public void shouldThrowClassNotFoundExceptionIfParentLoaderThrowsTheSame()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar(), new ClassLoader() {
            @Override public Class<?> loadClass(String name) throws ClassNotFoundException {
                throw new IllegalStateException("NestedJarClassLoader should not be calling me");
            }

            @Override protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                throw new ClassNotFoundException("Class not found for testing");
            }
        });
        try {
            Class.forName("javax.xml.parsers.SAXParserFactory", true, nestedJarClassLoader);
        } catch (Throwable e) {
            assertThat(e.getCause() instanceof ClassNotFoundException, is(true));
        }
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionIfParentLoaderInvocationFails()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar(), new URLClassLoader(((URLClassLoader) this.getClass().getClassLoader()).getURLs()) {
            @Override public Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                throw new IllegalStateException("Deliberate failing");
            }
        });
        Class.forName("javax.xml.parsers.SAXParserFactory", true, nestedJarClassLoader);
    }

    private URL urlForSDKJar() {
        URLClassLoader myLoader = (URLClassLoader) this.getClass().getClassLoader();
        URL[] urls = myLoader.getURLs();
        for (URL urlOfJar : urls) {
            if (urlOfJar.getFile().contains("com.microsoft.tfs.sdk-10.1.0.jar")) {
                return urlOfJar;
            }
        }
        throw new RuntimeException("Could not find ms tfs sdk in classpath");
    }


    @Test
    public void shouldContinueWorkLikeARegularClassLoaderIfTheGivenJarIsNotFound()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException, MalformedURLException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(new URL("file://does_not_exist.jar"));
        Class<?> clazz = Class.forName("javax.xml.parsers.SAXParserFactory", true, nestedJarClassLoader);
        assertThat(clazz.getClassLoader() == null, is(true));
        try {
            clazz.getMethod("newInstance").invoke(clazz);
        } catch (Exception e) {
            fail("should cast successfully");
        }
    }

    @Test
    public void regularResourceDelegationWillNotWorkWithThisClassLoader()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar());
        URL bootstrapResource = this.getClass().getClassLoader().getResource("javax/swing/text/html/default.css");
        assertThat(bootstrapResource, is(not(nullValue())));
        bootstrapResource = nestedJarClassLoader.getResource("sound.properties");
        assertThat(bootstrapResource, is(nullValue()));


    }

    @Test
    public void shouldHonourExcludesListDuringClassLoading()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar(), "javax/xml", "org/apache/xerces", "org/xml", "log4j.properties");
        Class<?> clazz = Class.forName("javax.xml.parsers.SAXParserFactory", true, nestedJarClassLoader);
        SAXParserFactory saxParserFactory = (SAXParserFactory) clazz.getMethod("newInstance").invoke(clazz);
        assertNotNull(saxParserFactory);
    }

    @Test
    public void shouldHonourExcludesListDuringResourceLoading()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        String loggerProperties = "log4j.properties";
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar(), "SAXParserFactory.class", loggerProperties);
        URL actual = nestedJarClassLoader.getResource(loggerProperties);
        URL expected = this.getClass().getClassLoader().getResource(loggerProperties);
        assertThat(actual, is(expected));
    }

    @Test
    public void regularResourceStreamDelegationWillWorkWithThisClassLoader()
            throws ClassNotFoundException, IllegalAccessException, InstantiationException, NoSuchMethodException, InvocationTargetException {
        ClassLoader nestedJarClassLoader = new NestedJarClassLoader(urlForSDKJar());
        InputStream bootstrapResource = this.getClass().getClassLoader().getResourceAsStream("javax/swing/text/html/default.css");
        assertThat(bootstrapResource, is(not(nullValue())));
        bootstrapResource = nestedJarClassLoader.getResourceAsStream("sound.properties");
        assertThat(bootstrapResource, is(nullValue()));


    }
}
