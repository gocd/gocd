/*
 * Copyright 2015 ThoughtWorks, Inc.
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
 */

package com.thoughtworks.go.config;

import java.io.File;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.thoughtworks.go.domain.ArtifactType;
import com.thoughtworks.go.domain.UnitTestReportGenerator;
import com.thoughtworks.go.domain.WildcardScanner;
import com.thoughtworks.go.util.FileUtil;
import com.thoughtworks.go.work.GoPublisher;
import org.apache.log4j.Logger;

@ConfigTag("test")
public class TestArtifactPlan extends ArtifactPlan {
    private static final Logger LOG = Logger.getLogger(TestArtifactPlan.class);
    public static final String TEST_OUTPUT_FOLDER = "testoutput";
    private final ArrayList<ArtifactPlan> plans = new ArrayList<>();
    static final String MERGED_RESULT_FOLDER = "result";
    public static final String TEST_PLAN_DISPLAY_NAME = "Test Artifact";

    public TestArtifactPlan() {
        super.setArtifactType(ArtifactType.unit);
    }

    public TestArtifactPlan(ArtifactPlan plan) {
        this();
        add(plan);
        setDest(TEST_OUTPUT_FOLDER);
    }

    public TestArtifactPlan(String src, String dest) {
        super(ArtifactType.unit, src, dest);
    }

    public void add(ArtifactPlan plan) {
        plans.add(plan);
    }

    public int size() {
        return plans.size();
    }

    @Override
    public void publish(GoPublisher goPublisher, File rootPath) {
        ArrayList<File> allFiles = uploadTestResults(goPublisher, rootPath);
        mergeAndUploadTestResult(goPublisher, allFiles);
    }

    private ArrayList<File> uploadTestResults(GoPublisher publisher, File rootPath) {
        ArrayList<File> allFiles = new ArrayList<>();
        for (ArtifactPlan plan : plans) {
            final File source = plan.getSource(rootPath);
            WildcardScanner wildcardScanner = new WildcardScanner(rootPath, plan.getSrc());
            File[] files = wildcardScanner.getFiles();

            if (files.length > 0) {
                final List<File> fileList = files == null ? new ArrayList<File>() : Arrays.asList(files);
                allFiles.addAll(fileList);
                for (File file : fileList) {
                    String destPath = destURL(rootPath, file, plan.getSrc(), plan.getDest());
                    publisher.upload(file, destPath);
                }
            } else {
                final String message = MessageFormat.format("The Directory {0} specified as a test artifact was not found."
                        + " Please check your configuration", FileUtil.normalizePath(source));
                publisher.consumeLineWithPrefix(message);
                LOG.error(message);
            }
        }
        return allFiles;
    }

    private void mergeAndUploadTestResult(GoPublisher publisher, ArrayList<File> allFiles) {
        if (allFiles.size() > 0) {
            File tempFolder = null;
            try {
                tempFolder = FileUtil.createTempFolder();
                File testResultSource = new File(tempFolder, MERGED_RESULT_FOLDER);
                testResultSource.mkdirs();
                UnitTestReportGenerator generator = new UnitTestReportGenerator(publisher, testResultSource);
                generator.generate(allFiles.toArray(new File[allFiles.size()]), "testoutput");
                publisher.upload(testResultSource, "testoutput");
            } finally {
                if (tempFolder!=null) {
                    FileUtil.deleteFolder(tempFolder);
                }
            }

        } else {
            String message = "No files were found in the Test Results folders";
            publisher.consumeLineWithPrefix(message);
            LOG.warn(message);
        }
    }

    @Override
    public void printSrc(StringBuilder builder) {
        if (getSrc() != null) {
            builder.append('[').append(getSrc()).append(']');
        } else {
            Iterator<ArtifactPlan> planIterator = plans.iterator();
            builder.append('[');
            while (planIterator.hasNext()) {
                ArtifactPlan plan = planIterator.next();
                builder.append(plan.getSrc());
                if (planIterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(']');
        }
    }

    @Override
    public String getArtifactTypeValue() {
        return TEST_PLAN_DISPLAY_NAME;
    }
}
