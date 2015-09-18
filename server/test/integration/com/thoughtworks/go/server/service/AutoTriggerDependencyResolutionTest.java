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

package com.thoughtworks.go.server.service;

import com.rits.cloning.Cloner;
import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.config.CruiseConfig;
import com.thoughtworks.go.config.GoConfigDao;
import com.thoughtworks.go.config.StageConfig;
import com.thoughtworks.go.config.materials.Filter;
import com.thoughtworks.go.config.materials.IgnoredFiles;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.Material;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.helper.StageConfigMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineConfigDependencyGraph;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.service.dd.MaxBackTrackLimitReachedException;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.ExpectedException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;
import java.util.List;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class AutoTriggerDependencyResolutionTest {
    public static final String STAGE_NAME = "s";
    public static final Cloner CLONER = new Cloner();
    @Autowired private DatabaseAccessHelper dbHelper;
    @Autowired private GoCache goCache;
    @Autowired private GoConfigDao goConfigDao;
    @Autowired private PipelineService pipelineService;
    @Autowired private ScheduleService scheduleService;
    @Autowired private MaterialRepository materialRepository;
    @Autowired private TransactionTemplate transactionTemplate;
    @Autowired private GoConfigService goConfigService;
    @Autowired private SystemEnvironment systemEnvironment;
    @Autowired private MaterialChecker materialChecker;
    @Autowired private PipelineTimeline pipelineTimeline;
    @Autowired private ServerHealthService serverHealthService;

    private GoConfigFileHelper configHelper = new GoConfigFileHelper();
    private ScheduleTestUtil u;

    @Before
    public void setUp() throws Exception {
        goCache.clear();
        configHelper.usingCruiseConfigDao(goConfigDao);
        configHelper.onSetUp();

        dbHelper.onSetUp();
        u = new ScheduleTestUtil(transactionTemplate, materialRepository, dbHelper, configHelper);
    }

    @After
    public void teardown() throws Exception {
        systemEnvironment.reset(SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT);
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    @Ignore("run PipelineDependencyDiamond.scn: this checks for natural-order schedule-order confusion -jj")
    @Test
    public void runTwistTest() {
    }

    @Test
    public void shouldChooseTheAppropriateRevisionsOfAllMaterials_inAComplexMultipleDiamondDependencySituation_withDependencyMaterialsNotResolvingStrictly() throws Exception {
        /**
         * +-----------------------------+
         * |             hg---------->P4 |
         * |             |\           |  |
         * |             | \          |  |
         * |             |  \----\    |  |
         * |             V        \   |  |
         * |      /----> P3 ------\\  |  |
         * |    /                  \\ |  |
         * |  /                     \V|  |
         * |/                       V V  |
         * svn -----> P1 ---------> P6 <-+---+
         *  \         |            ^ ^       |
         *   \        |           /  |       |
         *    \       V         /    |       |
         *     \---> P2-------/      |       |
         *          # ^              |       |
         *            |              |       |
         *           git----------->P5<-----git2
         *
         */
        int i = 1;
        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10"};
        u.checkinInOrder(svn, u.d(i++), svn_revs);

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder2");
        String[] hg_revs = {"h1", "h2", "h3", "h4", "h5", "h6", "h7"};
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        GitMaterial git1 = u.wf(new GitMaterial("git-1"), "folder3");
        u.checkinInOrder(git1, u.d(i++), "g1-1", "g1-2", "g1-3", "g1-4", "g1-5", "g1-6");

        GitMaterial git2 = u.wf(new GitMaterial("git-2"), "folder4");
        String[] git2_revs = {"g2-1", "g2-2", "g2-3", "g2-4", "g2-5", "g2-6", "g2-7", "g2-8"};
        u.checkinInOrder(git2, u.d(i++), git2_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1), u.m(svn), u.m(git1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(svn), u.m(hg));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(hg));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("P5", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("P6", u.m(svn), u.m(p4), u.m(hg), u.m(p3), u.m(p1), u.m(p2), u.m(p5), u.m(git2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s1");
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s2");
        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s3");
        String p1_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s4");
        String p1_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s6");
        String p1_6 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s8");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1, "s1", "g1-2");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2, "s2", "g1-3");
        String p2_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_4, "s5", "g1-5");
        String p2_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_5, "s6", "g1-6");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s1", "h1");
        String p3_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s2", "h3");
        String p3_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s3", "h5");
        String p3_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s7", "h6");
        String p4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h1");
        String p4_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h3");
        String p4_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h6");
        String p4_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h7");
        String p5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), "g1-2", "g2-2");
        String p5_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), "g1-3", "g2-3");
        String p5_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), "g1-6", "g2-5");

        MaterialRevisions given = u.mrs(
                u.mr(rel(git2, p6), true, git2_revs),
                u.mr(rel(p5, p6), false, p5_3),
                u.mr(rel(p2, p6), false, p2_4),
                u.mr(rel(p1, p6), true, p1_6),
                u.mr(rel(p3, p6), false, p3_4),
                u.mr(rel(hg, p6), false, hg_revs),
                u.mr(rel(p4, p6), false, p4_4),
                u.mr(rel(svn, p6), true, svn_revs));
        MaterialRevisions expected = u.mrs(
                u.mr(rel(git2, p6), true, "g2-3"),
                u.mr(rel(p5, p6), false, p5_2),
                u.mr(rel(p2, p6), false, p2_2),
                u.mr(rel(p1, p6), false, p1_2),
                u.mr(rel(p3, p6), false, p3_2),
                u.mr(rel(hg, p6), false, "h3"),
                u.mr(rel(p4, p6), false, p4_2),
                u.mr(rel(svn, p6), true, "s2"));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p6, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    private Material rel(ScheduleTestUtil.AddedPipeline dep, ScheduleTestUtil.AddedPipeline wrtPipeline) {
        return rel(dep.material, wrtPipeline);
    }

    private Material rel(Material material, ScheduleTestUtil.AddedPipeline wrtPipeline) {
        return rel(material, wrtPipeline, 0);
    }

    private Material rel(Material material, ScheduleTestUtil.AddedPipeline wrtPipeline, int nth) {
        for (Material mat : MaterialsMother.createMaterialsFromMaterialConfigs(wrtPipeline.config.materialConfigs())) {
            if (mat.getFingerprint().equals(material.getFingerprint())) {
                if (nth-- > 0) {
                    continue;
                }
                return CLONER.deepClone(mat);
            }
        }
        throw new RuntimeException("You don't have this material configured");
    }

    private MaterialRevisions getRevisionsBasedOnDependencies(ScheduleTestUtil.AddedPipeline p6, CruiseConfig cruiseConfig, MaterialRevisions given) {
        return getRevisionsBasedOnDependencies(cruiseConfig, given, p6.config.name());
    }

    private MaterialRevisions getRevisionsBasedOnDependencies(CruiseConfig cruiseConfig, MaterialRevisions given, CaseInsensitiveString pipelineName) {
        pipelineTimeline.update();
        return pipelineService.getRevisionsBasedOnDependencies(given, cruiseConfig, pipelineName);
    }

    @Test
    public void should_NOT_triggerWithRevisionOfUpstream_whichHasRunUsingMutuallyIncompatibleRevisions() {
        /**
         *                   |> P4
         *                  /   ^
         *                /     |
         *              /       |
         * git -----> P1 ----> P3 (manual)
         *              \     ^
         *               \    |
         *                \   |
         *                 V  |
         *                  P2
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder");
        String[] git_revs = {"g1", "g2"};
        int i = 1;
        u.checkinInOrder(git, u.d(i++), git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p3), u.m(p1));
        CruiseConfig cruiseConfig = goConfigDao.load();

        int extraHours5 = i++;
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(extraHours5), "g1");
        int extraHours4 = i++;
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(extraHours4), p1_1);
        int extraHours3 = i++;
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(extraHours3), p1_1, p2_1);
        int extraHours2 = i++;
        String p4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(extraHours2), p3_1, p1_1);

        int extraHours1 = i++;
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(extraHours1), "g1");
        u.scheduleWith(p2, p1_2);//p2 is running
        int extraHours = i++;
        String p3_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(extraHours), p1_2, p2_1);

        pipelineTimeline.update();

        final MaterialRevisions given = new MaterialRevisions();
        given.addRevision(u.mr(p3, true, p3_2));
        given.addRevision(u.mr(p1, true, p1_2));

        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(given));

    }

    @Test
    public void should_NOT_flip_flop_betweenTwoSetsOfAutoTriggerRevisionCombinations() {
        /**
         * git ---> P1
         *  | \     |
         *  |  \    |
         *  |   \   |
         *  |    \  |
         *  V    V  V
         * P2 ---> P3
         *
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2), u.m(git));
        CruiseConfig cruiseConfig = goConfigDao.load();

        int i = 0; // i counts hour, increments everytime,
        // important because we find auto-trigger compatible revision combinations based on time
        // (and this is what the twist test that reproduced the bug first time was using)

        int extraHours18 = i++;
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(extraHours18), "g1");
        int extraHours17 = i++;
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(extraHours17), "g1");

        int extraHours16 = i++;
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(extraHours16), p1_1, p2_1, "g1");

        int extraHours15 = i++;
        u.checkinInOrder(git, u.d(extraHours15), "g2");
        int extraHours14 = i++;
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(extraHours14), "g2");

        int extraHours13 = i++;
        u.checkinInOrder(git, u.d(extraHours13), "g3");
        int extraHours12 = i++;
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(extraHours12), "g3");

        int extraHours11 = i++;
        u.checkinInOrder(git, u.d(extraHours11), "g4");
        int extraHours10 = i++;
        String p2_3 = u.runAndPassWithGivenMDUTimestampAndRevisionObjects(p2, u.d(extraHours10), u.rs("g3", "g4"));

        int extraHours9 = i++;
        u.checkinInOrder(git, u.d(extraHours9), "g5");
        int extraHours8 = i++;
        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionObjects(p1, u.d(extraHours8), u.rs("g4", "g5"));
        int extraHours7 = i++;
        String p2_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(extraHours7), "g5");

        int extraHours6 = i++;
        List<String> revsForP1 = u.revisions(p1_3);
        List<String> revsForP2 = u.revisions(p2_4);
        List<String> revsForGit = u.revisions("g2", "g3", "g4", "g5");

        String p3_2 = u.runAndPassWithGivenMDUTimestampAndRevisionObjects(p3, u.d(extraHours6), u.getRevisionsForMaterials(revsForP1, revsForP2, revsForGit));

        int extraHours5 = i++;
        u.checkinInOrder(git, u.d(extraHours5), "g6");
        int extraHours4 = i++;
        String p2_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(extraHours4), "g6");

        int extraHours3 = i++;
        u.checkinInOrder(git, u.d(extraHours3), "g7");
        int extraHours2 = i++;
        String p1_4 = u.runAndPassWithGivenMDUTimestampAndRevisionObjects(p1, u.d(extraHours2), u.rs("g6", "g7"));

        int extraHours1 = i++;
        String p2_6 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(extraHours1), "g3");

        int extraHours = i++;
        String p3_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(extraHours), p1_2, p2_6, "g3");

        MaterialRevisions given = u.mrs(u.mr(p1, true, p1_4),
                u.mr(p2, false, p2_6),
                u.mr(git, true, "g7"));

        MaterialRevisions previous = u.mrs(new MaterialRevision[]{
                u.mr(p1, true, p1_2),
                u.mr(p2, false, p2_6),
                u.mr(git, true, "g3")});

        pipelineTimeline.update();

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, "P3", systemEnvironment, materialChecker, serverHealthService);

        assertThat(autoBuild.onModifications(given, false, previous), nullValue());
    }

    @Test
    @Ignore("We do not support duplicate materials now")
    public void shouldTrigger_usingFirstMaterialDeclarationToResolveDependencies_inCasePipelineHasMultipleMaterialDeclarationsToTheSameMaterial() throws Exception {
        /**
         *  +---------------------------+
         *  |                           |
         * git =======> P1--------+     |
         *  \\          |         |     |
         *   \\         |         |    /
         *    \\        v         v  V
         *     \\=====> P2 -----> P3
         *
         * P1    1(g1,g1)M            2(g1,g2)M                                                        3(g2,g1)M
         * P2    1(P1/1,g1,g1)A       2(P1/2,g1,g1)A       3(P1/2,g1,g3)M       4(P1/2,g2,g1)M         5(P1/3,g2,g1)M
         * P3    1(P1/1,P2/1,g1)A     2(P1/2,P2/2,g1)A     3(P1/2,P2/3,g1)A          NONE              4(P1/3,P2/5,g2)A
         */

        GitMaterial git = new GitMaterial("git");
        String[] git_revs = {"g1", "g2", "g3"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git, "p1-first"), u.m(git, "p1-second"));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1), u.m(git, "p2-first"), u.m(git, "p2-second"));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2), u.m(git));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPass(p1, "g1", "g1");

        assertThat(getRevisionsBasedOnDependencies(p2, cruiseConfig,
                u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p2), true, p1_1),
                        u.mr(rel(git, p2, 0), true, "g1", "g2", "g3"),
                        u.mr(rel(git, p2, 1), true, "g1", "g2", "g3")})),
                is(u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p2), true, p1_1),
                        u.mr(rel(git, p2, 0), true, "g1"),
                        u.mr(rel(git, p2, 1), true, "g1")})));
        String p2_1 = u.runAndPass(p2, p1_1, "g1", "g1");

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig,
                u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_1),
                        u.mr(rel(p2, p3), true, p2_1),
                        u.mr(rel(git, p3), true, "g1", "g2", "g3")})),
                is(u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_1),
                        u.mr(rel(p2, p3), true, p2_1),
                        u.mr(rel(git, p3), true, "g1")})));
        String p3_1 = u.runAndPass(p3, p1_1, p2_1, "g1");

        String p1_2 = u.runAndPass(p1, "g1", "g2");

        assertThat(getRevisionsBasedOnDependencies(p2, cruiseConfig,
                u.mrs(new MaterialRevision[]{u.mr(rel(p1, p2), true, p1_2),
                        u.mr(rel(git, p2, 0), true, "g2", "g3"),
                        u.mr(rel(git, p2, 1), true, "g2", "g3")})),
                is(u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p2), true, p1_2),
                        u.mr(rel(git, p2, 0), false, "g1"),
                        u.mr(rel(git, p2, 1), false, "g1")})));
        String p2_2 = u.runAndPass(p2, p1_2, "g1", "g1");

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig,
                u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_2),
                        u.mr(rel(p2, p3), true, p2_2),
                        u.mr(rel(git, p3), true, "g2", "g3")})),
                is(u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_2),
                        u.mr(rel(p2, p3), true, p2_2),
                        u.mr(rel(git, p3), false, "g1")})));
        String p3_2 = u.runAndPass(p3, p1_2, p2_2, "g1");

        String p2_3 = u.runAndPass(p2, p1_2, "g1", "g3");

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig,
                u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_2),
                        u.mr(rel(p2, p3), true, p2_3),
                        u.mr(rel(git, p3), true, "g2", "g3")})),
                is(u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_2),
                        u.mr(rel(p2, p3), true, p2_3),
                        u.mr(rel(git, p3), false, "g1")})));
        String p3_3 = u.runAndPass(p3, p1_2, p2_3, "g1");

        String p2_4 = u.runAndPass(p2, p1_2, "g2", "g1");

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, "P3", systemEnvironment, materialChecker, serverHealthService);
        MaterialRevision[] given = {
                u.mr(rel(p1, p3), true, p1_2),
                u.mr(rel(p2, p3), true, p2_4),
                u.mr(rel(git, p3), true, "g2", "g3")};
        MaterialRevision[] previous = {
                u.mr(rel(p1, p3), true, p1_2),
                u.mr(rel(p2, p3), true, p2_3),
                u.mr(rel(git, p3), true, "g1")};
        BuildCause buildCause = autoBuild.onModifications(u.mrs(given), false, u.mrs(previous));
        assertThat(buildCause, is(nullValue()));

        String p1_3 = u.runAndPass(p1, "g2", "g1");
        String p2_5 = u.runAndPass(p2, p1_3, "g2", "g1");

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig,
                u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_3),
                        u.mr(rel(p2, p3), true, p2_5),
                        u.mr(rel(git, p3), true, "g2", "g3")})),
                is(u.mrs(new MaterialRevision[]{
                        u.mr(rel(p1, p3), true, p1_3),
                        u.mr(rel(p2, p3), true, p2_5),
                        u.mr(rel(git, p3), true, "g2")})));
    }

    @Test
    @Ignore("We do not support this now")
    public void shouldTriggerWithRevisions_acrossDay2_whenSharedMaterialIsNotAvailableInDay2() throws Exception {
        /**
         * day 1.
         * git -------> P1--------+
         *   \                    |
         *    \                   |
         *     \                  v
         *      -----> P2 -----> P3
         *
         * day 2.
         * git -------> P1--------+
         *                        |
         *                        |
         *                        v
         * svn -------> P2 -----> P3
         *
         * day 3.
         * git -------> P1--------+
         *   \                    |
         *    \                   |
         *     \                  v
         *      -----> P2 -----> P3
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2", "g3"};
        int i = 1;
        u.checkinInOrder(git, u.d(i++), git_revs);

        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder2");
        String[] svn_revs = {"s1"};
        u.checkinInOrder(svn, u.d(i++), svn_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        //day 1:
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g2");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1, p2_1);

        //day 2:
        configHelper.setMaterialConfigForPipeline("P2", svn.config());
        cruiseConfig = goConfigDao.load();
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g3");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(new ScheduleTestUtil.AddedPipeline(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P2")), p2.material), u.d(i++),
                "s1");

        //day 3:
        configHelper.setMaterialConfigForPipeline("P2", git.config());
        cruiseConfig = goConfigDao.load();
        try {
            getRevisionsBasedOnDependencies(p3, cruiseConfig, u.mrs(
                    u.mr(p1, true, p1_2),
                    u.mr(p2, true, p2_2)));
            fail("should not have found a compatible revision");
        } catch (NoCompatibleUpstreamRevisionsException e) {
            //ignore
        }

        String p1_3 = u.runAndPass(p1, "g2");
        String p3_2 = u.runAndPass(p3, p1_3, p2_2);

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(p2, true, p2_2))), is(u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(p2, false, p2_1))));
    }

    @Test
    public void shouldTriggerWithRevisions_inDay3_whenSharedMaterialIsNotAvailableInDay2() throws Exception {
        /**
         * day 1.
         * git -------> P1--------+
         *   \                    |
         *    \                   |
         *     \                  v
         *      -----> P2 -----> P3
         *
         * day 2.
         * git -------> P1--------+
         *                        |
         *                        |
         *                        v
         * svn -------> P2 -----> P3
         *
         * day 3.
         * git -------> P1--------+
         *   \                    |
         *    \                   |
         *     \                  v
         *      -----> P2 -----> P3
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2", "g3"};
        u.checkinInOrder(git, git_revs);

        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder2");
        String[] svn_revs = {"s1"};
        u.checkinInOrder(svn, svn_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        //day 1:
        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, "g1");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1);

        //day 2:
        configHelper.setMaterialConfigForPipeline("P2", svn.config());
        cruiseConfig = goConfigDao.load();
        String p1_2 = u.runAndPass(p1, "g2");
        String p2_2 = u.runAndPass(new ScheduleTestUtil.AddedPipeline(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P2")), p2.material), "s1");
        String p3_2 = u.runAndPass(p3, p1_2, p2_2);

        //day 3:
        configHelper.setMaterialConfigForPipeline("P2", git.config());
        cruiseConfig = goConfigDao.load();
        String p1_3 = u.runAndPass(p1, "g2");
        String p2_3 = u.runAndPass(p2, "g2");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(p2, false, p2_3));

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(given));
    }

    @Test
    @Ignore("We do not support this now")
    public void shouldNotTrigger_withUserRevisionsRunWithoutSharedMaterial_butShouldLookPastSuchInstancesForOlderPipelines() throws Exception {
        /**
         * day 1.
         * git -------> P1--------+
         *   \                    |
         *    \                   |
         *     \                  v
         *      -----> P2 -----> P3
         *
         * day 2.
         * git -------> P1--------+
         *                        |
         *                        |
         *                        v
         * svn -------> P2 -----> P3
         *
         * day 3.
         * git -------> P1--------+
         *   \                    |
         *    \                   |
         *     \                  v
         *      -----> P2 -----> P3
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2", "g3"};
        u.checkinInOrder(git, git_revs);

        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder2");
        String[] svn_revs = {"s1"};
        u.checkinInOrder(svn, svn_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        //day 1:
        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, "g1");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1);

        //day 2:
        configHelper.setMaterialConfigForPipeline("P2", svn.config());
        cruiseConfig = goConfigDao.load();
        String p1_2 = u.runAndPass(p1, "g2");
        String p2_2 = u.runAndPass(new ScheduleTestUtil.AddedPipeline(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P2")), p2.material), "s1");
        String p3_2 = u.runAndPass(p3, p1_2, p2_2);

        //day 3:
        configHelper.setMaterialConfigForPipeline("P2", git.config());
        cruiseConfig = goConfigDao.load();
        String p1_3 = u.runAndPass(p1, "g2");
        String p2_3 = u.runAndPass(p2, "g2");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(p2, false, p2_3));

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(given));

        String p1_4 = u.runAndPass(p1, "g1");

        given = u.mrs(
                u.mr(p1, true, p1_4),
                u.mr(p2, false, p2_3));

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(u.mrs(
                u.mr(p1, true, p1_4),
                u.mr(p2, false, p2_1))));

        String p3_3 = u.runAndPass(p3, p1_4, p2_1);

        String p1_5 = u.runAndPass(p1, "g3");

        given = u.mrs(
                u.mr(p1, true, p1_5),
                u.mr(p2, false, p2_3));

        MaterialRevisions previous = u.mrs(
                u.mr(p1, true, p1_4),
                u.mr(p2, false, p2_1));

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, "P3", systemEnvironment, materialChecker, serverHealthService);
        assertThat(autoBuild.onModifications(given, false, previous), nullValue());
    }

    @Test
    public void shouldTrigger_withLatestOfUnsharedMaterial_keepingAllTheSharedMaterialRevisionsPegged() throws Exception {
        /**
         *
         * git -------> P1--------+
         *              |         |
         *              |         |
         *              v         v
         *              P2 -----> P3 <---- svn
         *
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2"};
        u.checkinInOrder(git, git_revs);

        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder2");
        String[] svn_revs = {"s1", "s2"};
        u.checkinInOrder(svn, svn_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2), u.m(svn));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, p1_1);

        String p1_2 = u.runAndPass(p1, "g2");
        u.scheduleWith(p2, p1_2);

        String p3_1 = u.runAndPass(p3, p1_1, p2_1, "s1");

        MaterialRevisions given = u.mrs(u.mr(p2, true, p2_1),
                u.mr(u.mw(p1), false, p1_2),
                u.mr(svn, true, "s2"));

        MaterialRevisions expected = u.mrs(u.mr(p2, true, p2_1),
                u.mr(u.mw(p1), false, p1_1),
                u.mr(svn, true, "s2"));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p3, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldPrefer_NewModifications_Over_OldModifications_whenFindingCompatibleRevisions() throws Exception {
        /**
         *
         * git -------> P1--------+
         *              |         |
         *              |         |
         *              v         v
         *              P2 -----> P3
         *
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1"};
        int i = 0;
        u.checkinInOrder(git, u.d(i++), git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        String p2_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);

        MaterialRevisions given = u.mrs(u.mr(p2, true, p2_3),
                u.mr(u.mw(p1), false, p1_2));

        MaterialRevisions expected = u.mrs(u.mr(p2, true, p2_3),
                u.mr(u.mw(p1), false, p1_2));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p3, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldPrefer_combinationThatBringsLatestPossibleRevisionsOfMostMaterials_whileComputingCompatibleRevisions() throws Exception {
        /**
         *
         *      +------ git ------+
         *      |        |        |
         *      |        |        |
         *      v        v        v
         *      P1      P2       P3
         *      |        |        |
         *      |        |        |
         *      +-----> P4 <------+
         *
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2", "g3", "g4"};
        int i = 1;
        u.checkinInOrder(git, u.d(i++), git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(git));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p2), u.m(p1), u.m(p3));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1");
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g2", "g3");
        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g4");
        String p1_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g2");
        String p1_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g4");

        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g1");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g2");
        String p2_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g3");
        String p2_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g4");
        String p2_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g2");//latest
        //Does this honour schedule order?
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "g1");
        String p3_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "g2");
        String p3_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "g3");
        String p3_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "g3");
        String p3_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "g4");

        MaterialRevisions given = u.mrs(u.mr(p2, true, p2_5),
                u.mr(p1, true, p1_5),
                u.mr(p3, false, p3_5));

        MaterialRevisions expected = u.mrs(u.mr(p2, true, p2_5),
                u.mr(p1, false, p1_4),
                u.mr(p3, false, p3_2));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p4, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldPrefer_materialRevisionThatChangedLast_over_anyMaterialRevisionsThatAreChanged_butAreOlder_whileComputingCompatibleRevisions() throws Exception {
        /**
         *
         *      +------ git ------+
         *      |        |        |
         *      |        |        |
         *      v        v        v
         *      P1      P2       P3
         *      |        |        |
         *      |        |        |
         *      +-----> P4 <------+
         *
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2", "g3", "g4"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(git));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p2), u.m(p1), u.m(p3));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPass(p1, "g1");
        String p1_2 = u.runAndPass(p1, "g2", "g3");
        String p1_3 = u.runAndPass(p1, "g4");
        String p1_4 = u.runAndPass(p1, "g2");
        String p1_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(2), "g4");

        String p2_1 = u.runAndPass(p2, "g1");
        String p2_2 = u.runAndPass(p2, "g2");
        String p2_3 = u.runAndPass(p2, "g3");
        String p2_4 = u.runAndPass(p2, "g4");
        String p2_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(3), "g2");//latest

        String p3_1 = u.runAndPass(p3, "g1");
        String p3_2 = u.runAndPass(p3, "g2");
        String p3_3 = u.runAndPass(p3, "g3");
        String p3_4 = u.runAndPass(p3, "g3");
        String p3_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(1), "g4");

        MaterialRevisions given = u.mrs(u.mr(p2, true, p2_5),
                u.mr(p1, true, p1_5),
                u.mr(p3, false, p3_5));

        MaterialRevisions expected = u.mrs(u.mr(p2, true, p2_5),
                u.mr(p1, false, p1_4),
                u.mr(p3, false, p3_2));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p4, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    @Test
    @Ignore("Changing and swapping the commented line will make this test work. Since we honour schedule order - we do not support this  now")
    public void shouldChooseTheAppropriateRevisionsOfAllMaterials_inAComplexMultipleDiamondDependencySituation_withDependencyMaterialIsNotResolvingStrictly_forAParent() throws Exception {
        /**
         * +-----------------------------+
         * |             hg---------->P4 |
         * |             |\           |  |
         * |             | \          |  |
         * |             |  \----\    |  |
         * |             V        \   |  |
         * |      /----> P3 ------\\  |  |
         * |    /                  \\ |  |
         * |  /                     \V|  |
         * |/                       V V  |
         * svn -----> P1 ---------> P6 <-+---+
         *  \         |            ^ ^       |
         *   \        |           /  |       |
         *    \       V         /    |       |
         *     \---> P2-------/      |       |
         *          # ^              |       |
         *            |              |       |
         *           git----------->P5<-----git2
         *
         */
        int i = 1;
        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2", "s3", "s4", "s5", "s6", "s7", "s8", "s9", "s10"};
        u.checkinInOrder(svn, u.d(i++), svn_revs);

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder2");
        String[] hg_revs = {"h1", "h2", "h3", "h4", "h5", "h6", "h7"};
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        GitMaterial git1 = u.wf(new GitMaterial("git-1"), "folder3");
        u.checkinInOrder(git1, u.d(i++), "g1-1", "g1-2", "g1-3", "g1-4", "g1-5", "g1-6");

        GitMaterial git2 = u.wf(new GitMaterial("git-2"), "folder4");
        String[] git2_revs = {"g2-1", "g2-2", "g2-3", "g2-4", "g2-5", "g2-6", "g2-7", "g2-8"};
        u.checkinInOrder(git2, u.d(i++), git2_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(svn), u.m(p1), u.m(git1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(svn), u.m(hg));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(hg));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("P5", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("P6", u.m(svn), u.m(p4), u.m(hg), u.m(p3), u.m(p1), u.m(p2), u.m(p5), u.m(git2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s1");
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s2");
        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s3");
        String p1_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s4");
        String p1_5 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s6");
        String p1_6 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "s8");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "s1", p1_1, "g1-2");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "s4", p1_4, "g1-3");
        String p2_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "s5", p1_4, "g1-5");
        String p2_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "s6", p1_5, "g1-6");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s1", "h1");
