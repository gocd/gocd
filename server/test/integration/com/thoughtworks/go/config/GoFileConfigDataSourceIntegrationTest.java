package com.thoughtworks.go.config;

import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.util.CachedDigestUtils;
import com.thoughtworks.go.util.ReflectionUtil;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.UUID;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class GoFileConfigDataSourceIntegrationTest {

    private final String DEFAULT_CHARSET = "defaultCharset";
    private final SystemEnvironment systemEnvironment = new SystemEnvironment();
    @Autowired
    private GoFileConfigDataSource dataSource;
    @Autowired
    private GoConfigService configService;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        File configDir = temporaryFolder.newFolder();
        String absolutePath = new File(configDir, "cruise-config.xml").getAbsolutePath();
        systemEnvironment.setProperty(SystemEnvironment.CONFIG_FILE_PROPERTY, absolutePath);
    }

    @After
    public void tearDown() throws Exception {
        ReflectionUtil.setStaticField(Charset.class, DEFAULT_CHARSET, null);
        systemEnvironment.clearProperty(SystemEnvironment.CONFIG_FILE_PROPERTY);
    }

    @Test
    public void shouldConvertToUTF8BeforeSavingConfigToFileSystem() throws IOException {
        ReflectionUtil.setStaticField(Charset.class, DEFAULT_CHARSET, Charset.forName("windows-1252"));
        BasicCruiseConfig config = new BasicCruiseConfig();
        GoFileConfigDataSource.GoConfigSaveResult result = dataSource.writeWithLock(new UpdateConfigCommand() {
            @Override
            public CruiseConfig update(CruiseConfig cruiseConfig) throws Exception {
                PipelineConfig pipelineConfig = PipelineConfigMother.pipelineConfig(UUID.randomUUID().toString());
                JobConfig job = new JobConfig("job");
                ExecTask task = new ExecTask();
                task.setCommand("powershell");
                task.setArgs("Get-ChildItem -Path . â€“Recurse");
                job.addTask(task);
                pipelineConfig.first().getJobs().add(job);
                cruiseConfig.addPipeline(UUID.randomUUID().toString(), pipelineConfig);
                return cruiseConfig;
            }
        }, new GoConfigHolder(config, config));
        assertThat(result.getConfigSaveState(), is(ConfigSaveState.UPDATED));
        FileInputStream inputStream = new FileInputStream(dataSource.fileLocation());
        String newMd5 = CachedDigestUtils.md5Hex(inputStream);
        assertThat(newMd5, is(result.getConfigHolder().config.getMd5()));
    }
}