/*
 * Copyright 2017 ThoughtWorks, Inc.
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

package com.thoughtworks.go.helpers;

import com.thoughtworks.go.util.ZipUtil;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class LocalhostWithLargeDataSets extends Localhost {
    private static final int PORT = 7493;
    private static final String PIPELINE_NAME = "studios";

    LocalhostWithLargeDataSets(int port, String overrideConfigFilePath, List<String> pipelineNames,
                               List<String> baseStageNames, List<String> baseBuildNames) throws Exception {
        super(port, overrideConfigFilePath, pipelineNames, baseStageNames, baseBuildNames);
    }

    // 1. ant stop-db
    // 2. start this manually in IntelliJ:
    //
    public static void main(String[] args) throws Exception {
        DataUtils.cloneCCHome();

        int port = PORT;
        String sourceConfigFilePath = "test/data/4stages5builds-cruise-config.xml";
        int numberOfPipelines = 5000;

        LocalhostWithLargeDataSets localhostWithLargeDataSets = new LocalhostWithLargeDataSets(port,
                sourceConfigFilePath,
                Arrays.asList(PIPELINE_NAME),
                Arrays.asList("stage1", "stage2", "stage3", "stage4"),
                Arrays.asList("build1", "build2", "build3", "build4", "build5"));
        prepareSampleDataFromZipFile();
        mainAction(localhostWithLargeDataSets, numberOfPipelines);
    }

    private static void prepareSampleDataFromZipFile() throws IOException {
        File dbDir = new File("db/hsqldb");
        File unzipped = unzipDatabaseFile(dbDir);
        FileUtils.copyFile(unzipped, new File(dbDir, "cruise.script"));
    }

    private static File unzipDatabaseFile(File dbDir) throws IOException {
        ZipUtil unzipUtil = new ZipUtil();
        String dataFileName = "5000pipelinesx4stagesx5buildplans.script";
        File srcZip = new File(dbDir, dataFileName + ".zip");
        unzipUtil.unzip(srcZip, dbDir);
        File unzipped = new File(dbDir, dataFileName);
        return unzipped;
    }


}
