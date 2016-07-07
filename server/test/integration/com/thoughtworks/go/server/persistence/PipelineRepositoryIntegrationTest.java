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

package com.thoughtworks.go.server.persistence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.PipelineConfig;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.DefaultSchedulingContext;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineTimelineEntry;
import com.thoughtworks.go.domain.User;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.dao.PipelineSqlMapDao;
import com.thoughtworks.go.server.dao.UserSqlMapDao;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.domain.user.PipelineSelections;
import com.thoughtworks.go.server.service.GoConfigService;
import com.thoughtworks.go.server.service.InstanceFactory;
import com.thoughtworks.go.server.service.ScheduleTestUtil;
import com.thoughtworks.go.server.transaction.TransactionSynchronizationManager;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.TimeProvider;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;

import static com.thoughtworks.go.helper.ModificationsMother.oneModifiedFile;
import static com.thoughtworks.go.helper.PipelineConfigMother.createPipelineConfig;
import static com.thoughtworks.go.helper.PipelineConfigMother.pipelineConfig;
import static com.thoughtworks.go.util.DataStructureUtils.a;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class PipelineRepositoryIntegrationTest {

    @Autowired
    PipelineSqlMapDao pipelineSqlMapDao;
    @Autowired DatabaseAccessHelper dbHelper;
    @Autowired PipelineRepository pipelineRepository;
    @Autowired MaterialRepository materialRepository;
    @Autowired UserSqlMapDao userSqlMapDao;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private TransactionSynchronizationManager transactionSynchronizationManager;
    @Autowired private GoConfigService goConfigService;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private InstanceFactory instanceFactory;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private static final String PIPELINE_NAME = "pipeline";
    public static final Cloner CLONER = new Cloner();

    @Before
    public void setup() throws Exception {
        dbHelper.onSetUp();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();
    }

    @After
    public void teardown() throws Exception {
        configHelper.onTearDown();
        dbHelper.onTearDown();
    }

    @Test
    public void shouldConsider_firstRevision_forAFlyweight_asInDb_whilePickingFromMultipleDeclarations() {
        ScheduleTestUtil u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
        int i = 1;

        GitMaterial git1 = u.wf(new GitMaterial("git"), "folder1");
        u.checkinInOrder(git1, "g1");

        GitMaterial git2 = u.wf(new GitMaterial("git"), "folder2");

        ScheduleTestUtil.AddedPipeline p = u.saveConfigWith("P", u.m(git1), u.m(git2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        u.checkinInOrder(git1, u.d(i++), "g2");

        u.runAndPass(p, "g1", "g2");

        u.runAndPass(p, "g2", "g1");

        PipelineTimeline timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        timeline.updateTimelineOnInit();

        List<PipelineTimelineEntry> timelineEntries = new ArrayList<PipelineTimelineEntry>(timeline.getEntriesFor("P"));
        assertThat(timelineEntries.get(0).getPipelineLocator().getCounter(), is(1));
        assertThat(timelineEntries.get(0).naturalOrder(), is(1.0));
        List<PipelineTimelineEntry.Revision> flyweightsRevs = new ArrayList<List<PipelineTimelineEntry.Revision>>(timelineEntries.get(0).revisions().values()).get(0);
        assertThat(flyweightsRevs.get(0).revision, is("g1"));
        assertThat(flyweightsRevs.get(1).revision, is("g2"));
        assertThat(timelineEntries.get(1).getPipelineLocator().getCounter(), is(2));
        assertThat(timelineEntries.get(1).naturalOrder(), is(2.0));
        flyweightsRevs = new ArrayList<List<PipelineTimelineEntry.Revision>>(timelineEntries.get(1).revisions().values()).get(0);
        assertThat(flyweightsRevs.get(0).revision, is("g2"));
        assertThat(flyweightsRevs.get(1).revision, is("g1"));

        MaterialConfigs materials = CLONER.deepClone(p.config.materialConfigs());
        Collections.reverse(materials);
        configHelper.setMaterialConfigForPipeline("P", materials.toArray(new MaterialConfig[0]));

        goConfigDao.load();

        timeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        timeline.updateTimelineOnInit();

        timelineEntries = new ArrayList<PipelineTimelineEntry>(timeline.getEntriesFor("P"));
        assertThat(timelineEntries.get(0).getPipelineLocator().getCounter(), is(1));
        assertThat(timelineEntries.get(0).naturalOrder(), is(1.0));
        flyweightsRevs = new ArrayList<List<PipelineTimelineEntry.Revision>>(timelineEntries.get(0).revisions().values()).get(0);
        assertThat(flyweightsRevs.get(0).revision, is("g1"));
        assertThat(flyweightsRevs.get(1).revision, is("g2"));
        assertThat(timelineEntries.get(1).getPipelineLocator().getCounter(), is(2));
        assertThat(timelineEntries.get(1).naturalOrder(), is(2.0));
        flyweightsRevs = new ArrayList<List<PipelineTimelineEntry.Revision>>(timelineEntries.get(1).revisions().values()).get(0);
        assertThat(flyweightsRevs.get(0).revision, is("g2"));
        assertThat(flyweightsRevs.get(1).revision, is("g1"));
    }

    @Test
    public void shouldReturnEarliestPMRFor1Material() throws Exception {
        HgMaterial hgmaterial = MaterialsMother.hgMaterial("first");

        PipelineConfig pipelineConfig = createPipelineConfig(PIPELINE_NAME, "stage", "job");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(hgmaterial.config()));
        DateTime date = new DateTime(1984, 12, 23, 0, 0, 0, 0);
        long firstId = createPipeline(hgmaterial, pipelineConfig, 1,
                oneModifiedFile("3", date.plusDays(2).toDate()),
                oneModifiedFile("2", date.plusDays(2).toDate()),
                oneModifiedFile("1", date.plusDays(3).toDate()));


        long secondId = createPipeline(hgmaterial, pipelineConfig, 2,
                oneModifiedFile("5", date.plusDays(1).toDate()),
                oneModifiedFile("4", date.toDate()));


        PipelineTimeline pipelineTimeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);

        List<PipelineTimelineEntry> entries = pipelineRepository.updatePipelineTimeline(pipelineTimeline);

        assertThat(pipelineTimeline.getEntriesFor(PIPELINE_NAME).size(), is(2));
        assertThat(entries.size(), is(2));
        assertThat(entries, hasItem(expected(firstId,
                Collections.singletonMap(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(2).toDate(), "123", hgmaterial.getFingerprint(), 10))), 1)));
        assertThat(entries, hasItem(expected(secondId,
                Collections.singletonMap(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(1).toDate(), "12", hgmaterial.getFingerprint(), 8))), 2)));

        assertThat(pipelineTimeline.getEntriesFor(PIPELINE_NAME), hasItem(expected(firstId,
                Collections.singletonMap(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(2).toDate(), "123", hgmaterial.getFingerprint(), 10))), 1)));
        assertThat(pipelineTimeline.getEntriesFor(PIPELINE_NAME), hasItem(expected(secondId,
                Collections.singletonMap(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(1).toDate(), "12", hgmaterial.getFingerprint(), 8))), 2)));
        assertThat(pipelineTimeline.maximumId(), is(secondId));

        long thirdId = createPipeline(hgmaterial, pipelineConfig, 3, oneModifiedFile("30", date.plusDays(10).toDate()));

        pipelineRepository.updatePipelineTimeline(pipelineTimeline);

        assertThat(pipelineTimeline.getEntriesFor(PIPELINE_NAME).size(), is(3));
        assertThat(pipelineTimeline.getEntriesFor(PIPELINE_NAME), hasItem(expected(thirdId,
                Collections.singletonMap(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(10).toDate(), "1234", hgmaterial.getFingerprint(), 12))), 3)));
        assertThat(pipelineTimeline.maximumId(), is(thirdId));

        assertThat(pipelineSqlMapDao.pipelineByIdWithMods(firstId).getNaturalOrder(), is(1.0));
        assertThat(pipelineSqlMapDao.pipelineByIdWithMods(secondId).getNaturalOrder(), is(0.5));
        assertThat(pipelineSqlMapDao.pipelineByIdWithMods(thirdId).getNaturalOrder(), is(2.0));

        PipelineTimeline pipelineTimeline2 = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        pipelineRepository.updatePipelineTimeline(pipelineTimeline2);
    }

    @Test
    public void shouldAddExistingPipelinesToTimelineForNewTimeline() {
        HgMaterial hgmaterial = MaterialsMother.hgMaterial(UUID.randomUUID().toString());

        PipelineConfig pipelineConfig = createPipelineConfig(PIPELINE_NAME, "stage", "job");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(hgmaterial.config()));
        DateTime date = new DateTime(1984, 12, 23, 0, 0, 0, 0);
        long firstId = createPipeline(hgmaterial, pipelineConfig, 1,
                oneModifiedFile("3", date.plusDays(2).toDate()),
                oneModifiedFile("2", date.plusDays(2).toDate()),
                oneModifiedFile("1", date.plusDays(3).toDate()));


        long secondId = createPipeline(hgmaterial, pipelineConfig, 2,
                oneModifiedFile("5", date.plusDays(1).toDate()),
                oneModifiedFile("4", date.toDate()));


        PipelineTimeline mods = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        mods.update();

        assertThat(pipelineSqlMapDao.pipelineByIdWithMods(firstId).getNaturalOrder(), is(1.0));
        assertThat(pipelineSqlMapDao.pipelineByIdWithMods(secondId).getNaturalOrder(), is(0.5));

        PipelineTimeline modsAfterReboot = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        modsAfterReboot.update();
    }

    @Test
    public void shouldReturnEarliestPMRForMultipleMaterial() throws Exception {
        final HgMaterial hgmaterial = MaterialsMother.hgMaterial("first");
        final SvnMaterial svnMaterial = MaterialsMother.svnMaterial();

        PipelineConfig pipelineConfig = createPipelineConfig(PIPELINE_NAME, "stage", "job");
        pipelineConfig.setMaterialConfigs(new MaterialConfigs(hgmaterial.config(), svnMaterial.config()));
        final DateTime date = new DateTime(1984, 12, 23, 0, 0, 0, 0);
        long first = save(pipelineConfig, 1, 1.0,
                new MaterialRevision(hgmaterial,
                        oneModifiedFile("13", date.plusDays(2).toDate()),
                        oneModifiedFile("12", date.plusDays(2).toDate()),
                        oneModifiedFile("11", date.plusDays(3).toDate())),
                new MaterialRevision(svnMaterial,
                        oneModifiedFile("23", date.plusDays(6).toDate()),
                        oneModifiedFile("22", date.plusDays(2).toDate()),
                        oneModifiedFile("21", date.plusDays(2).toDate()))
        );

        long second = save(pipelineConfig, 2, 0.0,
                new MaterialRevision(hgmaterial,
                        oneModifiedFile("15", date.plusDays(3).toDate()),
                        oneModifiedFile("14", date.plusDays(2).toDate())),
                new MaterialRevision(svnMaterial,
                        oneModifiedFile("25", date.plusDays(5).toDate())));

        PipelineTimeline pipelineTimeline = new PipelineTimeline(pipelineRepository, transactionTemplate, transactionSynchronizationManager);
        pipelineRepository.updatePipelineTimeline(pipelineTimeline);

        Collection<PipelineTimelineEntry> modifications = pipelineTimeline.getEntriesFor(PIPELINE_NAME);
        assertThat(modifications.size(), is(2));

        assertThat(modifications, hasItem(expected(first, new HashMap<String, List<PipelineTimelineEntry.Revision>>() {{
            put(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(2).toDate(), "123", hgmaterial.getFingerprint(), 8)));
            put(svnMaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(6).toDate(), "456", svnMaterial.getFingerprint(), 12)));
        }}, 1)));
        assertThat(modifications, hasItem(expected(second, new HashMap<String, List<PipelineTimelineEntry.Revision>>() {{
            put(hgmaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(3).toDate(), "234", hgmaterial.getFingerprint(), 9)));
            put(svnMaterial.getFingerprint(), a(new PipelineTimelineEntry.Revision(date.plusDays(5).toDate(), "345", svnMaterial.getFingerprint(), 10)));
        }}, 2)));
    }

    private PipelineTimelineEntry expected(long first, Map<String, List<PipelineTimelineEntry.Revision>> map, int counter) {
        return new PipelineTimelineEntry(PIPELINE_NAME, first, counter, map);
    }

    private long createPipeline(HgMaterial hgmaterial, PipelineConfig pipelineConfig, int counter, Modification... modifications) {
        return save(pipelineConfig, counter, new MaterialRevision(hgmaterial, modifications));
    }

    private long save(PipelineConfig pipelineConfig, int counter, MaterialRevision... materialRevisions) {
        return save(pipelineConfig, counter, 0.0, materialRevisions);
    }

    private long save(final PipelineConfig pipelineConfig, final int counter, final double naturalOrder, final MaterialRevision... materialRevisions) {
        return (Long) transactionTemplate.execute(new TransactionCallback() {
            public Object doInTransaction(TransactionStatus status) {
                MaterialRevisions revisions = new MaterialRevisions(materialRevisions);
                materialRepository.save(revisions);

                Pipeline instance = instanceFactory.createPipelineInstance(pipelineConfig, BuildCause.createWithModifications(revisions, "me"), new DefaultSchedulingContext(), "md5-test", new TimeProvider());
                instance.setCounter(counter);
                instance.setNaturalOrder(naturalOrder);
                return pipelineSqlMapDao.save(instance).getId();
            }
        });
    }

    @Test
    public void shouldReturnNullForInvalidIds() {
        assertThat(pipelineRepository.findPipelineSelectionsById(null), is(nullValue()));
        assertThat(pipelineRepository.findPipelineSelectionsById(""), is(nullValue()));
        assertThat(pipelineRepository.findPipelineSelectionsById("123"), is(nullValue()));
        try {
            pipelineRepository.findPipelineSelectionsById("foo");
            fail("should throw error");
        } catch (NumberFormatException e) {

        }
    }

    @Test
    public void shouldSaveSelectedPipelinesWithoutUserId() {
        Date date = new Date();
        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");

        long id = pipelineRepository.saveSelectedPipelines(new PipelineSelections(unSelected, date, null, true));
        PipelineSelections found = pipelineRepository.findPipelineSelectionsById(id);

        assertHasPipelines(found, new String[]{"pipeline3", "pipeline4"});
        assertHasPipelines(found, new String[]{"pipeline1", "pipeline2"}, false);
        assertThat(found.userId(), is(nullValue()));
        assertEquals(date, found.lastUpdated());
    }

    @Test
    public void shouldSaveSelectedPipelinesWithUserId() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        long id = pipelineRepository.saveSelectedPipelines(new PipelineSelections(unSelected, new Date(), user.getId(), true));
        assertThat(pipelineRepository.findPipelineSelectionsById(id).userId(), is(user.getId()));
    }

    @Test
    public void shouldSaveSelectedPipelinesWithBlacklistPreferenceFalse() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        long id = pipelineRepository.saveSelectedPipelines(new PipelineSelections(unSelected, new Date(), user.getId(), false));
        assertThat(pipelineRepository.findPipelineSelectionsById(id).isBlacklist(), is(false));
    }

    @Test
    public void shouldSaveSelectedPipelinesWithBlacklistPreferenceTrue() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        long id = pipelineRepository.saveSelectedPipelines(new PipelineSelections(unSelected, new Date(), user.getId(), true));
        assertThat(pipelineRepository.findPipelineSelectionsById(id).isBlacklist(), is(true));
    }

    @Test
    public void shouldFindSelectedPipelinesByUserId() {
        User user = createUser();

        List<String> unSelected = Arrays.asList("pipeline1", "pipeline2");
        long id = pipelineRepository.saveSelectedPipelines(new PipelineSelections(unSelected, new Date(), user.getId(), true));
        assertThat(pipelineRepository.findPipelineSelectionsByUserId(user.getId()).getId(), is(id));
    }

    @Test
    public void shouldReturnNullAsPipelineSelectionsIfUserIdIsNull() {
        assertThat(pipelineRepository.findPipelineSelectionsByUserId(null), is(nullValue()));
    }

    @Test
    public void shouldReturnNullAsPipelineSelectionsIfSelectionsExistForUser() {
        assertThat(pipelineRepository.findPipelineSelectionsByUserId(10L), is(nullValue()));
    }

    private User createUser() {
        userSqlMapDao.saveOrUpdate(new User("loser"));
        return userSqlMapDao.findUser("loser");
    }

    private void assertHasPipelines(PipelineSelections pipelineSelections, String[] pipelines) {
        assertHasPipelines(pipelineSelections, pipelines, true);
    }

    private void assertHasPipelines(PipelineSelections pipelineSelections, String[] pipelines, boolean has) {
        for (String pipeline : pipelines) {
            assertThat(pipelineSelections.includesPipeline(pipelineConfig(pipeline)),is(has));
        }
    }
}
