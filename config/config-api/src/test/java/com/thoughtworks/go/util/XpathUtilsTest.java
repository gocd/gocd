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

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.xml.sax.InputSource;

import javax.xml.xpath.XPathExpressionException;
import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

public class XpathUtilsTest {
    @TempDir
    public File temporaryFolder;

    private File testFile;
    private static final String XML = """
            <root>
            <son>
            <grandson name="someone"/>
            <grandson name="anyone" address=""></grandson>
            </son>
            </root>""";

    @AfterEach
    public void tearDown() throws Exception {
        if (testFile != null && testFile.exists()) {
            testFile.delete();
        }
    }

    @Test
    public void shouldEvaluateXpath() throws Exception {
        String xpath = "/root/son/grandson/@name";
        String value = XpathUtils.evaluate(getTestFile(), xpath);
        assertThat(value).isEqualTo("someone");
    }

    @Test
    public void shouldEvaluateAnotherXpath() throws Exception {
        String xpath = "//son/grandson[2]/@name";
        String value = XpathUtils.evaluate(getTestFile(), xpath);
        assertThat(value).isEqualTo("anyone");
    }

    @Test
    public void shouldEvaluateTextValueXpath() throws Exception {
        String xpath = "//son/grandson[2]/text()";
        String value = XpathUtils.evaluate(getTestFile(), xpath);
        assertThat(value).isEqualTo("");
    }

    @Test
    public void shouldThrowExceptionForIllegalXpath() {
        assertThrows(XPathExpressionException.class, () -> XpathUtils.evaluate(getTestFile(), "//"));
    }

    @Test
    public void shouldCheckIfNodeExists() throws Exception {
        String attribute = "//son/grandson[@name=\"anyone\"]/@address";
        assertThat(XpathUtils.evaluate(getTestFile(), attribute)).isEqualTo("");
        assertThat(XpathUtils.nodeExists(getTestFile(), attribute)).isTrue();

        String textNode = "//son/grandson[2]/text()";
        assertThat(XpathUtils.nodeExists(getTestFile(), textNode)).isFalse();
    }

    @Test
    public void shouldThrowExceptionForBadXML() {
        String attribute = "//badxpath";
        try {
            XpathUtils.evaluate(getTestFile("NOT XML"), attribute);
            fail("Should throw exception if xml is valid");
        } catch (Exception ignored) {
        }
    }

    @Test
    public void shouldReturnEmptyStringWhenMatchedNodeIsNotTextNode() throws Exception {
        String xpath = "/root/son";
        String value = XpathUtils.evaluate(getTestFile(), xpath);
        assertThat(value).isEqualTo("");
    }

    @Test
    public void shouldParseUTFFilesWithBOM() throws Exception {
        String xpath = "//son/grandson[@name=\"anyone\"]/@address";
        boolean exists = XpathUtils.nodeExists(getTestFileUsingUTFWithBOM(), xpath);

        assertThat(exists).isTrue();
    }

    private File getTestFileUsingUTFWithBOM() throws IOException {
        testFile = File.createTempFile("xpath", null, temporaryFolder);
        saveUtfFileWithBOM(testFile, XML);

        return testFile;
    }

    public static void saveUtfFileWithBOM(File file, String content) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file);
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(fos, UTF_8))) {
            // write UTF8 BOM mark if file is empty
            if (file.length() < 1) {
                final byte[] bom = new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
                fos.write(bom);
            }

            if (content != null) {
                bw.write(content);
            }
        }
    }

    @Test
    public void shouldEvaluateXpathOfCustomer() throws Exception {
        String xpath = "//coverageReport2/project/@coverage";
        File file = new File("src/test/resources/data/customer/CoverageSummary.xml");
        InputSource inputSource = new InputSource(file.getPath());
        assertThat(XpathUtils.nodeExists(inputSource, xpath)).isTrue();
        String value = XpathUtils.evaluate(file, xpath);
        assertThat(value).isEqualTo("27.7730732");
    }

    private File getTestFile() throws IOException {
        return getTestFile(XML);
    }

    private File getTestFile(String xml) throws IOException {
        testFile = File.createTempFile("xpath", null, temporaryFolder);
        FileUtils.writeStringToFile(testFile, xml, UTF_8);
        return testFile;
    }
}



