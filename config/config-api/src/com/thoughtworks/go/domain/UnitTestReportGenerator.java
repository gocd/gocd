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

package com.thoughtworks.go.domain;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.text.MessageFormat;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.XmlUtils;
import com.thoughtworks.go.util.XpathUtils;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.BOMInputStream;
import org.jdom.Document;
import org.jdom.JDOMException;

public class UnitTestReportGenerator implements TestReportGenerator {
    private final File folderToUpload;
    private GoPublisher publisher;
    XPathFactory factory = XPathFactory.newInstance();

    public UnitTestReportGenerator(GoPublisher publisher, File folderToUpload) {
        this.publisher = publisher;
        this.folderToUpload = folderToUpload;
    }

    public Properties generate(File[] allTestFiles) {
        FileOutputStream transformedHtml = null;
        File mergedResults = new File(folderToUpload.getAbsolutePath() + FileUtil.fileseparator() + TEST_RESULTS_FILE);
        File mergedResource = null;
        FileInputStream mergedFileStream = null;
        try {
            mergedResource = mergeAllTestResultToSingleFile(allTestFiles);
            transformedHtml = new FileOutputStream(mergedResults);

            try {
                mergedFileStream = new FileInputStream(mergedResource);
                InputStream xslt = getClass().getResourceAsStream("unittests.xsl");
                Source xmlSource = new StreamSource(mergedFileStream);
                Source xsltSource = new StreamSource(xslt);
                TransformerFactory transFact = TransformerFactory.newInstance();
                Transformer trans = transFact.newTransformer(xsltSource);
                StreamResult result = new StreamResult(transformedHtml);
                trans.transform(xmlSource, result);
            } catch (Exception e) {
                publisher.reportErrorMessage("Unable to publish test properties. Error was " + e.getMessage(), e);
            } finally {
                IOUtils.closeQuietly(mergedFileStream);
            }

            extractProperties(mergedResults);

            publisher.upload(mergedResults, "testoutput");

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
            String output = XpathUtils.evaluate(factory, xmlFile, xpath);
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
                for (Object file : FileUtils.listFiles(testFile,new String[]{"xml"},true)) {
                   pumpFileContentIfValid(out, (File)file);
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
        try {
            String content = FileUtil.readToEnd(new BOMInputStream(new FileInputStream(file)));
            final Document document = XmlUtils.buildXmlDocument(content);
            XmlUtils.writeXml(document.getRootElement(), out);
        } catch (JDOMException e) {
            publisher.consumeLine(MessageFormat.format("The file {0} could not be parsed as XML document: {1}", file.getName(), e.getMessage()));
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
