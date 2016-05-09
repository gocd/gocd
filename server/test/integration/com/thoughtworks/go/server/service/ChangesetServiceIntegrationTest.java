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

package com.thoughtworks.go.server.service;

import java.util.*;

import com.thoughtworks.go.config.*;
import com.thoughtworks.go.config.materials.MaterialConfigs;
import com.thoughtworks.go.config.materials.ScmMaterial;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialInstance;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.PersistentObject;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.PipelineMaterialRevision;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.Modification;
import com.thoughtworks.go.domain.materials.Modifications;
import com.thoughtworks.go.domain.materials.svn.SvnMaterialInstance;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.PipelineConfigMother;
import com.thoughtworks.go.i18n.Localizer;
import com.thoughtworks.go.security.GoCipher;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.Username;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.result.HttpLocalizedOperationResult;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.server.web.PipelineRevisionRange;
import com.thoughtworks.go.util.GoConfigFileHelper;
import org.joda.time.DateTime;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;

import static com.thoughtworks.go.helper.ModificationsMother.checkinWithComment;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})

public class ChangesetServiceIntegrationTest {
    @Autowired
    MaterialRepository materialRepository;
    @Autowired
    PipelineService pipelineService;
    @Autowired
    ChangesetService changesetService;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private Localizer localizer;
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private TransactionTemplate transactionTemplate;

    private PipelineConfig pipelineConfigWithTwoMaterials;
    private PipelineConfig pipelineConfig;
    private PipelineConfig pipelineConfigWithSvn;
    private ScmMaterial git;
    private ScmMaterial hg;
    private ScmMaterial svn;
    private GoConfigFileHelper configHelper;
    private static int counter = 0;

    @Before
    public void setUp() throws Exception {
        dbHelper.onSetUp();
        configHelper = new GoConfigFileHelper(goConfigDao);
        configHelper.onSetUp();
        git = MaterialsMother.gitMaterial("http://google.com", null, "master");
        git.setFolder("git");
        hg = MaterialsMother.hgMaterial();
        hg.setFolder("hg");
        svn = MaterialsMother.svnMaterial("http://google.com/svn");
        svn.setFolder("svn");
        pipelineConfig = configHelper.addPipeline("foo-bar", "stage", new MaterialConfigs(hg.config()), "build");
        pipelineConfigWithTwoMaterials = configHelper.addPipeline("foo", "stage", new MaterialConfigs(git.config(), hg.config()), "build");
        pipelineConfigWithSvn = configHelper.addPipeline("bar", "stage", new MaterialConfigs(svn.config()), "build");
    }

    @After
    public void tearDown() throws Exception {
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }


