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
package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.TestFileUtil;
import com.thoughtworks.go.util.XpathUtils;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.RegExUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.regex.Pattern;

public class UnitTestReportGenerator {
    private static final Logger LOG = LoggerFactory.getLogger(UnitTestReportGenerator.class);
    private static final String TEST_RESULTS_FILE = "index.html";
    private static final Pattern LINE_STARTING_WITH_XML_DECLARATION = Pattern.compile("^\\s*<\\?xml.*?\\?>");

    private static Templates templates;

    static {
        try (InputStream xslt = UnitTestReportGenerator.class.getResourceAsStream("unittests.xsl")) {
            templates = TransformerFactory.newInstance().newTemplates(new StreamSource(xslt));
        } catch (Exception e) {
            LOG.error("Could not load unit test converters", e);
        }
    }

    private final File folderToUpload;
    private final GoPublisher publisher;

    public UnitTestReportGenerator(GoPublisher publisher, File folderToUpload) {
        this.publisher = publisher;
        this.folderToUpload = folderToUpload;
    }

    public void generate(File[] allTestFiles, String uploadDestPath) {
        File mergedResults = new File(folderToUpload.getAbsolutePath() + File.separator + TEST_RESULTS_FILE);
        File mergedResource = null;
        try (FileOutputStream transformedHtml = new FileOutputStream(mergedResults)) {
            mergedResource = mergeAllTestResultToSingleFile(allTestFiles);
            try (FileInputStream mergedFileStream = new FileInputStream(mergedResource)) {
                Source xmlSource = new StreamSource(mergedFileStream);
                StreamResult result = new StreamResult(transformedHtml);
                templates.newTransformer().transform(xmlSource, result);
            } catch (Exception e) {
                publisher.reportErrorMessage("Unable to publish test properties. Error was " + e.getMessage(), e);
            }

            publisher.upload(mergedResults, uploadDestPath);

        } catch (Exception e) {
            publisher.reportErrorMessage("Unable to publish test properties. Error was " + e.getMessage(), e);
        } finally {
            if (mergedResource != null) {
                //noinspection ResultOfMethodCallIgnored
                mergedResource.delete();
            }
        }
    }

    private File mergeAllTestResultToSingleFile(File[] allTestFiles) throws IOException {
        File mergedResource = TestFileUtil.createUniqueTempFile("mergedFile.xml");
        try (FileOutputStream mergedResourcesStream = new FileOutputStream(mergedResource)) {
            merge(allTestFiles, mergedResourcesStream);
        }
        return mergedResource;
    }

    public void merge(File[] testFiles, OutputStream outputStream) throws IOException {
        try (PrintStream out = new PrintStream(outputStream, true, StandardCharsets.UTF_8)) {
            out.println("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>");
            out.println("<all-results>");

            for (File testFile : testFiles) {
                if (testFile.isDirectory()) {
                    for (File file : FileUtils.listFiles(testFile, new String[]{"xml"}, true)) {
                        pumpFileContentIfValid(out, file);
                    }
                } else {
                    pumpFileContentIfValid(out, testFile);
                }
            }
            out.println("</all-results>");
        }
    }

    private void pumpFileContentIfValid(PrintStream out, File testFile) throws IOException {
        if (!isValidXml(testFile)) {
            return;
        }
        pumpFileContent(testFile, out);
    }

    private void pumpFileContent(File file, PrintStream out) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {
            String line = bufferedReader.readLine();
            out.println(RegExUtils.removeFirst(line, LINE_STARTING_WITH_XML_DECLARATION));
            while ((line = bufferedReader.readLine()) != null) {
                out.println(line);
            }
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
