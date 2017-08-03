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

import com.thoughtworks.go.domain.*;
import com.thoughtworks.go.domain.exception.IllegalArtifactLocationException;
import com.thoughtworks.go.helper.JobInstanceMother;
import com.thoughtworks.go.server.dao.*;
import com.thoughtworks.go.util.SystemEnvironment;
import org.apache.commons.io.FileUtils;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.JettyWebXmlConfiguration;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.jetty.webapp.WebInfConfiguration;
import org.eclipse.jetty.webapp.WebXmlConfiguration;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import javax.sql.DataSource;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.go.helper.JobInstanceMother.completed;
import static com.thoughtworks.go.helper.PipelineMother.completedPipelineWithStagesAndBuilds;

public class Localhost {

    private static final int PORT = 7493;

    private final Server server;
    protected final List<String> baseStageNames;
    protected final List<String> baseBuildNames;
    protected final List<String> pipelineNames;

    Localhost(int port, String overrideConfigFilePath, List<String> pipelineNames, List<String> baseStageNames,
              List<String> baseBuildNames) throws Exception {
        this.pipelineNames = pipelineNames;
        this.baseStageNames = baseStageNames;
        this.baseBuildNames = baseBuildNames;

        File configXml = DataUtils.getConfigXmlOfWebApp();
        File srcFile;
        if (overrideConfigFilePath == null) {
            srcFile = DataUtils.getConfigXmlAsFile();
        } else {
            srcFile = new File(overrideConfigFilePath);
        }
        FileUtils.copyFile(srcFile, configXml);
        new SystemEnvironment().setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, configXml.getAbsolutePath());
        new SystemEnvironment().setProperty("jdbc.port", "9003");

        server = new Server(port);
        WebAppContext context = new WebAppContext("webapp", "/go");

		context.setConfigurationClasses(new String[]{
				WebInfConfiguration.class.getCanonicalName(),
				WebXmlConfiguration.class.getCanonicalName(),
				JettyWebXmlConfiguration.class.getCanonicalName()
		});

        context.setDefaultsDescriptor("webapp/WEB-INF/webdefault.xml");
        server.setHandler(context);
        this.setCookieExpireIn6Months(context);
    }

    private void setCookieExpireIn6Months(WebAppContext wac) {
        int sixMonths = 60 * 60 * 24 * 180;
		wac.getSessionHandler().getSessionManager().getSessionCookieConfig().setMaxAge(sixMonths);
    }

    public static void main(String[] args) throws Exception {
        DataUtils.cloneCCHome();

        int port = PORT;
        String sourceConfigFilePath = null;
        int numberOfPipelines = 10;

        Localhost localhost = new Localhost(port, sourceConfigFilePath,
                Arrays.asList("studios", "evolve"),
                Arrays.asList("mingle", "cruise", "stage3", "stage4"),
                Arrays.asList("functional", "unit", "build3", "build4", "build5"));
        mainAction(localhost, numberOfPipelines);
    }

    protected static void mainAction(Localhost localhost, int numberOfPipelines) throws Exception {
        startGoServer(localhost);
//        localhost.prepareSampleData(numberOfPipelines);
    }

    private static void startGoServer(final Localhost localhost) {
        new Thread(() -> {
            try {
                localhost.server.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    protected void prepareSampleData(int numberOfPipelines) throws Exception {
        ClassPathXmlApplicationContext context =
                new ClassPathXmlApplicationContext("classpath*:WEB-INF/applicationContext-dataLocalAccess.xml");
        DataSource dataSource = (DataSource) context.getBean("dataSource");
        PipelineSqlMapDao pipelineDao = (PipelineSqlMapDao) context.getBean("pipelineDao");
        StageSqlMapDao stageDao = (StageSqlMapDao) context.getBean("stageDao");
        JobInstanceDao jobInstanceDao = (JobInstanceDao) context.getBean("buildInstanceDao");

        final DatabaseAccessHelper dbHelper = new DatabaseAccessHelper(dataSource);
        dbHelper.onTearDown();
        dbHelper.onSetUp();
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                dbHelper.onTearDown();
            } catch (Exception e) {
            }
        }));

        for (int i = 0; i < numberOfPipelines; i++) {
            for (String pipelineName : pipelineNames) {
                createCompletedData(pipelineDao, pipelineName);
            }
        }
        createLatestPipelines(dbHelper, jobInstanceDao);
    }

    protected void createLatestPipelines(DatabaseAccessHelper dbHelper,
                                         JobInstanceDao jobInstanceDao)
            throws SQLException, IllegalArtifactLocationException, IOException {
        Stage mingleStage = dbHelper.saveTestPipelineWithoutSchedulingBuilds("studios", "mingle").getStages().get(0);
        long mingleStageId = mingleStage.getId();

        Pipeline mingle = dbHelper.getPipelineDao().mostRecentPipeline("studios");
        saveBuildPlanAndCreateLogFile(mingle, completed("functional", JobResult.Failed), jobInstanceDao);
        saveBuildPlanAndCreateLogFile(mingle, completed("unit", JobResult.Passed), jobInstanceDao);

        long evolveStageId = dbHelper.saveTestPipeline("evolve", "dev").getStages().get(0).getId();
        JobInstance building = JobInstanceMother.building("jobConfig1");

        Pipeline pipeline = dbHelper.getPipelineDao().mostRecentPipeline("evolve");

        jobInstanceDao.save(evolveStageId, building);
    }

    private void createCompletedData(PipelineDao pipelineDao, String pipelineName) throws SQLException {
        pipelineDao.saveWithStages(completedPipelineWithStagesAndBuilds(pipelineName, baseStageNames, baseBuildNames));
    }


    private void saveBuildPlanAndCreateLogFile(Pipeline pipeline, JobInstance jobInstance,
                                               JobInstanceDao jobInstanceDao) throws IllegalArtifactLocationException, IOException {
        jobInstanceDao.save(jobInstance.getStageId(), jobInstance);
        JobIdentifier jobIdentifier = new JobIdentifier(pipeline.getName(), pipeline.getLabel(),
                jobInstance.getStageName(), "1", jobInstance.getName());
        jobInstance.setIdentifier(jobIdentifier);
        LogFileHelper.createInstanceForLocalhost().createLogFileForBuildInstance(jobInstance);
    }

}

