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

import com.thoughtworks.go.domain.BaseCollection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.apache.commons.lang.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

import java.io.*;
import java.util.Collection;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;
import static java.text.MessageFormat.format;
import static org.hamcrest.core.IsNot.not;
import static org.junit.Assert.assertEquals;

public class TestUtils {
    private static final PrintStream DROP_STREAM = new PrintStream(new OutputStream() {
        public void write(int i) throws IOException {
        }
    });
    private static PrintStream systemOut;
    private static PrintStream systemErr;
    private static ByteArrayOutputStream console;

    public static void extractZipToDir(File repoZip, File repoDir) throws IOException {
        ZipInputStream is = null;
        try {
            is = new ZipInputStream(new FileInputStream(repoZip));
            ZipEntry entry = null;
            while((entry = is.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    FileUtils.forceMkdir(new File(repoDir, entry.getName()));
                } else {
                    File file = new File(repoDir, entry.getName());
                    FileUtils.forceMkdir(file.getParentFile());
                    FileOutputStream entryOs = null;
                    try {
                        entryOs = new FileOutputStream(file);
                        IOUtils.copy(is, entryOs);
                    } finally {
                        if (entryOs != null) {
                            entryOs.close();
                        }
                    }
                }
            }
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }

    protected TestUtils() {

    }

    public static void assertContains(String fullString, String thatIsContained) {
        assertEquals(thatIsContained + " was not found in " + fullString, true,
                StringUtils.contains(fullString, thatIsContained));
    }

    public static Emptymatcher empty() {
        return new Emptymatcher();
    }

    public static SizeMatcher sizeIs(int size) {
        return new SizeMatcher(size);
    }

    public static Matcher notEmpty() {
        return not(new Emptymatcher());
    }

    public static TypeSafeMatcher<File> exists() {
        return new TypeSafeMatcher<File>() {
            private File actual;

            public boolean matchesSafely(File file) {
                actual = file;
                return file.exists();
            }

            public void describeTo(Description description) {
                description.appendText("file exist: " + actual.getAbsolutePath());
            }
        };
    }

    public static TypeSafeMatcher<File> isSamePath(final File fileToMatch) {
        return new TypeSafeMatcher<File>() {
            public String actual;
            public String expected;

            public boolean matchesSafely(File o) {
                actual = o.getPath();
                expected = fileToMatch.getPath();
                return actual.equals(expected);
            }

            public void describeTo(Description description) {
                description.appendText("The actual path: [" + actual + "] does not match the expected path [" + expected + "]");
            }
        };
    }

    public static TypeSafeMatcher<String> isEquivalentPathName(final String pathToMatch) {
        return new TypeSafeMatcher<String>() {
            public String actual;
            public String expected;

            public boolean matchesSafely(String o) {
                actual = new File(o).getPath();
                expected = new File(pathToMatch).getPath();
                return actual.equals(expected);
            }

            public void describeTo(Description description) {
                description.appendText("The actual path:" + actual + "does not match the expected path:" + expected);
            }
        };
    }

    public static Matcher<? super String> isSameAsPath(final String expected) {
        final String expectedPath = expected.replace('/', File.separatorChar);

        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String actual) {
                return expectedPath.equals(actual);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(expectedPath);
            }
        };
    }

    public static TypeSafeMatcher<String> contains(final String toMatch) {
        return new TypeSafeMatcher<String>() {
            public boolean matchesSafely(String underTest) {
                return underTest.contains(toMatch);
            }

            public void describeTo(Description description) {
                description.appendText("The actual string does not contain the expected string.");
            }
        };
    }

    public static void copyAndClose(InputStream fileInputStream, OutputStream fileOutputStream) {
        try {
            IOUtils.copy(fileInputStream, fileOutputStream);
        } catch (IOException e) {
            bomb(e);
        } finally {
            IOUtils.closeQuietly(fileInputStream);
            IOUtils.closeQuietly(fileOutputStream);
        }
    }

    public static void copyAndClose(String inputFileName, String outputFileName) throws IOException {
        FileInputStream fileInputStream = new FileInputStream(inputFileName);
        FileOutputStream fileOutputStream = new FileOutputStream(outputFileName);
        copyAndClose(fileInputStream, fileOutputStream);
    }

    public static String console() {
        return console.toString();
    }

    public static void sleepQuietly(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public static class Emptymatcher extends BaseMatcher<BaseCollection> {

        public boolean matches(Object obj) {
            return ((BaseCollection) obj).size() == 0;
        }

        public void describeTo(Description description) {
            description.appendText("List is not empty");
        }
    }

    public static class SizeMatcher extends BaseMatcher<Collection> {
        private final int expectedSize;
        private int actualSize;

        public SizeMatcher(int expectedSize) {

            this.expectedSize = expectedSize;
        }

        public boolean matches(Object obj) {
            actualSize = ((Collection) obj).size();
            return actualSize == expectedSize;
        }

        public void describeTo(Description description) {
            description.appendText(format("The size was expected to {0} but was {1}", expectedSize, actualSize));
        }
    }

    public static void suppressConsoleOutput() {
        console = new ByteArrayOutputStream();
        PrintStream collector = new PrintStream(console);
        systemOut = System.out;
        systemErr = System.err;
        System.setOut(collector);
        System.setErr(collector);
    }

    public static void restoreConsoleOutput() {
        System.setOut(systemOut);
        System.setErr(systemErr);
    }

}
