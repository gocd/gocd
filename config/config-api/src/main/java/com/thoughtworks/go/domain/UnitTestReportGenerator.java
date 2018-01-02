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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.XpathUtils;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.text.MessageFormat;

public class UnitTestReportGenerator implements TestReportGenerator {
    private final File folderToUpload;
    private GoPublisher publisher;
    private static Templates templates;

    private static final Logger LOG = LoggerFactory.getLogger(UnitTestReportGenerator.class);

    static {
        try (InputStream xslt = UnitTestReportGenerator.class.getResourceAsStream("unittests.xsl")) {
            templates = TransformerFactory.newInstance().newTemplates(new StreamSource(xslt));
        } catch (Exception e) {
            LOG.error("Could not load unit test converters", e);
        }
    }

    public UnitTestReportGenerator(GoPublisher publisher, File folderToUpload) {
        this.publisher = publisher;
        this.folderToUpload = folderToUpload;
    }

    public Properties generate(File[] allTestFiles, String uploadDestPath) {
        FileOutputStream transformedHtml = null;
        File mergedResults = new File(folderToUpload.getAbsolutePath() + FileUtil.fileseparator() + TEST_RESULTS_FILE);
        File mergedResource = null;
        FileInputStream mergedFileStream = null;
        try {
            mergedResource = mergeAllTestResultToSingleFile(allTestFiles);
            transformedHtml = new FileOutputStream(mergedResults);

            try {
                mergedFileStream = new FileInputStream(mergedResource);
                Source xmlSource = new StreamSource(mergedFileStream);
                StreamResult result = new StreamResult(transformedHtml);
                templates.newTransformer().transform(xmlSource, result);
            } catch (Exception e) {
                publisher.reportErrorMessage("Unable to publish test properties. Error was " + e.getMessage(), e);
            }

            extractProperties(mergedResults);
            publisher.upload(mergedResults, uploadDestPath);

            return null;
        } catch (Exception e) {
            publisher.reportErrorMessage("Unable to publish test properties. Error was " + e.getMessage(), e);
        } finally {
            IOUtils.closeQuietly(mergedFileStream);
            IOUtils.closeQuietly(transformedHtml);
            if (mergedResource != null) {
                mergedResource.delete();
            }
        }
        return new Properties();
    }

    private Properties extractProperties(File fileSendToServer) throws XPathExpressionException, FileNotFoundException {
        final Properties properties = new Properties();
        addProperty(fileSendToServer, "tests_total_count", TOTAL_TEST_COUNT);
        addProperty(fileSendToServer, "tests_failed_count", FAILED_TEST_COUNT);
        addProperty(fileSendToServer, "tests_ignored_count", IGNORED_TEST_COUNT);
        addProperty(fileSendToServer, "tests_total_duration", TEST_TIME);
        return properties;
    }

    private File mergeAllTestResultToSingleFile(File[] allTestFiles) throws IOException {
        FileOutputStream mergedResourcesStream = null;
        try {
            File mergedResource = TestFileUtil.createUniqueTempFile("mergedFile.xml");
            mergedResourcesStream = new FileOutputStream(mergedResource);
            merge(allTestFiles, mergedResourcesStream);
            return mergedResource;
        } finally {
            IOUtils.closeQuietly(mergedResourcesStream);
        }
    }

    private void addProperty(File xmlFile, String cssClass, String cruiseProperty) {
        try {
            String xpath = "//div/p/span[@class='" + cssClass + "']";
            String output = XpathUtils.evaluate(xmlFile, xpath);
            output = output.startsWith(".") ? "0" + output : output;
            Property property = new Property(cruiseProperty, output);
            publisher.setProperty(property);
        } catch (Exception e) {
            publisher.consumeLine("Could not publish property " + e.getMessage());
        }
    }

    public void merge(File[] testFiles, OutputStream outputStream) throws IOException {
        PrintStream out = new PrintStream(outputStream, true, "UTF-8");
        out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
        out.println("<all-results>");

        for (File testFile : testFiles) {
            if (testFile.isDirectory()) {
                for (Object file : FileUtils.listFiles(testFile, new String[]{"xml"}, true)) {
                    pumpFileContentIfValid(out, (File) file);
                }
            } else {
                pumpFileContentIfValid(out, testFile);
            }
        }
        out.println("</all-results>");
    }

    private void pumpFileContentIfValid(PrintStream out, File testFile) throws IOException {
        if (!isValidXml(testFile)) {
            return;
        }
        pumpFileContent(testFile, out);
    }

    private void pumpFileContent(File file, PrintStream out) throws IOException {
        BufferedReader bufferedReader = null;
        try {
            bufferedReader = new BufferedReader(new FileReader(file));
            String line = bufferedReader.readLine();
            if (!line.contains("<?xml")) { // skip prolog
                out.println(line);
            }
            while ((line = bufferedReader.readLine()) != null) {
                out.println(line);
            }
        } finally {
            IOUtils.closeQuietly(bufferedReader);
        }
    }

    private boolean isValidXml(File file) {
        try {
            boolean isTestFile = nodeExists(file, "//test-results") || nodeExists(file, "//testsuite");

            if (!isTestFile) {
                publisher.consumeLine(MessageFormat.format("Ignoring file {0} - it is not a recognised test file.", file.getName()));
            }

            return isTestFile;
        } catch (Exception e) {
            publisher.consumeLine(MessageFormat.format("The file {0} could not be parsed. It seems to be invalid.", file.getName()));
            return false;
        }
    }

    private boolean nodeExists(File file, String xpath) {
        try {
            return XpathUtils.nodeExists(file, xpath);
        } catch (Exception ignored) {
            return false;
        }
    }

}