//        String p3_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s3", "h5");
        String p3_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s4", "h3");
        String p3_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s3", "h5");
        String p3_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), "s7", "h6");
        String p4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h1");
        String p4_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h3");
        String p4_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h6");
        String p4_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "h7");
        String p5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), "g1-2", "g2-2");
        String p5_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), "g1-3", "g2-3");
        String p5_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), "g1-6", "g2-5");

        MaterialRevisions given = u.mrs(u.mr(git2, false, git2_revs),
                u.mr(p5, false, p5_3),
                u.mr(p2, false, p2_4),
                u.mr(p1, false, p1_6),
                u.mr(p3, false, p3_4),
                u.mr(hg, false, hg_revs),
                u.mr(p4, false, p4_4),
                u.mr(svn, false, svn_revs));

        MaterialRevisions expected = u.mrs(u.mr(git2, false, "g2-1", "g2-2", "g2-3"),
                u.mr(p5, false, p5_2),
                u.mr(p2, false, p2_2),
                u.mr(p1, false, p1_4),
                u.mr(p3, false, p3_2),
                u.mr(hg, false, "h1", "h2", "h3"),
                u.mr(p4, false, p4_2),
                u.mr(svn, false, "s1", "s2", "s3", "s4"));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p6, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldGetTheRevisionsFromTheUpStreamPipelineThatUsesTheSameMaterial() throws Exception {
        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2", "s3"};
        u.checkinInOrder(svn, svn_revs);

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder2");
        String[] hg_revs = {"h1", "h2", "h3"};
        u.checkinInOrder(hg, hg_revs);

        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(svn));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(svn), u.m(hg));

        String up1_1 = u.runAndPass(up1, "s1");

        MaterialRevisions given = u.mrs(u.mr(svn, true, svn_revs),
                u.mr(hg, true, hg_revs),
                u.mr(up1, true, up1_1));

        MaterialRevisions expected = u.mrs(u.mr(svn, true, "s1"),
                u.mr(hg, true, "h3"),
                u.mr(up1, true, up1_1));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("current"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldGetTheRevisionsFromTheUpStreamPipelineFor2SameMaterial() throws Exception {
        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2", "s3"};
        u.checkinInOrder(svn, svn_revs);

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder2");
        String[] hg_revs = {"h1", "h2", "h3"};
        u.checkinInOrder(hg, hg_revs);

        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(svn), u.m(hg));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(svn), u.m(hg));

        String up1_1 = u.runAndPass(up1, "s1", "h1");

        MaterialRevisions given = u.mrs(u.mr(svn, true, svn_revs),
                u.mr(hg, true, hg_revs),
                u.mr(up1, true, up1_1));

        MaterialRevisions expected = u.mrs(u.mr(svn, true, "s1"),
                u.mr(hg, true, "h1"),
                u.mr(up1, true, up1_1));

        assertThat(getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("current")), is(expected));
    }

    @Test
    public void shouldChooseTheRevisionFromThirdWhenSecondIsNotModified() throws Exception {
        //      Third <- Second
        //         |     /
        //         |   /
        //         Last
        //

        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1"};
        u.checkinInOrder(svn, svn_revs);

        ScheduleTestUtil.AddedPipeline second = u.saveConfigWith("second", u.m(svn));
        ScheduleTestUtil.AddedPipeline third = u.saveConfigWith("third", u.m(second));
        ScheduleTestUtil.AddedPipeline last = u.saveConfigWith("last", u.m(second), u.m(third));

        String second_1 = u.runAndPass(second, "s1");
        String second_2 = u.runAndPass(second, "s1");
        String second_3 = u.runAndPass(second, "s1");
        String second_4 = u.runAndPass(second, "s1");

        String third_1 = u.runAndPass(third, second_1);
        String third_2 = u.runAndPass(third, second_2);
        String third_3 = u.runAndPass(third, second_2);

        MaterialRevisions given = u.mrs(u.mr(third, true, third_3),
                u.mr(second, true, second_4));

        MaterialRevisions expected = u.mrs(u.mr(third, true, third_3),
                u.mr(second, true, second_4));

        assertThat(getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("last")), is(expected));
    }

    @Test
    public void shouldChooseTheRevisionFromSecondWhenThirdIsNotModified() throws Exception {
        //      Third <- Second
        //         |     /
        //         |   /
        //         Last
        //

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2"};
        int i = 1;
        u.checkinInOrder(svn, u.d(i++), svn_revs);

        ScheduleTestUtil.AddedPipeline second = u.saveConfigWith("second", u.m(svn));
        ScheduleTestUtil.AddedPipeline third = u.saveConfigWith("third", u.m(second));
        ScheduleTestUtil.AddedPipeline last = u.saveConfigWith("last", u.m(second), u.m(third));

        String second_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s1");
        String second_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s1");
        String second_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s2");
        String second_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s2");

        String third_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(third, u.d(i++), second_2);
        String third_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(third, u.d(i++), second_2);
        String third_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(third, u.d(i++), second_2);

        MaterialRevisions given = u.mrs(u.mr(third, true, third_3),
                u.mr(second, true, second_4));

        MaterialRevisions expected = u.mrs(u.mr(third, true, third_3),
                u.mr(second, true, second_2));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("last"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldChooseTheRevisionFromThirdWhenBothThirdAndSecondAreModified() throws Exception {
        //      Third <- Second <-- SVN
        //         |      |
        //         v      v
        //           Last

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2"};
        int i = 1;
        u.checkinInOrder(svn, u.d(i++), svn_revs);

        ScheduleTestUtil.AddedPipeline second = u.saveConfigWith("second", u.m(svn));
        ScheduleTestUtil.AddedPipeline third = u.saveConfigWith("third", u.m(second));
        ScheduleTestUtil.AddedPipeline last = u.saveConfigWith("last", u.m(second), u.m(third));

        String second_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s1");
        String second_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s1");
        String second_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s1");
        String second_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(second, u.d(i++), "s2");

        String third_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(third, u.d(i++), second_1);
        String third_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(third, u.d(i++), second_1);
        String third_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(third, u.d(i++), second_2);

        MaterialRevisions given = u.mrs(u.mr(third, true, third_3),
                u.mr(second, true, second_4));

        MaterialRevisions expected = u.mrs(u.mr(third, true, third_3),
                u.mr(second, true, second_3));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("last"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldIgnoreUpstreamPipelineWhenThereIsNothingInCommon() throws Exception {

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        String[] svn_revs = {"s1", "s2"};
        int i = 1;
        u.checkinInOrder(svn, u.d(i++), svn_revs);

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder2");
        String[] hg_revs = {"hg1", "hg2", "hg3"};
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        ScheduleTestUtil.AddedPipeline up0 = u.saveConfigWith("up0", u.m(hg));
        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(up0));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up0), u.m(up1), u.m(svn));

        String up0_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up0, u.d(i++), "hg1", "hg2", "hg3");
        String up0_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up0, u.d(i++), "hg1", "hg2", "hg3");
        String up1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i++), up0_1);

        MaterialRevisions given = u.mrs(u.mr(up0, true, up0_2),
                u.mr(up1, false, up1_1),
                u.mr(svn, true, "s1", "s2"));

        MaterialRevisions expected = u.mrs(u.mr(up0, true, up0_2),
                u.mr(up1, false, up1_1),
                u.mr(svn, true, "s2"));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("current"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldGetTheRevisionsForDependencyMaterialFromUpStreamPipeline() throws Exception {
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"hg1", "hg2", "hg3", "hg4"};
        int i = 1;
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        ScheduleTestUtil.AddedPipeline common = u.saveConfigWith("common", u.m(hg));
        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(common));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(common));

        String common_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), "hg1");
        String common_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), "hg2");
        String common_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), "hg3");
        String common_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), "hg4");
        String up1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i++), common_3);

        MaterialRevisions given = u.mrs(u.mr(up1, false, up1_1),
                u.mr(common, false, common_4));

        MaterialRevisions expected = u.mrs(u.mr(up1, false, up1_1),
                u.mr(common, false, common_3));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("current"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldGetTheRevisionsForDependencyMaterial_WithSharedParentInMiddleOfTheTree() throws Exception {
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"hg1", "hg2"};
        int i = 1;
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        ScheduleTestUtil.AddedPipeline commonParent = u.saveConfigWith("commonParent", u.m(hg));
        ScheduleTestUtil.AddedPipeline common = u.saveConfigWith("common", u.m(commonParent));
        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(common));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(common));

        String commonParent_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(commonParent, u.d(i++), "hg1");
        String commonParent_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(commonParent, u.d(i++), "hg2");
        String common_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), commonParent_1);
        String common_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), commonParent_1);
        String common_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), commonParent_2);
        String common_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(common, u.d(i++), commonParent_2);
        String up1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i++), common_3);

        MaterialRevisions given = u.mrs(u.mr(up1, false, up1_1),
                u.mr(common, false, common_4));

        MaterialRevisions expected = u.mrs(u.mr(up1, false, up1_1),
                u.mr(common, false, common_4));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("current"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldGetTheRevisionsFromTheNearestUpStreamPipeline() throws Exception {
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"hg1", "hg2", "hg3"};
        int i = 1;
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        ScheduleTestUtil.AddedPipeline up0 = u.saveConfigWith("up0", u.m(hg));
        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(up0), u.m(hg));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(hg));


        String up0_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up0, u.d(i++), "hg1");
        String up0_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up0, u.d(i++), "hg2");
        String up0_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up0, u.d(i++), "hg3");
        String up1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i), up0_2, "hg2");

        MaterialRevisions given = u.mrs(u.mr(hg, true, "hg3"),
                u.mr(up1, true, up1_1));

        MaterialRevisions expected = u.mrs(u.mr(hg, true, "hg2"),
                u.mr(up1, true, up1_1));

        MaterialRevisions revisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("current"));
        assertThat(revisions, is(expected));
    }

    @Test
    public void shouldNotGetTheRevisionsFromUpStreamPipelineIfTheDependencyMaterialHasNotChanged() throws Exception {
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"hg1", "hg2", "hg3"};
        u.checkinInOrder(hg, hg_revs);

        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(hg));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(hg), u.m(up1));

        String up1_1 = u.runAndPass(up1, "hg1");

        MaterialRevisions given = u.mrs(u.mr(hg, false, "hg1"),
                u.mr(up1, false, up1_1));

        MaterialRevisions expected = u.mrs(u.mr(hg, false, "hg1"),
                u.mr(up1, false, up1_1));

        assertThat(getRevisionsBasedOnDependencies(current, goConfigDao.load(), given), is(expected));
    }

    @Test
    public void shouldGetTheRevisionsFromTheUpStreamPipelineThatUsesTheSameMaterialEvenIfItIsNotADirectMaterial() throws Exception {
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"hg1", "hg2", "hg3"};
        u.checkinInOrder(hg, hg_revs);

        ScheduleTestUtil.AddedPipeline up0 = u.saveConfigWith("up0", u.m(hg));
        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(up0));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(hg));

        String up0_1 = u.runAndPass(up0, "hg1");
        String up0_2 = u.runAndPass(up0, "hg1");
        String up1_1 = u.runAndPass(up1, up0_2);

        MaterialRevisions given = u.mrs(u.mr(hg, false, "hg3"),
                u.mr(up1, true, up1_1));

        MaterialRevisions expected = u.mrs(u.mr(hg, false, "hg1"),
                u.mr(up1, true, up1_1));

        assertThat(getRevisionsBasedOnDependencies(current, goConfigDao.load(), given), is(expected));
    }

    @Test
    public void shouldFailGracefully_whenOneOfTheUpstreamPipelineInvolved_doNotHaveAnyInstances() throws Exception {
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"hg1"};
        int i = 1;
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        ScheduleTestUtil.AddedPipeline up0 = u.saveConfigWith("up0", u.m(hg));
        ScheduleTestUtil.AddedPipeline up0_peer = u.saveConfigWith("up0-peer", u.m(hg));
        ScheduleTestUtil.AddedPipeline up1 = u.saveConfigWith("up1", u.m(up0));
        ScheduleTestUtil.AddedPipeline current = u.saveConfigWith("current", u.m(up1), u.m(hg));

        String up0_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up0, u.d(i++), "hg1");
        String up1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(up1, u.d(i), up0_1);

        configHelper.setMaterialConfigForPipeline("up1", up0_peer.materialConfig());
        configHelper.load();

        MaterialRevisions given = new MaterialRevisions();
        given.addRevision(u.mr(hg, false, "hg1"));
        given.addRevision(u.mr(up1, true, up1_1));

        try {
            getRevisionsBasedOnDependencies(current, goConfigDao.load(), given);
            fail("Should have detected no-compatible-revisions situation, as config has changed.");
        } catch (NoCompatibleUpstreamRevisionsException e) {
            //ignore
        }

    }

    @Test
    public void shouldChooseTheRevisionFromSecondInAComplexSituation() throws Exception {
        // hg -> First          git
        //  |      \             |
        //  |      Third  <- Second
        //  |        |      /
        //  |        |    /
        //  +------> Last
        //

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");

        String[] hg_revs = {"h1", "h2"};
        u.checkinInOrder(hg, hg_revs);

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        String[] git_revs = {"g1", "g2"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline first = u.saveConfigWith("first", u.m(hg));
        ScheduleTestUtil.AddedPipeline second = u.saveConfigWith("second", u.m(git));
        ScheduleTestUtil.AddedPipeline third = u.saveConfigWith("third", u.m(first), u.m(second));
        ScheduleTestUtil.AddedPipeline last = u.saveConfigWith("last", u.m(hg), u.m(second), u.m(third));

        String first_1 = u.runAndPass(first, "h1");
        String second_1 = u.runAndPass(second, "g1");
        String second_2 = u.runAndPass(second, "g1");
        String second_3 = u.runAndPass(second, "g2");
        String second_4 = u.runAndPass(second, "g2");
        String third_1 = u.runAndPass(third, first_1, second_2);
        String third_2 = u.runAndPass(third, first_1, second_2);
        String third_3 = u.runAndPass(third, first_1, second_2);

        MaterialRevisions given = u.mrs(u.mr(hg, true, hg_revs),
                u.mr(second, true, second_4),
                u.mr(third, true, third_3));

        MaterialRevisions expected = u.mrs(u.mr(hg, true, "h1"),
                u.mr(second, true, second_2),
                u.mr(third, true, third_3));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("last"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldNotTriggerWithRevisions_ForWhichCurrentlyTheMaterialConfigurationIsDifferent() throws Exception {
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder1");
        int i = 1;
        String[] git_revs1 = {"g11"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder2");
        String[] git_revs2 = {"g21", "g22"};
        u.checkinInOrder(git2, u.d(i++), git_revs2);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        //day 1:
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11", "g21");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11", "g21");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1, p2_1);

        //day 2:
        configHelper.setMaterialConfigForPipeline("P2", git1.config());
        cruiseConfig = goConfigDao.load();
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i), "g11", "g22");
        ScheduleTestUtil.AddedPipeline new_p2 = new ScheduleTestUtil.AddedPipeline(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("p2")), p2.material);
        u.scheduleWith(new_p2, "g11");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_2),
                u.mr(new_p2, false, p2_1));

        try {
            assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(nullValue()));
            fail("Should have failed. There is no compatiable revision as material configuration has changed for p2");
        } catch (NoCompatibleUpstreamRevisionsException e) {
            assertThat(e.getMessage(), is("Failed resolution of pipeline p3 : Cause : No valid revisions found for the upstream dependency: DependencyMaterialConfig{pipelineName='p2', stageName='s'}"));
        }
    }

    @Test
    public void shouldResolveSimpleDiamond() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2), u.m(p3));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1);

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1));

        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(given));

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        given = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(p3, false, p3_1));

        MaterialRevisions expected = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1));
        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(expected));
    }

    @Test
    public void shouldResolveWhenAChainOfDependencyMaterialConstituteASharedMaterial() {
        //  +--------> P1 ------+
        //  |                   |
        //  |                   |
        // git                 P2
        //  |                   |
        //  +--------> P3<------+


        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder-name");
        String[] git_revs1 = {"g11", "g12"};
        int i = 1;
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(git1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");

        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");

        MaterialRevisions given = u.mrs(
                u.mr(git1, true, "g12"),
                u.mr(p2, false, p2_1));

        MaterialRevisions expected = u.mrs(
                u.mr(git1, false, "g11"),
                u.mr(p2, false, p2_1));

        MaterialRevisions actual = getRevisionsBasedOnDependencies(p3, cruiseConfig, given);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldResolveWhenAChainOfDependencyMaterialConstituteASharedMaterial_withAnUnrelatedMaterial() {
        //  hg---> P0 ---+
        //               |
        //   +--------> P1 ------+
        //   |                   |
        //   |                   v
        //  git                 P2
        //   |                   |
        //   +--------> P3 <-----+

        HgMaterial hg = u.wf(new HgMaterial("hg1", null), "folder1");
        String[] hg_revs = {"h11", "h12"};
        int i = 1;
        u.checkinInOrder(hg, u.d(i++), hg_revs);

        GitMaterial git = u.wf(new GitMaterial("git1"), "folder2");
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p0 = u.saveConfigWith("p0", u.m(hg));
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git), u.m(p0));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(git), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p0_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p0, u.d(i++), "h11");
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11", p0_1);
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);

        String p0_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p0, u.d(i++), "h12");
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12", p0_2);
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        MaterialRevisions given = u.mrs(
                u.mr(git, true, "g12"),
                u.mr(p2, false, p2_2));

        MaterialRevisions expected = u.mrs(
                u.mr(git, false, "g12"),
                u.mr(p2, false, p2_2));

        MaterialRevisions actual = getRevisionsBasedOnDependencies(p3, cruiseConfig, given);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldAddAllSharedMaterialsDeeperInTheGraph() {
        GitMaterial git = u.wf(new GitMaterial("git1"), "folder1");
        int i = 0;
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p3));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("p6", u.m(p4));
        ScheduleTestUtil.AddedPipeline p7 = u.saveConfigWith("p7", u.m(p5), u.m(p6));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11");
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1);
        String p4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), p2_1);
        String p5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), p3_1);
        String p6_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p6, u.d(i++), p4_1);

        MaterialRevisions given = u.mrs(
                u.mr(p5, true, p5_1),
                u.mr(p6, true, p6_1));

        MaterialRevisions expected = u.mrs(
                u.mr(p5, true, p5_1),
                u.mr(p6, true, p6_1));

        assertThat(getRevisionsBasedOnDependencies(p7, cruiseConfig, given), is(expected));

        String p1_2 = u.runAndPass(p1, "g12");
        String p3_2 = u.runAndPass(p3, p1_2);
        String p5_2 = u.runAndPass(p5, p3_2);

        given = u.mrs(
                u.mr(p5, true, p5_2),
                u.mr(p6, true, p6_1));

        assertThat(getRevisionsBasedOnDependencies(p7, cruiseConfig, given), is(expected));
    }

    @Test
    public void shouldAddAllSharedMaterialsDeeperInTheGraph_whenCurrentPipelineHasDirectDependencyOnScmMaterial() {
        GitMaterial git = u.wf(new GitMaterial("git1"), "folder1");
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git, git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p3));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(git), u.m(p4));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPass(p1, "g11");
        String p2_1 = u.runAndPass(p2, p1_1);
        String p3_1 = u.runAndPass(p3, p2_1);
        String p4_1 = u.runAndPass(p4, p3_1);

        MaterialRevisions given = u.mrs(
                u.mr(git, true, "g11"),
                u.mr(p4, true, p4_1));

        MaterialRevisions expected = u.mrs(
                u.mr(git, true, "g11"),
                u.mr(p4, true, p4_1));

        assertThat(getRevisionsBasedOnDependencies(p5, cruiseConfig, given), is(expected));

        String p1_2 = u.runAndPass(p1, "g12");
        String p2_2 = u.runAndPass(p2, p1_2);
        String p3_2 = u.runAndPass(p3, p2_2);

        given = u.mrs(
                u.mr(git, true, "g12"),
                u.mr(p4, false, p4_1));

        expected = u.mrs(
                u.mr(git, false, "g11"),
                u.mr(p4, false, p4_1));
        assertThat(getRevisionsBasedOnDependencies(p5, cruiseConfig, given), is(expected));
    }

    @Test
    public void shouldResolveDependenciesAcrossSeveralLevelsOfLinearRelations() {
        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p3));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("P5", u.m(git), u.m(p4));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, p1_1);
        String p3_1 = u.runAndPass(p3, p2_1);
        String p4_1 = u.runAndPass(p4, p3_1);

        String p1_2 = u.runAndPass(p1, "g2");
        String p2_2 = u.runAndPass(p2, p1_2);

        MaterialRevisions given = u.mrs(u.mr(git, true, git_revs),
                u.mr(p4, true, p4_1));

        MaterialRevisions expected = u.mrs(u.mr(git, true, "g1"),
                u.mr(p4, true, p4_1));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("p5"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldResolveDependenciesAcrossSeveralLevelsOfLinearRelations_withNamesThatManifestDirtyGlobalStorageProblem() {
        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p3));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(git), u.m(p4));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, p1_1);
        String p3_1 = u.runAndPass(p3, p2_1);
        String p4_1 = u.runAndPass(p4, p3_1);

        String p1_2 = u.runAndPass(p1, "g2");
        String p2_2 = u.runAndPass(p2, p1_2);

        MaterialRevisions given = u.mrs(u.mr(git, true, git_revs),
                u.mr(p4, true, p4_1));

        MaterialRevisions expected = u.mrs(u.mr(git, true, "g1"),
                u.mr(p4, true, p4_1));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("p5"));
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldResolveSimpleDiamondWithMultipleScmRevisionsToBeRun() {
        /*
             +---> P1 ---+
             |           v
            git         P2
             |          v
             +--------> P3
         */
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12", "g13"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2), u.m(git1));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p2_1, "g11");

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g13");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_3);

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(git1, true, "g13"));

        MaterialRevisions expected = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(git1, true, "g12", "g13"));
        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(expected));
    }

    @Test
    public void shouldResolveSimpleDiamondWithMultipleScmRevisionsThatHasTriggeredTheUpstreamPipeline() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12", "g13"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p2), u.m(git1));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p2_1, "g11");

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionObjects(p1, u.d(i++), u.rs("g12", "g13"));
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(git1, true, "g12", "g13"));

        MaterialRevisions expected = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(git1, true, "g12", "g13"));
        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(expected));
    }


    @Test
    public void shouldOutputCorrectScmRevisionsForBuildCausePopup() {
        int i = 1;
        GitMaterial git = u.wf(new GitMaterial("git"), "folder");
        String[] git_revs1 = {"g11", "g12", "g13", "g14", "g15"};
        u.checkinInOrder(git, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git), u.m(p1));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g12", p1_2);

        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g13");
        String p1_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g14");


        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_4),
                u.mr(git, true, "g15"));

        MaterialRevisions expected = u.mrs(
                u.mr(p1, true, p1_4),
                u.mr(git, true, "g13", "g14"));
        assertThat(getRevisionsBasedOnDependencies(p2, cruiseConfig, given), is(expected));
    }


    @Test
    public void shouldTwistFaninBehaviourWithDifferentConfiguration() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder1");
        String[] git_revs1 = {"g11",};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder2");
        String[] git_revs2 = {"g21", "g22"};
        u.checkinInOrder(git2, u.d(i++), git_revs2);

        ScheduleTestUtil.AddedPipeline c1 = u.saveConfigWith("c1", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline c2 = u.saveConfigWith("c2", u.m(git1));
        ScheduleTestUtil.AddedPipeline c3 = u.saveConfigWith("c3", u.m(git1), u.m(c1), u.m(c2));
        ScheduleTestUtil.AddedPipeline c4 = u.saveConfigWith("c4", u.m(c1), u.m(c2), u.m(c3));
        ScheduleTestUtil.AddedPipeline c5 = u.saveConfigWith("c5", u.m(git2));
        ScheduleTestUtil.AddedPipeline c6 = u.saveConfigWith("c6", u.m(git1), u.m(c1), u.m(c4), u.m(c5));

        CruiseConfig cruiseConfig = goConfigDao.load();


        String c1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c1, u.d(i++), "g11", "g21");
        String c2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c2, u.d(i++), "g11");
        String c3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c3, u.d(i++), "g11", c1_1, c2_1);
        String c4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c4, u.d(i++), c1_1, c2_1, c3_1);
        String c5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c5, u.d(i++), "g21");
        String c6_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c6, u.d(i++), "g11", c1_1, c4_1, c5_1);

        String c1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c1, u.d(i++), "g11", "g22");
        String c5_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c5, u.d(i++), "g22");
        String c4_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(c4, u.d(i++), c1_2, c2_1, c3_1);

        MaterialRevisions given = u.mrs(
                u.mr(git1, false, "g11"),
                u.mr(c1, true, c1_2),
                u.mr(c4, true, c4_2),
                u.mr(c5, true, c5_2));

        MaterialRevisions expected = u.mrs(
                u.mr(git1, false, "g11"),
                u.mr(c1, true, c1_1),
                u.mr(c4, true, c4_1),
                u.mr(c5, true, c5_1));

        MaterialRevisions actual = getRevisionsBasedOnDependencies(c6, cruiseConfig, given);
        assertThat(actual, is(expected));
    }

    @Test
    public void shouldResolveDependenciesWhen_PeggedRevisionOfAncestorPipelineIsNotMaintained() {
        // g3 +   g5
        //  |  \  |
        //  v   V v
        // P5 -> P4 -> P8 -> P11
        //  ^   ^
        //  |  /
        // g4 +

        GitMaterial git3 = new GitMaterial("/tmp/git-repo3", null, "git3");
        u.checkinInOrder(git3, "g31");

        GitMaterial git4 = new GitMaterial("/tmp/git-repo4", null, "git4");
        u.checkinInOrder(git4, "g41");

        GitMaterial git5 = new GitMaterial("/tmp/git-repo5", null, "git5");
        u.checkinInOrder(git5, "g51");

        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", StageConfig.DEFAULT_NAME, u.m(git3), u.m(git4));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", StageConfig.DEFAULT_NAME, u.m(p5), u.m(git3), u.m(git4), u.m(git5));
        ScheduleTestUtil.AddedPipeline p8 = u.saveConfigWith("p8", StageConfig.DEFAULT_NAME, u.m(p4));
        ScheduleTestUtil.AddedPipeline p11 = u.saveConfigWith("p11", StageConfig.DEFAULT_NAME, u.m(p8));

        String p5_1 = u.runAndPass(p5, "g31", "g41");
        String p4_1 = u.runAndPass(p4, p5_1, "g31", "g41", "g51");

        String p8_1 = u.runAndPass(p8, p4_1);

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p8, true, p8_1)});

        assertThat(getRevisionsBasedOnDependencies(goConfigDao.load(), given, new CaseInsensitiveString("p11")), is(given));
    }

    @Test
    public void shouldNotTriggerPipelineWhenOnlyValidChangesAreIgnoredFiles() throws Exception {
        //      p1  <- SVN
        //       |    /
        //       v   v
        //        p2 <- git (BL)

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        u.checkinInOrder(svn, "s1", "s2");

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        git.setFilter(new Filter(new IgnoredFiles("ignored.txt")));
        u.checkinInOrder(git, "g1");
        u.checkinFile(git, "g2", new File("ignored.txt"), ModifiedAction.modified);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(svn), u.m(git));

        String p1_1 = u.runAndPass(p1, "s1");

        String p2_1 = u.runAndPass(p2, p1_1, "s1", "g1");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s2"),
                u.mr(git, true, "g2"),
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1"),
                u.mr(git, true, "g1"),
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), systemEnvironment, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, false, previousRevisions);
        assertThat(buildCause, is(nullValue()));
    }

    @Test
    public void shouldTriggerPipelineWhenThereAreNoNewChangesButMaterialIsRemoved() throws Exception {
        //      p1  <- SVN
        //       |    /
        //       v   v
        //        p2 <- git

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        u.checkinInOrder(svn, "s1", "s2");

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        u.checkinInOrder(git, "g1", "g2");


        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(svn), u.m(git));

        String p1_1 = u.runAndPass(p1, "s1");
        String p2_1 = u.runAndPass(p2, p1_1, "s1", "g1");

        configHelper.setMaterialConfigForPipeline("p2", u.m(p1).materialConfig(), u.m(svn).materialConfig());

        p2 = new ScheduleTestUtil.AddedPipeline(goConfigService.currentCruiseConfig().pipelineConfigByName(new CaseInsensitiveString("p2")), p2.material);

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1")
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1")
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), systemEnvironment, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, true, previousRevisions);
        assertThat(buildCause, is(notNullValue()));
        assertThat(buildCause.getMaterialRevisions(), is(given));
    }

    @Test
    public void shouldNotTriggerPipelineWhenItHasAlreadyRunWithPeggedRevisions() throws Exception {
        //      p1  <- SVN
        //       |    /
        //       v   v
        //        p2 <- git

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        u.checkinInOrder(svn, "s1", "s2");

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        u.checkinFile(git, "g1", new File("some_file.txt"), ModifiedAction.modified);
        u.checkinFile(git, "g2", new File("some_new_file.txt"), ModifiedAction.modified);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(svn), u.m(git));

        String p1_1 = u.runAndPass(p1, "s1");

        String p2_1 = u.runAndPass(p2, p1_1, "s1", "g1");
        String p2_2 = u.runAndPass(p2, p1_1, "s1", "g2");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s2"),
                u.mr(git, true, "g2"),
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1"),
                u.mr(git, true, "g1"),
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), systemEnvironment, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, false, previousRevisions);
        assertThat(buildCause, is(nullValue()));
    }

    @Test
    public void fanInBehaviourShouldBeCorrectWhenStagesHaveDifferentNamesInGraph() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", "stage1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", "stage2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", "stage3", u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", "stage4", u.m(p2), u.m(p3));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1);

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1));

        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(given));

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        given = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(p3, false, p3_1));

        MaterialRevisions expected = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1));
        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(expected));
    }

    @Test
    public void fanInBehaviourShouldBeCorrectWhenStagesHaveBeenRenamedInHistory() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", "stage1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", "stage2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", "stage3", u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", "stage4", u.m(p2), u.m(p3));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1);

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1));

        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(given));

        p1.config.add(0, StageConfigMother.manualStage("renamed_stage"));
        configHelper.writeConfigFile(cruiseConfig);
        cruiseConfig = goConfigDao.load();

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        given = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(p3, false, p3_1));

        MaterialRevisions expected = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1));
        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(expected));
    }

    @Test
    public void shouldGetOnlyLatestScmRevisionOnFirstRun() {
        GitMaterial gitMaterial = new GitMaterial("git-url", null, "dest-folder");
        u.checkinInOrder(gitMaterial, "git-1", "git-2", "git-3");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(gitMaterial));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(gitMaterial));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(gitMaterial), u.m(p1), u.m(p2));

        String p1_1 = u.runAndPass(p1, "git-3");
        String p2_1 = u.runAndPass(p2, "git-3");


        MaterialRevisions given = u.mrs(
                u.mr(gitMaterial, true, "git-3"),
                u.mr(p1, false, p1_1),
                u.mr(p2, false, p2_1));


        MaterialRevisions expected = u.mrs(
                u.mr(gitMaterial, true, "git-3"),
                u.mr(p1, false, p1_1),
                u.mr(p2, false, p2_1));

        assertThat(getRevisionsBasedOnDependencies(p3, goConfigDao.load(), given), is(expected));
    }

    /* TRIANGLE TEST BEGIN */
    @Test
    public void shouldResolveTriangleDependency() throws Exception {
        GitMaterial git = new GitMaterial("git");
        String[] git_revs = {"g1"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(git));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p4));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("p6", u.m(p4), u.m(p5));

        String p4_1 = u.runAndPass(p4, "g1");
        String p4_2 = u.runAndPass(p4, "g1");
        String p5_1 = u.runAndPass(p5, p4_2);
        String p4_3 = u.runAndPass(p4, "g1");
        String p4_4 = u.runAndPass(p4, "g1");

        PipelineConfigDependencyGraph graph = new PipelineConfigDependencyGraph(p6.config, new PipelineConfigDependencyGraph(p5.config), new PipelineConfigDependencyGraph(p4.config));

        MaterialRevisions given = u.mrs(u.mr(p4, true, p4_4), u.mr(p5, true, p5_1));
        MaterialRevisions expected = u.mrs(u.mr(p4, true, p4_2), u.mr(p5, true, p5_1));

        assertThat(pipelineService.getRevisionsBasedOnDependencies(graph, given), is(expected));
    }

    @Test
    public void shouldResolveTriangleDependencyViaAutoBuild() throws Exception {
        SystemEnvironment env = mock(SystemEnvironment.class);
        when(env.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);

        int i = 0;
        GitMaterial git = new GitMaterial("git");
        String[] git_revs = {"g1"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(git));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p4));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("p6", u.m(p4), u.m(p5));

        String p4_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "g1");
        String p4_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "g1");

        String p5_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p5, u.d(i++), p4_2);

        String p4_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i++), "g1");
        String p4_4 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p4, u.d(i), "g1");

        MaterialRevisions given = u.mrs(
                u.mr(p4, true, p4_4),
                u.mr(p5, true, p5_1));
        MaterialRevisions expected = u.mrs(
                u.mr(p4, false, p4_2),
                u.mr(p5, false, p5_1));

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p6.config.name().toString(), env, materialChecker, serverHealthService);
        pipelineTimeline.update();

        BuildCause buildCause = autoBuild.onModifications(given, false, null);

        assertThat(buildCause.getMaterialRevisions(), is(expected));
    }

    @Test
    public void shouldNotTriggerPipelineWhenItHasAlreadyRunWithPeggedRevisions_WithFanInOff() throws Exception {
        //      p1  <- SVN
        //       |    /
        //       v   v
        //        p2 <- git

        SystemEnvironment env = mock(SystemEnvironment.class);
        when(env.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);


        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        u.checkinInOrder(svn, "s1", "s2");

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        u.checkinFile(git, "g1", new File("some_file.txt"), ModifiedAction.modified);
        u.checkinFile(git, "g2", new File("some_new_file.txt"), ModifiedAction.modified);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(svn), u.m(git));

        String p1_1 = u.runAndPass(p1, "s1");

        String p2_1 = u.runAndPass(p2, p1_1, "s1", "g1");
        String p2_2 = u.runAndPass(p2, p1_1, "s1", "g2");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s2"),
                u.mr(git, true, "g2"),
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, false, "s1"),
                u.mr(git, false, "g2"),
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), env, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, false, previousRevisions);
        assertThat(buildCause, is(nullValue()));
    }

    @Test
    public void shouldNotTriggerPipelineWhenOnlyValidChangesAreIgnoredFiles_WhenFaninOff() throws Exception {
        //      p1  <- SVN
        //       |    /
        //       v   v
        //        p2 <- git (BL)

        SystemEnvironment env = mock(SystemEnvironment.class);
        when(env.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        u.checkinInOrder(svn, "s1", "s2");

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        git.setFilter(new Filter(new IgnoredFiles("ignored.txt")));
        u.checkinInOrder(git, "g1");
        u.checkinFile(git, "g2", new File("ignored.txt"), ModifiedAction.modified);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(svn), u.m(git));

        String p1_1 = u.runAndPass(p1, "s1");

        String p2_1 = u.runAndPass(p2, p1_1, "s1", "g1");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s2"),
                u.mr(git, true, "g2"),
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1"),
                u.mr(git, true, "g1"),
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), env, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, false, previousRevisions);
        assertThat(buildCause, is(nullValue()));
    }

    @Test
    public void shouldTriggerPipelineWhenThereAreNoNewChangesButMaterialIsRemoved_WithFanInOff() throws Exception {
        //      p1  <- SVN
        //       |    /
        //       v   v
        //        p2 <- git

        SystemEnvironment env = mock(SystemEnvironment.class);
        when(env.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);

        SvnMaterial svn = u.wf(new SvnMaterial("url", "username", "password", false), "folder1");
        u.checkinInOrder(svn, "s1", "s2");

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        u.checkinInOrder(git, "g1", "g2");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(svn));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(svn), u.m(git));

        String p1_1 = u.runAndPass(p1, "s1");
        String p2_1 = u.runAndPass(p2, p1_1, "s1", "g1");

        configHelper.setMaterialConfigForPipeline("p2", u.m(p1).materialConfig(), u.m(svn).materialConfig());

        p2 = new ScheduleTestUtil.AddedPipeline(goConfigService.currentCruiseConfig().pipelineConfigByName(new CaseInsensitiveString("p2")), p2.material);

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1")
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(svn, true, "s1")
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), env, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, true, previousRevisions);
        assertThat(buildCause, is(notNullValue()));
        assertThat(buildCause.getMaterialRevisions(), is(given));
    }

    @Test
    public void shouldTriggerPipelineWhenThereAreOnlyIgnoredChanges_WithFanInOff() throws Exception {
        //      p1  <- git (BL)
        //       |    /
        //       v   v
        //        p2

        SystemEnvironment env = mock(SystemEnvironment.class);
        when(env.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        git.setFilter(new Filter(new IgnoredFiles("ignored.txt")));
        u.checkinInOrder(git, "g1");
        u.checkinFile(git, "g2", new File("ignored.txt"), ModifiedAction.modified);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1), u.m(git));

        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, p1_1, "g1");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(git, true, "g2")
        });
        MaterialRevisions previousRevisions = u.mrs(new MaterialRevision[]{
                u.mr(p1, true, p1_1),
                u.mr(git, true, "g1")
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), env, materialChecker, serverHealthService);
        pipelineTimeline.update();
        BuildCause buildCause = autoBuild.onModifications(given, true, previousRevisions);
        assertThat(buildCause, is(notNullValue()));
        assertThat(buildCause.getMaterialRevisions(), is(given));
    }
    /* TRIANGLE TEST END */

    @Ignore("FanInOffBehavior.scn")
    @Test
    public void shouldTriggerAutoBuildWithOriginalMaterialRevisionsWhenFaninIsTurnedOff_NoDiamondsNoTriangles() {
        //  git1 -> P1
        //           |
        //           V
        //  git2 -> P2

        SystemEnvironment env = mock(SystemEnvironment.class);
        when(env.enforceRevisionCompatibilityWithUpstream()).thenReturn(false);

        GitMaterial git1 = u.wf(new GitMaterial("git"), "folder");
        GitMaterial git2 = u.wf(new GitMaterial("git"), "folder");
        u.checkinFile(git1, "g1_1", new File("some_file.txt"), ModifiedAction.modified);
        u.checkinFile(git1, "g1_2", new File("some_file.txt"), ModifiedAction.modified);
        u.checkinFile(git2, "g2_1", new File("some_file.txt"), ModifiedAction.modified);
        u.checkinFile(git2, "g2_2", new File("some_file.txt"), ModifiedAction.modified);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1), u.m(git2));

        String p1_1 = u.runAndPass(p1, "g1_1");
        String p2_1 = u.runAndPass(p2, p1_1, "g2_1");
        String p1_2 = u.runAndPass(p1, "g1_2");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_2),
                u.mr(git2, true, "g2_2")
        });

        MaterialRevisions previous = u.mrs(new MaterialRevision[]{
                u.mr(p1, false, p1_1),
                u.mr(git2, true, "g2_1")
        });

        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, p2.config.name().toString(), env, materialChecker, serverHealthService);
        pipelineTimeline.update();

        BuildCause buildCause = autoBuild.onModifications(given, false, previous);
        assertThat(buildCause, is(notNullValue()));
        assertThat(buildCause.getMaterialRevisions(), is(given));
    }

    @Test
    public void shouldResolveCorrectlyWhenAStageInvolvedInDependencyHasFailed() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git1_revs = {"g11", "g12"};
        u.checkinInOrder(git1, u.d(i++), git1_revs);

        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder-git2");
        String[] git2_revs = {"g21", "g22"};
        u.checkinInOrder(git2, u.d(i++), git2_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(git2));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p2), u.m(p3), u.m(p4));
        CruiseConfig cruiseConfig = goConfigDao.load();


        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1);
        Pipeline p4_1 = u.runAndPassAndReturnPipelineInstance(p4, u.d(i++), "g21");
        String p4_1_2 = u.rerunStageAndCancel(p4_1, p4.config.getFirstStageConfig());

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, true, p3_1),
                u.mr(p4, true, p4_1.getStages().get(0).getIdentifier().getStageLocator()));

        assertThat(getRevisionsBasedOnDependencies(p5, cruiseConfig, given), is(given));

    }

    @Test
    @ExpectedException(MaxBackTrackLimitReachedException.class)
    public void shouldResolveSimpleDiamondAndThrowLimitException() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(p1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p2), u.m(p3));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1);
        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1);

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_2);

        MaterialRevisions given = u.mrs(
                u.mr(p2, true, p2_2),
                u.mr(p3, false, p3_1));

        MaterialRevisions expected = u.mrs(
                u.mr(p2, true, p2_1),
                u.mr(p3, false, p3_1));
        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(expected));

        systemEnvironment.set(SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT, 1);

        getRevisionsBasedOnDependencies(p4, cruiseConfig, given);
    }

    @Test(timeout = 10 * 1000)
    @ExpectedException(NoCompatibleUpstreamRevisionsException.class)
    public void shouldContinueBackTrackingFromItsLastKnownPositionAndNotFromTheBeginning() {
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder1");
        String[] git_revs1 = {"g11", "g12", "g13"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder2");
        String[] git_revs2 = {"g21", "g22", "g23"};
        u.checkinInOrder(git2, u.d(i++), git_revs2);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11", "g21");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g12", "g21");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11", "g23");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_2));

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(given));
    }
}