    @Test
    public void shouldUnderstandModificationsBetween_MultipleSetsOfInstances_OfDifferentPipelines() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Modification hgCommit1 = checkinWithComment("abcd", "#4518 - foo", checkinTime);
        Modification gitCommit1 = checkinWithComment("1234", "#3750 - agent index", checkinTime);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit1),
                dbHelper.addRevisionsWithModifications(git, gitCommit1));

        Modification hgCommit2 = checkinWithComment("bcde", "#4520 - foo", checkinTime);
        Modification gitCommit2 = checkinWithComment("2355", "#3750 - agent index", checkinTime);
        dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit2), dbHelper.addRevisionsWithModifications(git, gitCommit2));


        Modification hgCommit3 = checkinWithComment("cdef", "#4521 - get gadget working", checkinTime);
        Modification gitCommit3 = checkinWithComment("2345", "#4200 - whatever", checkinTime);
        Pipeline pipelineThree = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit3),
                dbHelper.addRevisionsWithModifications(git, gitCommit3));

        Modification svnCommit1 = checkinWithComment("9876", "svn ci", checkinTime);
        Pipeline pipelineSvnOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithSvn, dbHelper.addRevisionsWithModifications(svn, svnCommit1));

        Modification svnCommit2 = checkinWithComment("5432", "another svn ci", checkinTime);
        Pipeline pipelineSvnTwo = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithSvn, dbHelper.addRevisionsWithModifications(svn, svnCommit2));

        Modification svnCommit3 = checkinWithComment("666", "svn ci 3", checkinTime);
        Modification svnCommit4 = checkinWithComment("121212", "svn ci 4", checkinTime);
        Pipeline pipelineSvnThree = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithSvn, dbHelper.addRevisionsWithModifications(svn, svnCommit4, svnCommit3));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> revisions = changesetService.revisionsBetween(
                Arrays.asList(pipelineRevRange(pipelineOne, pipelineThree), pipelineRevRange(pipelineSvnOne, pipelineSvnThree)),
                loser, result);

        List<MaterialRevision> expectedRevisions = Arrays.asList(
                new MaterialRevision(hg, hgCommit3, hgCommit2),
                new MaterialRevision(git, gitCommit3, gitCommit2),
                new MaterialRevision(svn, svnCommit4, svnCommit3, svnCommit2));


        assertMaterialRevisions(expectedRevisions, revisions);
        assertThat(result.isSuccessful(), is(true));
    }

    private PipelineRevisionRange pipelineRevRange(Pipeline from, Pipeline to) {
        return new PipelineRevisionRange(from.getName(), rev(from), rev(to));
    }

    private String rev(Pipeline pipeline) {
        return String.format("%s/%s/%s/%s", pipeline.getName(), pipeline.getCounter(), pipeline.getFirstStage().getName(), pipeline.getFirstStage().getCounter());
    }

    @Test
    public void shouldUnderstandModificationsBetweenTwoPipelineInstances() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Modification hgCommit1 = checkinWithComment("abcd", "#4518 - foo", checkinTime);
        Modification gitCommit1 = checkinWithComment("1234", "#3750 - agent index", checkinTime);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit1),
                dbHelper.addRevisionsWithModifications(git, gitCommit1));

        Modification hgCommit2 = checkinWithComment("bcde", "#4520 - foo", checkinTime);
        Modification gitCommit2 = checkinWithComment("2355", "#3750 - agent index", checkinTime);
        dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit2), dbHelper.addRevisionsWithModifications(git, gitCommit2));


        Modification hgCommit3 = checkinWithComment("cdef", "#4521 - get gadget working", checkinTime);
        Modification gitCommit3 = checkinWithComment("2345", "#4200 - whatever", checkinTime);
        Pipeline pipelineThree = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit3),
                dbHelper.addRevisionsWithModifications(git, gitCommit3));

        List<MaterialRevision> expectedRevisions = Arrays.asList(
                new MaterialRevision(hg, hgCommit3, hgCommit2),
                new MaterialRevision(git, gitCommit3, gitCommit2));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> revisions = changesetService.revisionsBetween("foo", pipelineOne.getCounter(), pipelineThree.getCounter(), loser, result, true, false);
        assertMaterialRevisions(expectedRevisions, revisions);
        assertThat(result.isSuccessful(), is(true));

        List<String> cardsInBetween = changesetService.getCardNumbersBetween("foo", pipelineOne.getCounter(), pipelineThree.getCounter(), loser, result, false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(4));
        assertThat(cardsInBetween, is(Arrays.asList("4200", "4521", "3750", "4520")));
    }

    @Test
    public void shouldFlipCountersAndFindTheDiffInModificationsEvenWhenFromCounterIsBiggerThanTheToCounter() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Modification hgCommit1 = checkinWithComment("abcd", "#4518 - foo", checkinTime);
        Modification gitCommit1 = checkinWithComment("1234", "#3750 - agent index", checkinTime);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit1),
                dbHelper.addRevisionsWithModifications(git, gitCommit1));

        Modification hgCommit2 = checkinWithComment("bcde", "#4520 - foo", checkinTime);
        Modification gitCommit2 = checkinWithComment("2355", "#3750 - agent index", checkinTime);
        dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit2), dbHelper.addRevisionsWithModifications(git, gitCommit2));


        Modification hgCommit3 = checkinWithComment("cdef", "#4521 - get gadget working", checkinTime);
        Modification gitCommit3 = checkinWithComment("2345", "#4200 - whatever", checkinTime);
        Pipeline pipelineThree = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit3),
                dbHelper.addRevisionsWithModifications(git, gitCommit3));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> cardsInBetween = changesetService.getCardNumbersBetween("foo", pipelineThree.getCounter(), pipelineOne.getCounter(), loser, result, false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(4));
        assertThat(cardsInBetween, is(Arrays.asList("4200", "4521", "3750", "4520")));
    }

    @Test
    public void shouldFilterOutCardNumbersWhenGivingTheCardNumbersWhenMingleConfigIsDifferentFromParent() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();


        PipelineConfig upstream = configHelper.addPipeline("upstream", "up-stage", git.config(), new MingleConfig("https://upstream-mingle", "go"), "job");

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "up-stage");
        PipelineConfig downstream = configHelper.addPipeline("downstream", "down-stage", dependencyMaterial.config(), new MingleConfig("https://downstream-mingle", "go"), "job");

        //Schedule upstream
        Modification gitCommit1 = checkinWithComment("1234", "#3750, #3123 - agent index", checkinTime);
        MaterialRevision materialRevision = dbHelper.addRevisionsWithModifications(git, gitCommit1);
        Pipeline upInstance1 = dbHelper.checkinRevisionsToBuild(build, upstream, materialRevision);

        Modification gitCommit2 = checkinWithComment("1239", "#4150, #786 - agent index", checkinTime);
        materialRevision = dbHelper.addRevisionsWithModifications(git, gitCommit2);
        Pipeline upInstance2 = dbHelper.checkinRevisionsToBuild(build, upstream, materialRevision);

        //Schedule downstream
        ArrayList<MaterialRevision> materialRevisions = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(materialRevisions, dependencyMaterial, upInstance1);
        Pipeline downInstance1 = dbHelper.checkinRevisionsToBuild(build, downstream, materialRevisions);

        materialRevisions.clear();
        dbHelper.addDependencyRevisionModification(materialRevisions, dependencyMaterial, upInstance2);
        Pipeline downInstance2 = dbHelper.checkinRevisionsToBuild(build, downstream, materialRevisions);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> cardsInBetween = changesetService.getCardNumbersBetween("downstream", downInstance1.getCounter(), downInstance2.getCounter(), loser, result, false);

        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(0));
    }

    @Test
    public void shouldNotDuplicateModificationsWhileComputingRevisionsBetweenTwoPipelineInstances() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Modification hgCommit1 = checkinWithComment("abcd", "#4518 - foo", checkinTime);
        MaterialRevision materialRevision = dbHelper.addRevisionsWithModifications(hg, hgCommit1);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, materialRevision);

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, materialRevision);
        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, materialRevision);

        Pipeline pipelineFour = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, materialRevision);


        List<MaterialRevision> expectedRevisions = Arrays.asList(new MaterialRevision(hg, hgCommit1));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> revisions = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfig.name()), pipelineOne.getCounter(), pipelineFour.getCounter(), loser, result, true,
                false);
        assertMaterialRevisions(expectedRevisions, revisions);
        assertThat(result.isSuccessful(), is(true));

        List<String> cardsInBetween = changesetService.getCardNumbersBetween(CaseInsensitiveString.str(pipelineConfig.name()), pipelineOne.getCounter(), pipelineFour.getCounter(), loser, result,
                false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(1));
        assertThat(cardsInBetween, is(Arrays.asList("4518")));
    }

    @Test
    public void shouldIgnoreModificationsWithNoCardNumbersInComments() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, modificationWithNoCardNumberInComment(checkinTime)));

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, modificationWithNoCardNumberInComment(checkinTime)));
        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, modificationWithNoCardNumberInComment(checkinTime)));

        Pipeline pipelineFour = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, modificationWithNoCardNumberInComment(checkinTime)));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> cardsInBetween = changesetService.getCardNumbersBetween(CaseInsensitiveString.str(pipelineConfig.name()), pipelineOne.getCounter(), pipelineFour.getCounter(), loser, result,
                false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(0));
    }

    private Modification modificationWithNoCardNumberInComment(Date checkinTime) {
        return checkinWithComment("abcd" + UUID.randomUUID(), "Commit made against no card number - foo", checkinTime);
    }

    @Test
    public void shouldShowCardsWhenMaterialsChange() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);

        Pipeline pipelineFrom = dbHelper.checkinRevisionsToBuild(build, pipelineConfig,
                dbHelper.addRevisionsWithModifications(hg, modificationWithNoCardNumberInComment(new Date())));
        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("8923", "hg commit for card #2750 and #3400", new Date())));

        CruiseConfig config = configHelper.load();
        pipelineConfig = config.pipelineConfigByName(pipelineConfig.name());
        pipelineConfig.materialConfigs().clear();
        pipelineConfig.addMaterialConfig(git.config());
        configHelper.writeConfigFile(config);

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(git, checkinWithComment("1234abcd", "commit affecting #10", new Date())));
        Pipeline pipelineTo = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(git, checkinWithComment("9876dcba", "card #199", new Date())));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<String> cardsInBetween = changesetService.getCardNumbersBetween(CaseInsensitiveString.str(pipelineConfig.name()), pipelineFrom.getCounter(), pipelineTo.getCounter(), loser, result, false);
        assertThat(cardsInBetween, is(Arrays.asList("199", "10", "2750", "3400")));
    }

    @Test
    public void shouldGetModificationsFrom0To1() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Modification hgCommit1 = checkinWithComment("abcd", "#4518 - foo", checkinTime);
        Modification gitCommit1 = checkinWithComment("1234", "#3750 - agent index", checkinTime);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit1),
                dbHelper.addRevisionsWithModifications(git, gitCommit1));

        List<MaterialRevision> expectedRevisions = Arrays.asList(new MaterialRevision(hg, hgCommit1), new MaterialRevision(git, gitCommit1));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> revisions = changesetService.revisionsBetween("foo", 0, pipelineOne.getCounter(), loser, result, true, false);
        assertMaterialRevisions(expectedRevisions, revisions);
        assertThat(result.isSuccessful(), is(true));

        List<String> cardsInBetween = changesetService.getCardNumbersBetween("foo", 0, pipelineOne.getCounter(), loser, result, false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(2));
        assertThat(cardsInBetween, is(Arrays.asList("3750", "4518")));
    }

    @Test
    public void shouldGetModificationsFrom1To1() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);
        Date checkinTime = new Date();

        Modification hgCommit1 = checkinWithComment("abcd", "#4518 - foo", checkinTime);
        Modification gitCommit1 = checkinWithComment("1234", "#3750 - agent index", checkinTime);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(build, pipelineConfigWithTwoMaterials, dbHelper.addRevisionsWithModifications(hg, hgCommit1),
                dbHelper.addRevisionsWithModifications(git, gitCommit1));

        List<MaterialRevision> expectedRevisions = Arrays.asList(new MaterialRevision(hg, hgCommit1), new MaterialRevision(git, gitCommit1));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> revisions = changesetService.revisionsBetween("foo", pipelineOne.getCounter(), pipelineOne.getCounter(), loser, result, true, false);
        assertMaterialRevisions(expectedRevisions, revisions);
        assertThat(result.isSuccessful(), is(true));

        List<String> cardsInBetween = changesetService.getCardNumbersBetween("foo", pipelineOne.getCounter(), pipelineOne.getCounter(), loser, result, false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(cardsInBetween.size(), is(2));
        assertThat(cardsInBetween, is(Arrays.asList("3750", "4518")));
    }

    @Test
    public void shouldFailWhenUserDoesNotHaveAccess() {
        SecurityConfig securityConfig = new SecurityConfig(new LdapConfig(new GoCipher()), new PasswordFileConfig("/tmp/foo.passwd"), true);
        securityConfig.adminsConfig().add(new AdminUser(new CaseInsensitiveString("admin")));
        configHelper.addSecurity(securityConfig);
        CruiseConfig config = this.configHelper.getCachedGoConfig().loadForEditing();
        config.pipelines(BasicPipelineConfigs.DEFAULT_GROUP).setAuthorization(new Authorization(new ViewConfig(new AdminUser(new CaseInsensitiveString("admin")))));
        configHelper.writeConfigFile(config);
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        changesetService.revisionsBetween("foo", 1, 3, new Username(new CaseInsensitiveString("some_loser")), result, true, false);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("You do not have view permissions for pipeline 'foo'."));
        assertThat(result.httpCode(), is(401));
    }

    @Test
    public void shouldReturn404WhenPipelineIsNotFound() {
        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        changesetService.revisionsBetween("Pipeline_Not_Found", 1, 3, new Username(new CaseInsensitiveString("some_loser")), result, true, false);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Pipeline 'Pipeline_Not_Found' not found."));
        assertThat(result.httpCode(), is(404));
    }

    @Test
    public void shouldFailUnlessPipelineCountersAreNatural() {
        validateFailsForNonNaturalCounter(-1, 10);
        validateFailsForNonNaturalCounter(0, 0);
        validateFailsForNonNaturalCounter(2, -2);
        validateFailsForNonNaturalCounter(2, 0);
        validateFailsForNonNaturalCounter(-12, -10);
    }

    @Test
    public void shouldReturnResults_WhenPipelineCountersIsBisect_ButUserWantsToSeeDiff() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);

        DateTime now = new DateTime();
        Pipeline firstPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("1", "#3518 - hg - foo", now.toDate())));

        Modification bisectModification = checkinWithComment("3", "#4750 - Rev 3", now.plusDays(3).toDate());
        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("2", "#3750 - Rev 2", now.plusDays(2).toDate()), bisectModification,
                checkinWithComment("4", "#4750 - Rev 4", now.plus(4).toDate())));

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("5", "#5750 - Rev 5", now.plusDays(5).toDate())));

        Pipeline bisectPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, new MaterialRevision(hg, bisectModification));

        Pipeline nextPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("6", "#4150 - Rev 6", now.plusDays(7).toDate())));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        //when to counter is a bisect
        List<MaterialRevision> revisionList = changesetService.
                revisionsBetween("foo-bar", firstPipeline.getCounter(), bisectPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, true, true);
        assertThat(stringRevisions(revisionList), is(Arrays.asList("5", "2", "3", "4")));
        List<String> cardList = changesetService.getCardNumbersBetween("foo-bar", firstPipeline.getCounter(), bisectPipeline.getCounter(),
                new Username(new CaseInsensitiveString("loser")), result, true);
        assertThat(cardList, is(Arrays.asList("5750", "3750", "4750")));

        //When from counter is a bisect
        revisionList = changesetService.revisionsBetween("foo-bar", bisectPipeline.getCounter(), nextPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, true, true);
        assertThat(stringRevisions(revisionList), is(Arrays.asList("6", "5", "2")));
        cardList = changesetService.getCardNumbersBetween("foo-bar", bisectPipeline.getCounter(), nextPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, true);
        assertThat(cardList, is(Arrays.asList("4150", "5750", "3750")));
    }

    @Test
    public void shouldReturnAnEmptyListWhenEitherOfThePipelineCounterIsABisect() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);

        DateTime now = new DateTime();
        Pipeline firstPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("1", "#3518 - hg - foo", now.toDate())));

        Modification bisectModification = checkinWithComment("3", "#4750 - Rev 3", now.plusDays(3).toDate());
        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("2", "#3750 - Rev 2", now.plusDays(2).toDate()), bisectModification,
                checkinWithComment("4", "#4750 - Rev 4", now.plus(4).toDate())));

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("5", "#5750 - Rev 5", now.plusDays(5).toDate())));

        Pipeline bisectPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, new MaterialRevision(hg, bisectModification));

        Pipeline nextPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("6", "#4150 - Rev 6", now.plusDays(7).toDate())));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        //when to counter is a bisect
        List<MaterialRevision> revisionList = changesetService.revisionsBetween("foo-bar", firstPipeline.getCounter(), bisectPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, true,
                false);
        assertThat(revisionList.isEmpty(), is(true));
        List<String> cardList = changesetService.getCardNumbersBetween("foo-bar", firstPipeline.getCounter(), bisectPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result,
                false);
        assertThat(cardList.isEmpty(), is(true));

        //When from counter is a bisect
        revisionList = changesetService.revisionsBetween("foo-bar", bisectPipeline.getCounter(), nextPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, true, false);
        assertThat(revisionList.isEmpty(), is(true));
        cardList = changesetService.getCardNumbersBetween("foo-bar", bisectPipeline.getCounter(), nextPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, false);
        assertThat(cardList.isEmpty(), is(true));
    }

    @Test
    public void shouldSkipBisectPipelineCountersWhenThereIsABisectBetweenToAndFromCounters() {
        Username loser = new Username(new CaseInsensitiveString("loser"));
        ManualBuild build = new ManualBuild(loser);

        DateTime now = new DateTime();
        Pipeline firstPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg,
                checkinWithComment("1", "#3518 - hg - foo", now.toDate())));

        Modification bisectModification = checkinWithComment("3", "#4750 - Rev 3", now.plusDays(3).toDate());
        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg,
                checkinWithComment("2", "#3750 - Rev 2", now.plusDays(2).toDate()),
                bisectModification,
                checkinWithComment("4", "#4750 - Rev 4", now.plusDays(4).toDate())));

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg, checkinWithComment("5", "#5750 - Rev 5", now.plusDays(5).toDate())));

        dbHelper.checkinRevisionsToBuild(build, pipelineConfig, new MaterialRevision(hg, bisectModification));

        Pipeline lastPipeline = dbHelper.checkinRevisionsToBuild(build, pipelineConfig, dbHelper.addRevisionsWithModifications(hg,
                checkinWithComment("6", "#6760 - Rev 6", now.plusDays(6).toDate())));

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> revisionsBetween = changesetService.revisionsBetween("foo-bar", firstPipeline.getCounter(), lastPipeline.getCounter(), new Username(new CaseInsensitiveString("loser")), result, true,
                false);
        assertThat(result.isSuccessful(), is(true));
        assertThat(revisionsBetween.size(), is(1));
        Modifications actualMods = revisionsBetween.get(0).getModifications();
        assertThat(actualMods.size(), is(5));
    }

    @Test
    public void shouldPopulateActualFromRevisionId() {
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        PipelineConfig downstreamConfig = configHelper.addPipeline("downstream", "stage", dependencyMaterial.config(), "build");

        Username username = new Username(new CaseInsensitiveString("user1"));

        //By default the pmr's actualFromRevisionId should be the fromRevisionId
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        Pipeline upstream1 = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);
        assertPipelineMaterialRevisions(upstream1);

        //First downstream pipeline's actualFromRevisionId should also be it's fromRevisionId
        List<MaterialRevision> revisionsForDownstream1 = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(revisionsForDownstream1, dependencyMaterial, upstream1);
        Pipeline downstream1 = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), downstreamConfig, revisionsForDownstream1);
        assertPipelineMaterialRevisions(downstream1);

        // When downstream is triggered with a range of upstream modifications it's actualFromRevisionId should be 1 more than the last one that was built
        List<MaterialRevision> revisionsForUpstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream2, git);
        Pipeline upstream2 = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream2);
        assertPipelineMaterialRevisions(upstream2);

        List<MaterialRevision> revisionsForUpstream3 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream3, git);
        Pipeline upstream3 = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream3);
        assertPipelineMaterialRevisions(upstream3);

        List<MaterialRevision> revisionsForUpstream4 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream4, git);
        Pipeline upstream4 = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream4);
        assertPipelineMaterialRevisions(upstream4);

        List<MaterialRevision> depMaterialRevision = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(depMaterialRevision, dependencyMaterial, upstream2);
        Modification expectedMod = depMaterialRevision.get(0).getLatestModification();
        MaterialInstance dep = materialRepository.findOrCreateFrom(dependencyMaterial);
        saveRev(expectedMod, dep);

        dbHelper.addDependencyRevisionModification(depMaterialRevision, dependencyMaterial, upstream3);
        saveRev(depMaterialRevision.get(0).getLatestModification(), dep);

        List<MaterialRevision> revisionsForDownstream2 = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(revisionsForDownstream2, dependencyMaterial, upstream4);
        Pipeline downstream2 = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), downstreamConfig, revisionsForDownstream2);
        List<PipelineMaterialRevision> pmrs = materialRepository.findPipelineMaterialRevisions(downstream2.getId());

        assertThat(pmrs.size(), is(1));
        assertThat(pmrs.get(0).getActualFromRevisionId(), is(expectedMod.getId()));
    }

    private void saveRev(final Modification expectedMod, final MaterialInstance dep) {
        transactionTemplate.execute(new TransactionCallbackWithoutResult() {
            @Override
            protected void doInTransactionWithoutResult(TransactionStatus status) {
                materialRepository.saveModification(dep, expectedMod);
            }
        });
    }

    private void assertPipelineMaterialRevisions(Pipeline upstreamOne) {
        List<PipelineMaterialRevision> pmrs = materialRepository.findPipelineMaterialRevisions(upstreamOne.getId());
        assertThat(pmrs.size(), is(1));
        assertThat(pmrs.get(0).getActualFromRevisionId(), is(pmrs.get(0).getFromModification().getId()));
    }

    @Test
    public void shouldReturnTheHgMaterialForAGivenPipeline() {
        List<MaterialRevision> revisions = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisions, hg);

        Username username = new Username(new CaseInsensitiveString("user1"));
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfig, revisions);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfig.name()), pipelineOne.getCounter(), pipelineOne.getCounter(), username, result, true,
                false);

        assertMaterialRevisions(revisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnModsOf2MaterialsForAGivenPipeline() {
        List<MaterialRevision> revisions = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisions, hg);
        addRevisionWith2Mods(revisions, git);

        Username username = new Username(new CaseInsensitiveString("user1"));
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisions);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), pipelineOne.getCounter(), pipelineOne.getCounter(), username, result, true,
                false);

        assertMaterialRevisions(revisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnModsOf2MaterialsBetweenTheGivenPipelineCounters() {
        List<MaterialRevision> revisionsForPipeline1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForPipeline1, hg);
        addRevisionWith2Mods(revisionsForPipeline1, git);

        Username username = new Username(new CaseInsensitiveString("user1"));
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForPipeline1);

        List<MaterialRevision> revisionsForPipeline2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForPipeline2, hg);
        addRevisionWith2Mods(revisionsForPipeline2, git);

        Pipeline pipelineTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForPipeline2);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), pipelineOne.getCounter(), pipelineTwo.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForPipeline2);

        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnModsOfItsOwnAndAnUpstreamPipelineForTheGivenPipelineCounters() {
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        pipelineConfigWithTwoMaterials.addMaterialConfig(dependencyMaterial.config());
        pipelineConfigWithTwoMaterials.removeMaterialConfig(git.config());

        Username username = new Username(new CaseInsensitiveString("user1"));

        //Schedule upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForPipeline1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForPipeline1, hg);
        dbHelper.addDependencyRevisionModification(revisionsForPipeline1, dependencyMaterial, upstreamOne);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForPipeline1);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), pipelineOne.getCounter(), pipelineOne.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream1, revisionsForPipeline1);

        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnModsOfItsOwnAndAllUpstreamPipelinesForTheGivenCounter() {
        SvnMaterial svn = MaterialsMother.svnMaterial("http://svn");
        PipelineConfig grandFatherPipeline = configHelper.addPipeline("granpa", "stage", svn.config(), "job");

        DependencyMaterial parentDependencyMaterial = MaterialsMother.dependencyMaterial("granpa", "stage");
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");
        upstreamPipeline.addMaterialConfig(parentDependencyMaterial.config());

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        pipelineConfigWithTwoMaterials.addMaterialConfig(dependencyMaterial.config());
        pipelineConfigWithTwoMaterials.removeMaterialConfig(git.config());

        Username username = new Username(new CaseInsensitiveString("user1"));

        //Schedule grandfather
        List<MaterialRevision> revisionsForGrandfather1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForGrandfather1, svn);
        Pipeline grandFatherOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), grandFatherPipeline, revisionsForGrandfather1);

        //Schedule upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        dbHelper.addDependencyRevisionModification(revisionsForUpstream1, parentDependencyMaterial, grandFatherOne);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForPipeline1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForPipeline1, hg);
        dbHelper.addDependencyRevisionModification(revisionsForPipeline1, dependencyMaterial, upstreamOne);
        Pipeline pipelineOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForPipeline1);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), pipelineOne.getCounter(), pipelineOne.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForGrandfather1, revisionsForUpstream1, revisionsForPipeline1);

        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldFilterOutMaterialRevisionsThatTheUserIsNotAuthorizedToView() throws Exception {
        configHelper.turnOnSecurity();
        configHelper.addAdmins("Yogi");

        Username otherUser = new Username(new CaseInsensitiveString("otherUser"));
        Username user = new Username(new CaseInsensitiveString("user"));

        SvnMaterial svn = MaterialsMother.svnMaterial("http://svn");
        PipelineConfig grandFatherPipeline = configHelper.addPipelineWithGroup("unauthorizedGroup", "granpa", new MaterialConfigs(svn.config()), "stage", "job");
        configHelper.setViewPermissionForGroup("unauthorizedGroup", CaseInsensitiveString.str(otherUser.getUsername()));

        DependencyMaterial parentDependencyMaterial = MaterialsMother.dependencyMaterial("granpa", "stage");
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", new MaterialConfigs(git.config(), parentDependencyMaterial.config()), "job");

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        PipelineConfig downstream = configHelper.addPipeline("downstream", "stage", dependencyMaterial.config(), "job");

        //Schedule grandfather
        List<MaterialRevision> revisionsForGrandfather1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForGrandfather1, svn);
        Pipeline grandFatherOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), grandFatherPipeline, revisionsForGrandfather1);

        //Schedule upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        dbHelper.addDependencyRevisionModification(revisionsForUpstream1, parentDependencyMaterial, grandFatherOne);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(revisionsForDownstream, dependencyMaterial, upstreamOne);
        Pipeline pipelineDownstream = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), downstream, revisionsForDownstream);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(pipelineDownstream.getName(), pipelineDownstream.getCounter(), pipelineDownstream.getCounter(), user, result, true, false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream1, revisionsForDownstream);
        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldNotFilterOutMaterialRevisionsAtThePointInTheDependencyIfMingleConfigDoesNotMatchWithParent() throws Exception {
        Username user = new Username(new CaseInsensitiveString("user"));

        SvnMaterial svn = MaterialsMother.svnMaterial("http://svn");
        PipelineConfig grandFatherPipeline = configHelper.addPipelineWithGroup("unauthorizedGroup", "granpa", new MaterialConfigs(svn.config()), new MingleConfig("https://granpa-mingle", "go"), "stage", "job");
        configHelper.setViewPermissionForGroup("unauthorizedGroup", CaseInsensitiveString.str(user.getUsername()));

        DependencyMaterial parentDependencyMaterial = MaterialsMother.dependencyMaterial("granpa", "stage");
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", new MaterialConfigs(git.config(), parentDependencyMaterial.config()), "job");

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        PipelineConfig downstream = configHelper.addPipeline("downstream", "stage", dependencyMaterial.config(), new MingleConfig("https://downstream-mingle", "go"), "job");

        //Schedule grandfather
        List<MaterialRevision> revisionsForGrandfather1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForGrandfather1, svn);
        Pipeline grandFatherOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), grandFatherPipeline, revisionsForGrandfather1);

        //Schedule upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        dbHelper.addDependencyRevisionModification(revisionsForUpstream1, parentDependencyMaterial, grandFatherOne);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(revisionsForDownstream, dependencyMaterial, upstreamOne);
        Pipeline pipelineDownstream = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), downstream, revisionsForDownstream);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(pipelineDownstream.getName(), pipelineDownstream.getCounter(), pipelineDownstream.getCounter(), user, result, true, false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForGrandfather1, revisionsForUpstream1, revisionsForDownstream);
        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldFilterOutMaterialRevisionsAtThePointInTheDependencyIfTrackingToolDoesNotMatchWithParent() throws Exception {
        Username user = new Username(new CaseInsensitiveString("user"));

        SvnMaterial svn = MaterialsMother.svnMaterial("http://svn");
        PipelineConfig grandFatherPipeline = configHelper.addPipelineWithGroup("unauthorizedGroup", "granpa", new MaterialConfigs(svn.config()), new TrackingTool("http://jira/${ID}", "some-regex"), "stage", "job");
        configHelper.setViewPermissionForGroup("unauthorizedGroup", CaseInsensitiveString.str(user.getUsername()));

        DependencyMaterial parentDependencyMaterial = MaterialsMother.dependencyMaterial("granpa", "stage");
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", new MaterialConfigs(git.config(), parentDependencyMaterial.config()), "job");

        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        PipelineConfig downstream = configHelper.addPipeline("downstream", "stage", dependencyMaterial.config(), new TrackingTool("http://mingle/${ID}", "another-regex"), "job");

        //Schedule grandfather
        List<MaterialRevision> revisionsForGrandfather1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForGrandfather1, svn);
        Pipeline grandFatherOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), grandFatherPipeline, revisionsForGrandfather1);

        //Schedule upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        dbHelper.addDependencyRevisionModification(revisionsForUpstream1, parentDependencyMaterial, grandFatherOne);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream = new ArrayList<MaterialRevision>();
        dbHelper.addDependencyRevisionModification(revisionsForDownstream, dependencyMaterial, upstreamOne);
        Pipeline pipelineDownstream = dbHelper.checkinRevisionsToBuild(new ManualBuild(user), downstream, revisionsForDownstream);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(pipelineDownstream.getName(), pipelineDownstream.getCounter(), pipelineDownstream.getCounter(), user, result, true, false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream1, revisionsForDownstream);
        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldNotConsider2MingleConfigsAsDifferentIfTheMqlCriteriaAreDifferent() {
        PipelineConfig pipelineConfig = PipelineConfigMother.createPipelineConfig("pipeline", "stage", "job");
        pipelineConfig.setMingleConfig(new MingleConfig("https://foo.com", "go", "status > In Dev"));
        assertThat(changesetService.mingleConfigMatches(pipelineConfig, new MingleConfig("https://foo.com", "go", "status > Something Else")), is(true));
    }

    @Test
    public void shouldReturn_SCMMods_AcrossParentStageFailures() {
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        pipelineConfigWithTwoMaterials.addMaterialConfig(dependencyMaterial.config());
        pipelineConfigWithTwoMaterials.removeMaterialConfig(git.config());

        Username username = new Username(new CaseInsensitiveString("user1"));

        //Schedule first of upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream1, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream1, dependencyMaterial, upstreamOne);
        Pipeline downstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream1);

        //Schedule second upstream
        List<MaterialRevision> revisionsForUpstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream2, git);
        Pipeline upstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream2);

        //Schedule second downstream
        List<MaterialRevision> revisionsForDownstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream2, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream2, dependencyMaterial, upstreamTwo);
        Pipeline downstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream2);

        //Schedule multiple upstream, but no corresponding downstream, because upstream stage failed(may be?)
        List<MaterialRevision> revisionsForUpstream3 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream3, git);
        Pipeline upstreamThree = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream3);

        List<MaterialRevision> revisionsForUpstream4 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream4, git);
        Pipeline upstreamFour = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream4);

        //Schedule upstream again(upstream stage starts passing once again)
        List<MaterialRevision> revisionsForUpstream5 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream5, git);
        Pipeline upstreamFive = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream5);

        //Schedule downstream for comparision
        List<MaterialRevision> revisionsForDownstream3 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream3, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream3, dependencyMaterial, upstreamFive);
        Pipeline downstreamThree = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream3);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), downstreamOne.getCounter(), downstreamThree.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream2, revisionsForUpstream3, revisionsForUpstream4, revisionsForUpstream5, revisionsForDownstream2, revisionsForDownstream3);

        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldNotReturnAMaterialWhenNothingHasChangedInIt() {
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        pipelineConfigWithTwoMaterials.addMaterialConfig(dependencyMaterial.config());
        pipelineConfigWithTwoMaterials.removeMaterialConfig(git.config());

        Username username = new Username(new CaseInsensitiveString("user1"));

        //Schedule first of upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream1, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream1, dependencyMaterial, upstreamOne);
        Pipeline downstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream1);

        //Schedule second upstream
        List<MaterialRevision> revisionsForUpstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream2, git);
        List<MaterialRevision> expectedGitRevision = revisionsForUpstream2;
        Pipeline upstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream2);

        //Schedule second downstream
        List<MaterialRevision> up2Rev = dbHelper.addDependencyRevisionModification(revisionsForDownstream1, dependencyMaterial, upstreamTwo);
        Pipeline downstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream1);//Using the same hg revision.

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), downstreamOne.getCounter(), downstreamTwo.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream2, up2Rev);//Should not contain the hg revision that has not changed

        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturn_SCMMods_AcrossParentStageFailures_WithFailuresNextToFrom() {
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        pipelineConfigWithTwoMaterials.addMaterialConfig(dependencyMaterial.config());
        pipelineConfigWithTwoMaterials.removeMaterialConfig(git.config());

        Username username = new Username(new CaseInsensitiveString("user1"));

        //Schedule first of upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream1, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream1, dependencyMaterial, upstreamOne);
        Pipeline downstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream1);

        //Schedule multiple upstream, but no corresponding downstream, because upstream stage failed(may be?)
        List<MaterialRevision> revisionsForUpstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream2, git);
        Pipeline upstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream2);

        List<MaterialRevision> revisionsForUpstream3 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream3, git);
        Pipeline upstreamThree = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream3);

        //Schedule upstream again(upstream stage starts passing once again)
        List<MaterialRevision> revisionsForUpstream4 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream4, git);
        Pipeline upstreamFour = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream4);

        //Schedule downstream for comparision
        List<MaterialRevision> revisionsForDownstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream2, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream2, dependencyMaterial, upstreamFour);
        Pipeline downstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream2);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), downstreamOne.getCounter(), downstreamTwo.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream2, revisionsForUpstream3, revisionsForUpstream4, revisionsForDownstream2);

        assertMaterialRevisions(expectedRevisions, actual);

        assertThat(result.isSuccessful(), is(true));

        actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), downstreamTwo.getCounter(), downstreamTwo.getCounter(), username, result, true,
                false);//same to and from

        assertMaterialRevisions(expectedRevisions, actual);

        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void shouldReturnModsForRangeOfInstancesOfAnUpstreamPipeline() {
        PipelineConfig upstreamPipeline = configHelper.addPipeline("upstream", "stage", git.config(), "job");
        DependencyMaterial dependencyMaterial = MaterialsMother.dependencyMaterial("upstream", "stage");
        pipelineConfigWithTwoMaterials.addMaterialConfig(dependencyMaterial.config());
        pipelineConfigWithTwoMaterials.removeMaterialConfig(git.config());

        Username username = new Username(new CaseInsensitiveString("user1"));

        //Schedule first of upstream
        List<MaterialRevision> revisionsForUpstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream1, git);
        Pipeline upstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream1);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream1 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream1, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream1, dependencyMaterial, upstreamOne);
        Pipeline downstreamOne = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream1);

        //Schedule multiple upstreams
        List<MaterialRevision> revisionsForUpstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream2, git);
        Pipeline upstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream2);

        List<MaterialRevision> revisionsForUpstream3 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream3, git);
        Pipeline upstreamThree = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream3);

        List<MaterialRevision> revisionsForUpstream4 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForUpstream4, git);
        Pipeline upstreamFour = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), upstreamPipeline, revisionsForUpstream4);

        //Schedule downstream
        List<MaterialRevision> revisionsForDownstream2 = new ArrayList<MaterialRevision>();
        addRevisionWith2Mods(revisionsForDownstream2, hg);
        dbHelper.addDependencyRevisionModification(revisionsForDownstream2, dependencyMaterial, upstreamFour, upstreamThree, upstreamTwo);
        Pipeline downstreamTwo = dbHelper.checkinRevisionsToBuild(new ManualBuild(username), pipelineConfigWithTwoMaterials, revisionsForDownstream2);

        HttpLocalizedOperationResult result = new HttpLocalizedOperationResult();
        List<MaterialRevision> actual = changesetService.revisionsBetween(CaseInsensitiveString.str(pipelineConfigWithTwoMaterials.name()), downstreamTwo.getCounter(), downstreamTwo.getCounter(), username, result, true,
                false);

        List<MaterialRevision> expectedRevisions = groupByMaterial(revisionsForUpstream2, revisionsForUpstream3, revisionsForUpstream4, revisionsForDownstream2);

        assertMaterialRevisions(expectedRevisions, actual);
        assertThat(result.isSuccessful(), is(true));
    }

    @Test
    public void testShouldGroupMaterialsBasedOnFingerprint() {
        SvnMaterial first = new SvnMaterial("url", "user1", "password", false);
        Modification mod1 = new Modification("user1", "comment", "email", new Date(), "revision");
        mod1.setMaterialInstance(new SvnMaterialInstance("url", "user1", "flyweight1", false));

        Modification mod2 = new Modification("user1", "comment", "email", new Date(), "revision");
        mod2.setMaterialInstance(new SvnMaterialInstance("url", "user1", "flyweight1", false));

        Map<Material, Modifications> map = changesetService.groupModsByMaterial(Arrays.asList(mod1, mod2));
        assertThat(map.size(), is(1));
        assertThat(map.get(first), is(new Modifications(mod1, mod2)));
    }

    private void assertMaterialRevisions(List<MaterialRevision> expected, List<MaterialRevision> actual) {
        assertThat(actual.size(), is(expected.size()));
        for (int i = 0, size = expected.size(); i < size; i++) {
            MaterialRevision expectedRevision = expected.get(i);
            MaterialRevision actualRevision = actual.get(i);
            assertEquals(expectedRevision.getMaterial().createMaterialInstance(), actualRevision.getMaterial().createMaterialInstance());
            assertEquals(expectedRevision.getModifications(), actualRevision.getModifications());
        }
    }

    private List<MaterialRevision> groupByMaterial(List<MaterialRevision>... toBeGrouped) {
        MaterialRevisions grouped = new MaterialRevisions();
        for (List<MaterialRevision> materialRevisions : toBeGrouped) {
            for (MaterialRevision materialRevision : materialRevisions) {
                MaterialRevision revision = grouped.findRevisionFor(materialRevision.getMaterial());
                if (revision == null) {
                    revision = new MaterialRevision(materialRevision.getMaterial(), new ArrayList<Modification>());
                    grouped.addRevision(revision);
                }
                revision.addModifications(materialRevision.getModifications());
                Collections.sort(revision.getModifications(), PersistentObject.ORDER_DESCENDING_ID);
            }
        }
        return grouped.getRevisions();
    }

    private void addRevisionWith2Mods(List<MaterialRevision> revisions, final ScmMaterial material) {
        String revision1 = nextInt();
        String revision2 = nextInt();
        revisions.add(dbHelper.addRevisionsWithModifications(material,
                checkinWithComment(revision2, "comment" + revision2, new Date(), "file" + revision2),
                checkinWithComment(revision1, "comment" + revision1, new Date(), "file" + revision1)));
    }

    private String nextInt() {
        return String.valueOf(++counter);
    }

    private void validateFailsForNonNaturalCounter(int fromCounter, int toCounter) {
        HttpLocalizedOperationResult result;
        result = new HttpLocalizedOperationResult();
        changesetService.revisionsBetween("foo", fromCounter, toCounter, new Username(new CaseInsensitiveString("loser")), result, true, false);
        assertThat(result.isSuccessful(), is(false));
        assertThat(result.message(localizer), is("Pipeline counters should be positive."));
        assertThat(result.httpCode(), is(400));
    }

    private List<String> stringRevisions(List<MaterialRevision> revisionList) {
        List<String> stringRevisions = new ArrayList<String>();
        for (MaterialRevision revision : revisionList) {
            for (Modification mod : revision.getModifications()) {
                stringRevisions.add(mod.getRevision());
            }
        }
        return stringRevisions;
    }
}
