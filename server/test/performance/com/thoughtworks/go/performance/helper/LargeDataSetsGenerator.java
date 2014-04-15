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

package com.thoughtworks.go.performance.helper;

import java.io.File;
import static java.util.Arrays.asList;
import java.util.List;

import com.thoughtworks.go.util.GoConfigFileHelper;
import static com.thoughtworks.go.helper.PipelineMother.completedPipelineWithStagesAndBuilds;
import static com.thoughtworks.go.helper.PipelineMother.firstStageBuildingAndSecondStageScheduled;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import org.apache.commons.io.FileUtils;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class LargeDataSetsGenerator {
    private static final List<String> BASE_STAGE_NAMES = asList("stage1", "stage2", "stage3", "stage4");
    private static final List<String> BASE_BUILD_NAMES = asList("build1", "build2", "build3", "build4", "build5");
    private static final int DEFAULT_NUM_OF_PIPELINES = 100;

    //TODO: right now I run this manually:
    // 1. in IntelliJ, after running 'ant standalone rebuild-db'
    // 2. cruise-config.xml created in "server/test/data/4stages5builds-cruise-config.xml"
    // 3. ant stop-db
    // 4. manually the server/db/hgsqldb/cruise.script, and server/test/data/4stages5builds-cruise-config.xml,
    //    to your server

    public static void main(String... args) throws Exception {
        String action = args.length > 0 ? args[0] : null;
        int numOfPipelines = args.length > 1 ? Integer.parseInt(args[1]) : DEFAULT_NUM_OF_PIPELINES;
        chooseAction(action, numOfPipelines).doit();
    }

    private static Action chooseAction(String action, int numOfPipelines) {
        if ("gendata".equalsIgnoreCase(action)) {
            return new GenData(numOfPipelines);
        }
        if ("genconfig".equalsIgnoreCase(action)) {
            return new GenConfig();
        }
        if ("all".equalsIgnoreCase(action)) {
            return new GenAll(numOfPipelines);
        }
        return new Usage();
    }

    private interface Action {
        void doit() throws Exception;
    }

    private static class GenData implements Action {
        private final int numOfPipelines;
        private final PipelineSqlMapDao pipelineDao;
        private final DatabaseAccessHelper dbHelper;

        public GenData(int numOfPipelines) {
            ClassPathXmlApplicationContext context =
                    new ClassPathXmlApplicationContext("classpath*:WEB-INF/applicationContext-dataLocalAccess.xml");
            this.numOfPipelines = numOfPipelines;
            this.dbHelper = new DatabaseAccessHelper();
            this.pipelineDao = (PipelineSqlMapDao) context.getBean("pipelineDao");
        }

        public void doit() throws Exception {
            dbHelper.onTearDown();
            dbHelper.onSetUp();
            for (int i = 0; i < numOfPipelines; i++) {
                pipelineDao.saveWithStages(
                        completedPipelineWithStagesAndBuilds("studios", BASE_STAGE_NAMES, BASE_BUILD_NAMES));
            }

            pipelineDao.saveWithStages(
                    firstStageBuildingAndSecondStageScheduled("studios", BASE_STAGE_NAMES, BASE_BUILD_NAMES));
        }
    }

    private static class GenConfig implements Action {
        public void doit() throws Exception {
            GoConfigFileHelper configFileHelper = new GoConfigFileHelper();

            configFileHelper.initializeConfigFile();
            String pipelineName = "studios";
            String firstStageName = BASE_STAGE_NAMES.get(0);
            configFileHelper.addPipeline(pipelineName, firstStageName, (String[]) BASE_BUILD_NAMES.toArray());

            for (String baseStageName : BASE_STAGE_NAMES.subList(1, BASE_STAGE_NAMES.size())) {
                configFileHelper.addStageToPipeline(pipelineName, baseStageName, (String[]) BASE_BUILD_NAMES.toArray());
            }

            FileUtils.copyFile(configFileHelper.getConfigFile(),
                    new File("server/test/data/", "4stages5builds-cruise-config.xml"));
        }
    }

    private static class GenAll implements Action {
        private final LargeDataSetsGenerator.GenData genData;
        private final LargeDataSetsGenerator.GenConfig genConfig;

        public GenAll(int numOfPipelines) {
            genData = new GenData(numOfPipelines);
            genConfig = new GenConfig();
        }

        public void doit() throws Exception {
            genData.doit();
            genConfig.doit();
        }
    }

    private static class Usage implements Action {
        public void doit() {
            System.out.println("Usage: genData/genConfig/all [numberOfPipelines]");
            System.out.println("\tnumberOfPipelines default: " + DEFAULT_NUM_OF_PIPELINES);
        }
    }


}
