/*
 * Copyright Thoughtworks, Inc.
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
package com.thoughtworks.go.server.service;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.EnvironmentVariableConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.EnvironmentVariable;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.security.CryptoException;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.domain.MaterialForScheduling;
import com.thoughtworks.go.server.domain.PipelineScheduleOptions;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.materials.MaterialUpdateStatusNotifier;
import com.thoughtworks.go.server.materials.MaterialUpdateSuccessfulMessage;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.scheduling.TriggerMonitor;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.service.result.HttpOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.apache.commons.collections4.IterableUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {
        "classpath:/applicationContext-global.xml",
        "classpath:/applicationContext-dataLocalAccess.xml",
        "classpath:/testPropertyConfigurer.xml",
        "classpath:/spring-all-servlet.xml",
})
public class PipelineTriggerServiceIntegrationTest {
    @Autowired
    private TriggerMonitor triggerMonitor;
    @Autowired
    private PipelineTriggerService pipelineTriggerService;
    @Autowired
    private PipelineConfigService pipelineConfigService;
    @Autowired
    private PipelinePauseService pipelinePauseService;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private MaterialUpdateStatusNotifier materialUpdateStatusNotifier;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private EntityHashingService entityHashingService;
    @Autowired
    private ScheduleService scheduleService;
    @Autowired
    private PipelineScheduleQueue pipelineScheduleQueue;
    @Autowired
    private PipelineSqlMapDao pipelineSqlMapDao;
    private String pipelineName;
    private String stageName;
    private String jobName;
    private PipelineConfig pipelineConfig;
    private SvnMaterial svnMaterial;
    private ScheduleTestUtil u;
    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private HttpOperationResult result;
    private Username admin;
    private String group;

    @BeforeEach
    public void setUp() throws Exception {
        admin = new Username("admin1");
        pipelineName = UUID.randomUUID().toString();
        stageName = UUID.randomUUID().toString();
        jobName = UUID.randomUUID().toString();
        group = UUID.randomUUID().toString();
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        int i = 1;
        svnMaterial = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svnRevs = {"s1", "s2", "s3"};
        u.checkinInOrder(svnMaterial, u.d(i++), svnRevs);

        configHelper.addSecurityWithAdminConfig();
        pipelineConfig = PipelineConfigMother.createManualTriggerPipelineConfig(svnMaterial.config(), pipelineName, stageName, jobName);
        pipelineConfigService.createPipelineConfig(admin, pipelineConfig, new HttpLocalizedOperationResult(), group);
        configHelper.addAuthorizedUserForPipelineGroup(admin.getDisplayName(), group);
        pipelinePauseService.unpause(pipelineName);
        pipelineScheduleQueue.clear();
        result = new HttpOperationResult();
    }

    @AfterEach
    public void tearDown() throws Exception {
        pipelineScheduleQueue.clear();
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Test
    public void shouldScheduleAPipelineWithLatestRevisionOfAssociatedMaterial() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline %s accepted", this.pipelineName));
        assertThat(result.httpCode()).isEqualTo(202);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isTrue();

        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(svnMaterial, 1));

        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(pipelineNameCaseInsensitive);
        assertNotNull(buildCause);
        assertThat(buildCause.getApprover()).isEqualTo(CaseInsensitiveString.str(admin.getUsername()));
        assertThat(buildCause.getMaterialRevisions().findRevisionFor(pipelineConfig.materialConfigs().first()).getLatestRevisionString()).isEqualTo("s3");
        assertThat(buildCause.getBuildCauseMessage()).isEqualTo("Forced by admin1");
        assertTrue(buildCause.getVariables().isEmpty());
    }

    @Test
    public void shouldScheduleAPipelineWithTheProvidedMaterialRevisions() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        String peggedRevision = "s2";
        MaterialForScheduling material = new MaterialForScheduling(pipelineConfig.materialConfigs().first().getFingerprint(), peggedRevision);
        pipelineScheduleOptions.getMaterials().add(material);

        pipelineTriggerService.schedule(pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline %s accepted", pipelineName));
        assertThat(result.httpCode()).isEqualTo(202);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isTrue();

        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(svnMaterial, 1));

        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(pipelineNameCaseInsensitive);
        assertNotNull(buildCause);
        assertThat(buildCause.getApprover()).isEqualTo(CaseInsensitiveString.str(admin.getUsername()));
        assertThat(buildCause.getMaterialRevisions().findRevisionFor(pipelineConfig.materialConfigs().first()).getLatestRevisionString()).isEqualTo("s2");
        assertThat(buildCause.getBuildCauseMessage()).isEqualTo("Forced by admin1");
        assertTrue(buildCause.getVariables().isEmpty());
    }

    @Test
    public void shouldNotPerformMDUIfScheduleOptionsIsSetToDisallowMDU() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        pipelineScheduleOptions.shouldPerformMDUBeforeScheduling(false);
        String peggedRevision = "s2";
        MaterialForScheduling material = new MaterialForScheduling(pipelineConfig.materialConfigs().first().getFingerprint(), peggedRevision);
        pipelineScheduleOptions.getMaterials().add(material);

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline %s accepted", this.pipelineName));
        assertThat(result.httpCode()).isEqualTo(202);
        assertThat(materialUpdateStatusNotifier.hasListenerFor(pipelineConfig)).isFalse();
        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(pipelineNameCaseInsensitive);
        assertNotNull(buildCause);
        assertThat(buildCause.getApprover()).isEqualTo(CaseInsensitiveString.str(admin.getUsername()));
        assertThat(buildCause.getMaterialRevisions().findRevisionFor(pipelineConfig.materialConfigs().first()).getLatestRevisionString()).isEqualTo("s2");
        assertThat(buildCause.getBuildCauseMessage()).isEqualTo("Forced by admin1");
        assertTrue(buildCause.getVariables().isEmpty());
    }

    @Test
    public void shouldNotScheduleAPipelineIfTheProvidedMaterialRevisionIsNotKnownAndScheduleOptionSuggestsNoMDU() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        pipelineScheduleOptions.shouldPerformMDUBeforeScheduling(false);
        String peggedRevision = "unseen-revision";
        String fingerprint = pipelineConfig.materialConfigs().first().getFingerprint();
        MaterialForScheduling material = new MaterialForScheduling(fingerprint, peggedRevision);
        pipelineScheduleOptions.getMaterials().add(material);

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo(String.format("Error while scheduling pipeline: %s { Unable to find revision [%s] for material [%s] }", this.pipelineName, peggedRevision, new MaterialConfigConverter().toMaterial(pipelineConfig.materialConfigs().first())));
        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
    }

    @Test
    public void shouldScheduleAPipelineWithTheProvidedEnvironmentVariables() {
        pipelineConfig.addEnvironmentVariable("ENV_VAR1", "VAL1");
        pipelineConfig.addEnvironmentVariable("ENV_VAR2", "VAL2");
        pipelineConfig.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", "SECURE_VAL", true));
        pipelineConfig.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR2", "SECURE_VAL2", true));
        String digest = entityHashingService.hashForEntity(pipelineConfigService.getPipelineConfig(pipelineConfig.name().toString()), group);
        pipelineConfigService.updatePipelineConfig(admin, pipelineConfig, group, digest, new HttpLocalizedOperationResult());
        Integer pipelineCounterBefore = pipelineSqlMapDao.getCounterForPipeline(pipelineName);

        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "ENV_VAR1", "overridden_value", false));
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", "overridden_secure_value", true));

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline %s accepted", this.pipelineName));
        assertThat(result.httpCode()).isEqualTo(202);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isTrue();

        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(svnMaterial, 1));

        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(pipelineNameCaseInsensitive);
        assertNotNull(buildCause);
        assertThat(buildCause.getApprover()).isEqualTo(CaseInsensitiveString.str(admin.getUsername()));
        assertThat(buildCause.getMaterialRevisions().findRevisionFor(pipelineConfig.materialConfigs().first()).getLatestRevisionString()).isEqualTo("s3");
        assertThat(buildCause.getBuildCauseMessage()).isEqualTo("Forced by admin1");
        assertThat(buildCause.getVariables().size()).isEqualTo(2);
        EnvironmentVariable plainTextVariable = IterableUtils.find(buildCause.getVariables(), variable -> variable.getName().equals("ENV_VAR1"));
        EnvironmentVariable secureVariable = IterableUtils.find(buildCause.getVariables(), variable -> variable.getName().equals("SECURE_VAR1"));
        assertThat(plainTextVariable.getValue()).isEqualTo("overridden_value");
        assertThat(secureVariable.getValue()).isEqualTo("overridden_secure_value");
        assertThat(secureVariable.isSecure()).isTrue();

        scheduleService.autoSchedulePipelinesFromRequestBuffer();

        Integer pipelineCounterAfter = pipelineSqlMapDao.getCounterForPipeline(this.pipelineName);
        assertThat(pipelineCounterAfter).isEqualTo(pipelineCounterBefore + 1);
        BuildCause buildCauseOfLatestRun = pipelineSqlMapDao.findBuildCauseOfPipelineByNameAndCounter(this.pipelineName, pipelineCounterAfter);
        assertThat(buildCauseOfLatestRun).isEqualTo(buildCause);
    }

    @Test
    public void shouldScheduleAPipelineWithTheProvidedEncryptedEnvironmentVariable() throws CryptoException {
        pipelineConfig.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", "SECURE_VAL", true));
        String digest = entityHashingService.hashForEntity(pipelineConfigService.getPipelineConfig(pipelineConfig.name().toString()), group);
        pipelineConfigService.updatePipelineConfig(admin, pipelineConfig, group, digest, new HttpLocalizedOperationResult());
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        String overriddenEncryptedValue = new GoCipher().encrypt("overridden_value");
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", overriddenEncryptedValue));

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, this.result);

        assertThat(this.result.isSuccess()).isTrue();
        materialUpdateStatusNotifier.onMessage(new MaterialUpdateSuccessfulMessage(svnMaterial, 1));

        BuildCause buildCause = pipelineScheduleQueue.toBeScheduled().get(pipelineNameCaseInsensitive);
        assertNotNull(buildCause);
        EnvironmentVariable secureVariable = IterableUtils.find(buildCause.getVariables(), variable -> variable.getName().equals("SECURE_VAR1"));
        assertThat(secureVariable.getValue()).isEqualTo("overridden_value");
        assertThat(secureVariable.isSecure()).isTrue();
    }

    @Test
    public void shouldNotScheduleAPipelineWithTheJunkEncryptedEnvironmentVariable() {
        pipelineConfig.addEnvironmentVariable(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", "SECURE_VAL", true));
        String digest = entityHashingService.hashForEntity(pipelineConfigService.getPipelineConfig(pipelineConfig.name().toString()), group);
        pipelineConfigService.updatePipelineConfig(admin, pipelineConfig, group, digest, new HttpLocalizedOperationResult());
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        String overriddenEncryptedValue = "some_junk";
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", overriddenEncryptedValue));

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, this.result);

        assertThat(this.result.isSuccess()).isFalse();
        assertThat(this.result.fullMessage()).isEqualTo("Request to schedule pipeline rejected { Encrypted value for variable named 'SECURE_VAR1' is invalid. This usually happens when the cipher text is modified to have an invalid value. }");
        assertThat(this.result.httpCode()).isEqualTo(422);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
    }

    @Test
    public void shouldReturnErrorIfThePipelineBeingScheduledDoesnotExist() {
        String pipelineName = "does-not-exist";
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();

        pipelineTriggerService.schedule(pipelineName, pipelineScheduleOptions, admin, result);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo("Pipeline 'does-not-exist' not found.");
        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString(pipelineName))).isFalse();
    }

    @Test
    public void shouldReturnErrorIfTheFirstStageOfThePipelineBeingScheduledIsAlreadyRunning() {
        pipelineTriggerService.schedule(pipelineName, new PipelineScheduleOptions(), admin, result);
        assertThat(result.isSuccess()).isTrue();

        pipelineTriggerService.schedule(pipelineName, new PipelineScheduleOptions(), admin, result);
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo(String.format("Failed to trigger pipeline: %s { Pipeline already triggered }", pipelineName));
        assertThat(result.getServerHealthState().getDescription()).isEqualTo("Pipeline already triggered");
        assertThat(result.httpCode()).isEqualTo(409);
    }

    @Test
    public void shouldReturnErrorIfThePipelineBeingScheduledDoesnotExistAndHasMaterialsSetInRequest() {
        String pipelineName = "does-not-exist";
        assertThat(triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString(pipelineName))).isFalse();

        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        pipelineScheduleOptions.getMaterials().add(new MaterialForScheduling("non-existant-material", "r1"));
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "ENV_VAR1", "overridden_value", false));
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", "overridden_secure_value", true));

        pipelineTriggerService.schedule(pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo("Pipeline 'does-not-exist' not found.");
        assertThat(result.httpCode()).isEqualTo(404);
        assertThat(triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString(pipelineName))).isFalse();
    }

    @Test
    public void shouldReturnErrorIfThePipelineBeingScheduledDoesNotContainTheMaterialsSetInRequest() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        MaterialForScheduling material = new MaterialForScheduling("non-existant-material", "r1");
        pipelineScheduleOptions.getMaterials().add(material);

        pipelineTriggerService.schedule(this.pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline rejected { Pipeline '%s' does not contain the following material(s): [non-existant-material]. }", this.pipelineName));
        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
    }

    @Test
    public void shouldReturnErrorWhenSchedulingAPipelineWithUnconfiguredEnvironmentVariables() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "ENV_VAR1", "value", false));
        pipelineScheduleOptions.getAllEnvironmentVariables().add(new EnvironmentVariableConfig(new GoCipher(), "SECURE_VAR1", "value", true));

        pipelineTriggerService.schedule(pipelineName, pipelineScheduleOptions, admin, result);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline rejected { Variable 'ENV_VAR1' has not been configured for pipeline '%s' }", pipelineName));
        assertThat(result.httpCode()).isEqualTo(422);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
    }

    @Test
    public void shouldReturnErrorWhenAnUnauthorizedUserTriesToScheduleAPipeline() {
        CaseInsensitiveString pipelineNameCaseInsensitive = new CaseInsensitiveString(this.pipelineName);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();

        pipelineTriggerService.schedule(pipelineName, pipelineScheduleOptions, new Username("foo"), result);

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.fullMessage()).isEqualTo(String.format("Failed to trigger pipeline [%s] { User foo does not have permission to schedule %s/%s }", pipelineName, pipelineName, stageName));
        assertThat(result.getServerHealthState().getDescription()).isEqualTo(String.format("User foo does not have permission to schedule %s/%s", pipelineName, pipelineConfig.first().name()));
        assertThat(result.httpCode()).isEqualTo(403);
        assertThat(triggerMonitor.isAlreadyTriggered(pipelineNameCaseInsensitive)).isFalse();
    }

    @Test
    public void shouldErrorOutIfSchedulingAPipelineWithBothValueAndEncryptedValueForAGiveVariable() throws CryptoException {
        PipelineScheduleOptions pipelineScheduleOptions = new PipelineScheduleOptions();
        EnvironmentVariableConfig config = new EnvironmentVariableConfig(new GoCipher(), "SEC_VAR1", "PLAIN", true);
        config.deserialize("SEC_VAR1", "PLAIN", true, new GoCipher().encrypt("ENCRYPTED"));
        pipelineScheduleOptions.getAllEnvironmentVariables().add(config);

        pipelineTriggerService.schedule(pipelineName, pipelineScheduleOptions, new Username("foo"), result);
        System.out.println(result.fullMessage());

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.httpCode()).isEqualTo(422);

        assertThat(result.fullMessage()).isEqualTo(String.format("Request to schedule pipeline rejected { Variable 'SEC_VAR1' has not been configured for pipeline '%s' }", pipelineName));
        assertThat(triggerMonitor.isAlreadyTriggered(new CaseInsensitiveString(pipelineName))).isFalse();
    }
}
