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
import com.thoughtworks.go.config.materials.*;
import com.thoughtworks.go.config.materials.dependency.DependencyMaterial;
import com.thoughtworks.go.config.materials.git.GitMaterial;
import com.thoughtworks.go.config.materials.mercurial.HgMaterial;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.MaterialRevision;
import com.thoughtworks.go.domain.MaterialRevisions;
import com.thoughtworks.go.domain.Pipeline;
import com.thoughtworks.go.domain.buildcause.BuildCause;
import com.thoughtworks.go.domain.materials.ModifiedAction;
import com.thoughtworks.go.helper.MaterialsMother;
import com.thoughtworks.go.server.cache.GoCache;
import com.thoughtworks.go.server.dao.DatabaseAccessHelper;
import com.thoughtworks.go.server.domain.PipelineTimeline;
import com.thoughtworks.go.server.materials.MaterialChecker;
import com.thoughtworks.go.server.persistence.MaterialRepository;
import com.thoughtworks.go.server.transaction.TransactionTemplate;
import com.thoughtworks.go.serverhealth.ServerHealthService;
import com.thoughtworks.go.util.GoConfigFileHelper;
import com.thoughtworks.go.util.SystemEnvironment;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.File;

import static com.thoughtworks.go.util.SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath:WEB-INF/applicationContext-global.xml",
        "classpath:WEB-INF/applicationContext-dataLocalAccess.xml",
        "classpath:WEB-INF/applicationContext-acegi-security.xml"
})
public class FaninDependencyResolutionTest {
    public static final String STAGE_NAME = "s";
    public static final Cloner CLONER = new Cloner();
    @Autowired
    private DatabaseAccessHelper dbHelper;
    @Autowired
    private GoCache goCache;
    @Autowired
    private GoConfigDao goConfigDao;
    @Autowired
    private PipelineService pipelineService;
    @Autowired
    private MaterialRepository materialRepository;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private GoConfigService goConfigService;
    @Autowired
    private SystemEnvironment systemEnvironment;
    @Autowired
    private MaterialChecker materialChecker;
    @Autowired
    private PipelineTimeline pipelineTimeline;
    @Autowired
    private ServerHealthService serverHealthService;

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
        systemEnvironment.reset(RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT);
        dbHelper.onTearDown();
        configHelper.onTearDown();
    }

    private void setMaxBackTrackLimit(int limit) {
        systemEnvironment.set(RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT, limit);
    }

    private Integer maxBackTrackLimit() {
        return systemEnvironment.get(SystemEnvironment.RESOLVE_FANIN_MAX_BACK_TRACK_LIMIT);
    }

    @Test
    public void shouldRestoreMaterialNamesBasedOnMaterialConfig() throws Exception {
        /*
            g -> up   -> down
                 +-> mid -+
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        u.checkinInOrder(git, "g1");

        ScheduleTestUtil.AddedPipeline up = u.saveConfigWith("up", u.m(git));
        ScheduleTestUtil.MaterialDeclaration upForMid = u.m(up);
        ((DependencyMaterial) upForMid.material).setName(new CaseInsensitiveString("up-for-mid"));
        ScheduleTestUtil.AddedPipeline mid = u.saveConfigWith("mid", upForMid);
        ScheduleTestUtil.MaterialDeclaration upForDown = u.m(up);
        ((DependencyMaterial) upForDown.material).setName(new CaseInsensitiveString("up-for-down"));
        ScheduleTestUtil.AddedPipeline down = u.saveConfigWith("down", u.m(mid), upForDown);
        CruiseConfig cruiseConfig = goConfigDao.load();

        String up_1 = u.runAndPass(up, "g1");
        String mid_1 = u.runAndPass(mid, up_1);
        String down_1 = u.runAndPass(down, mid_1, up_1);

        MaterialRevisions given = u.mrs(
                u.mr(mid, false, mid_1),
                u.mr(up, false, up_1));

        MaterialRevisions revisionsBasedOnDependencies = getRevisionsBasedOnDependencies(down, cruiseConfig, given);

        for (MaterialRevision revisionsBasedOnDependency : revisionsBasedOnDependencies) {
            DependencyMaterial dependencyPipeline = (DependencyMaterial) revisionsBasedOnDependency.getMaterial();
            if (dependencyPipeline.getPipelineName().equals(new CaseInsensitiveString("up"))) {
                assertThat(dependencyPipeline.getName(), is(new CaseInsensitiveString("up-for-down")));
            }
        }
        assertThat(revisionsBasedOnDependencies, is(given));
    }

    @Test
    public void shouldTriggerCommonCIAndCDPhasesCorrectly_FAILING_SCN() throws Exception {

        //     -----> Acceptance ----
        //     |        |           v
        //  Build       |          Staging ---> Production
        //     |        v           ^
        //     ----->Regression ----
        //
        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        u.checkinInOrder(git, "g1", "g2", "g3");

        ScheduleTestUtil.AddedPipeline build = u.saveConfigWith("build", u.m(git));
        ScheduleTestUtil.AddedPipeline acceptance = u.saveConfigWith("acceptance", u.m(build));
        ScheduleTestUtil.AddedPipeline regression = u.saveConfigWith("regression", u.m(build), u.m(acceptance));
        ScheduleTestUtil.AddedPipeline staging = u.saveConfigWith("staging", u.m(acceptance), u.m(regression));
        ScheduleTestUtil.AddedPipeline production = u.saveConfigWith("production", u.m(staging));
        CruiseConfig cruiseConfig = goConfigDao.load();

        int i = 1;

        String b_1 = u.runAndPass(build, "g1");

        String a_1 = u.runAndPass(acceptance, b_1);
        String r_1 = u.runAndPass(regression, b_1, a_1);
        String s_1 = u.runAndPass(staging, a_1, r_1);
        String p_1 = u.runAndPass(production, s_1);

        String b_2 = u.runAndPass(build, "g2");

        MaterialRevisions given = u.mrs(
                u.mr(build, true, b_2),
                u.mr(acceptance, false, a_1));
        MaterialRevisions expected = u.mrs(
                u.mr(build, true, b_1),
                u.mr(acceptance, false, a_1));
        assertThat(getRevisionsBasedOnDependencies(regression, cruiseConfig, given), is(expected));

        String a_2 = u.runAndPass(acceptance, b_2);

        given = u.mrs(
                u.mr(build, true, b_2),
                u.mr(acceptance, false, a_2));
        expected = u.mrs(
                u.mr(build, true, b_2),
                u.mr(acceptance, true, a_2));
        assertThat(getRevisionsBasedOnDependencies(regression, cruiseConfig, given), is(expected));

        String r_2 = u.runAndPass(regression, b_2, a_2);
        String r_3 = u.runAndPass(regression, b_1, a_2);

        given = u.mrs(
                u.mr(acceptance, true, a_2),
                u.mr(regression, true, r_3));
        expected = u.mrs(
                u.mr(acceptance, true, a_2),
                u.mr(regression, true, r_2));
        assertThat(getRevisionsBasedOnDependencies(staging, cruiseConfig, given), is(expected));

        String s_2 = u.runAndPass(staging, a_2, r_2);
        String s_3 = u.runAndPass(staging, a_2, r_3);

        given = u.mrs(u.mr(staging, true, s_3));
        expected = u.mrs(u.mr(staging, true, s_2));

        assertThat(getRevisionsBasedOnDependencies(production, cruiseConfig, given), is(expected));

        String b_3 = u.runAndPass(build, "g3");
        String a_3 = u.runAndPass(acceptance, b_3);
        String r_4 = u.runAndPass(regression, b_3, a_3);
        String s_4 = u.runAndPass(staging, a_3, r_1);
        String s_5 = u.runAndPass(staging, a_1, r_4);

        given = u.mrs(
                u.mr(acceptance, true, a_3),
                u.mr(regression, true, r_4));
        expected = u.mrs(
                u.mr(acceptance, true, a_3),
                u.mr(regression, true, r_4));
        MaterialRevisions previousMaterialRevisions = u.mrs(
                u.mr(acceptance, false, a_1),
                u.mr(regression, false, r_4));
        assertThat(getRevisionsBasedOnDependencies(staging, cruiseConfig, given), is(expected));
//        assertThat(getBuildCause(staging,given,previousMaterialRevisions), is(not(nullValue()))); //TODO: *************** Bug where pipeline should be triggered <Sara>

        String r_5 = u.runAndPass(regression, b_3, a_3);

        given = u.mrs(
                u.mr(acceptance, true, a_3),
                u.mr(regression, true, r_5));
        expected = u.mrs(
                u.mr(acceptance, true, a_3),
                u.mr(regression, true, r_5));
        assertThat(getRevisionsBasedOnDependencies(staging, cruiseConfig, given), is(expected));
        previousMaterialRevisions = u.mrs(
                u.mr(acceptance, false, a_1),
                u.mr(regression, false, r_4));
        assertThat(getBuildCause(staging, given, previousMaterialRevisions).getMaterialRevisions(), is(expected));
    }

    @Ignore("Fails with FaninGraph")
    @Test
    public void addingNewMaterialsShouldTriggerAsExpected() throws Exception {

        //     -----> Acceptance ----
        //     |        |           v
        //  Build       |          Staging ---> Production
        //     |        v           ^
        //     ----->Regression ----
        //

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        u.checkinInOrder(git, "g1", "g2", "g3");

        ScheduleTestUtil.AddedPipeline build = u.saveConfigWith("build", u.m(git));
        ScheduleTestUtil.AddedPipeline acceptance = u.saveConfigWith("acceptance", u.m(build));
        ScheduleTestUtil.AddedPipeline regression = u.saveConfigWith("regression", u.m(build), u.m(acceptance));
        ScheduleTestUtil.AddedPipeline staging = u.saveConfigWith("staging", u.m(acceptance), u.m(regression));
        ScheduleTestUtil.AddedPipeline production = u.saveConfigWith("production", u.m(staging));
        CruiseConfig cruiseConfig = goConfigDao.load();

        int i = 1;

        String b_1 = u.runAndPass(build, "g1");

        String a_1 = u.runAndPass(acceptance, b_1);
        String r_1 = u.runAndPass(regression, b_1, a_1);
        String s_1 = u.runAndPass(staging, a_1, r_1);
        String p_1 = u.runAndPass(production, s_1);

        // Adding new QA repos to acceptance and regression

        HgMaterial hg = u.wf(new HgMaterial("hgurl", null), "hg_folder");
        u.checkinInOrder(hg, "h1");
        acceptance = u.addMaterialToPipeline(acceptance, u.m(hg));
        cruiseConfig = goConfigDao.load();

        String a_2 = u.runAndPass(acceptance, b_1, "h1");

        MaterialRevisions given = u.mrs(
                u.mr(acceptance, true, a_2),
                u.mr(regression, false, r_1));
        MaterialRevisions expected = u.mrs(
                u.mr(acceptance, true, a_1),
                u.mr(regression, false, r_1));
        assertThat(getRevisionsBasedOnDependencies(staging, cruiseConfig, given), is(expected));

        given = u.mrs(
                u.mr(build, false, b_1),
                u.mr(acceptance, true, a_2));

        assertThat(getRevisionsBasedOnDependencies(regression, cruiseConfig, given), is(given));

        String r_2 = u.runAndPass(regression, b_1, a_2);
        regression = u.addMaterialToPipeline(regression, u.m(hg));
        cruiseConfig = goConfigDao.load();

        given = u.mrs(
                u.mr(acceptance, true, a_2),
                u.mr(regression, false, r_2));

//        assertThat(getRevisionsBasedOnDependencies(staging, cruiseConfig, given, defaultPegger()), is(given)); TODO: Doesnot find this to be comaptible <Sara>


        String r_3 = u.runAndPass(regression, b_1, a_2, "h1");

        given = u.mrs(
                u.mr(acceptance, true, a_2),
                u.mr(regression, false, r_3));
        assertThat(getRevisionsBasedOnDependencies(staging, cruiseConfig, given), is(given));

        String s_2 = u.runAndPass(staging, a_2, r_3);
        given = u.mrs(u.mr(staging, true, s_2));
        assertThat(getRevisionsBasedOnDependencies(production, cruiseConfig, given), is(given));
    }

    @Ignore("Fails with FaninGraph")
    @Test
    public void shouldTriggerInspiteOfMaterialConfigChange_FAILING_SCN() throws Exception {
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder1");
        u.checkinInOrder(git1, "g11", "g12");

        GitMaterial git2 = u.wf(new GitMaterial("git2"), "folder2");
        git2.setFolder("folder");
        u.checkinInOrder(git2, "g21", "g22");

        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(git1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p3));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p4), u.m(git2));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("p6", u.m(p4), u.m(git2));
        ScheduleTestUtil.AddedPipeline p7 = u.saveConfigWith("p7", u.m(p5), u.m(p6));
        CruiseConfig cruiseConfig = goConfigDao.load();

        String p3_1 = u.runAndPass(p3, "g11");
        String p4_1 = u.runAndPass(p4, p3_1);
        String p5_1 = u.runAndPass(p5, p4_1, "g21");
        String p6_1 = u.runAndPass(p6, p4_1, "g21");
        String p7_1 = u.runAndPass(p7, p5_1, p6_1);

        String p3_2 = u.runAndPass(p3, "g11");
        p3 = u.addMaterialToPipeline(p3, u.m(git2));
        cruiseConfig = goConfigDao.load();

        MaterialRevisions given = u.mrs(u.mr(p3, true, p3_2));
        assertThat(getRevisionsBasedOnDependencies(p4, cruiseConfig, given), is(given));
        String p4_2 = u.runAndPass(p4, p3_2);
        String p5_2 = u.runAndPass(p5, p4_2, "g21");
        String p6_2 = u.runAndPass(p6, p4_2, "g21");
        given = u.mrs(
                u.mr(p5, true, p5_2),
                u.mr(p6, true, p6_2));
        MaterialRevisions expected = u.mrs(
                u.mr(p5, true, p5_2),
                u.mr(p6, true, p6_2));


//        assertThat(getRevisionsBasedOnDependencies(p7, cruiseConfig, given), is(expected)); //TODO: ************************** Bug where p7 does not get triggerred <Sara>
    }

    @Test
    public void shouldReturnPreviousBuild_sRevisionsIfOneParentFailed() {
//            git ----------
//             |            |
//        ----- ------      |
//       |           |      |
//       v           v      |
//       P1         P2      |
//        |         |       |
//        |         |       |
//         --->P3<--        |
//             ^____________|

        GitMaterial gitMaterial = new GitMaterial("git-url");
        u.checkinInOrder(gitMaterial, "g1", "g2");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(gitMaterial));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(gitMaterial));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2), u.m(gitMaterial));

        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, "g1");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1, "g1");

        String p1_2 = u.runAndPass(p1, "g2");
        String p2_2 = u.runAndFail(p2, "g2");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(gitMaterial, true, "g2"),
                u.mr(p1, true, p1_2),
                u.mr(p2, true, p2_1)});

        MaterialRevisions expected = u.mrs(new MaterialRevision[]{
                u.mr(gitMaterial, true, "g1"),
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_1)});

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p3, goConfigDao.load(), given);
        assertThat(finalRevisions, is(expected));
    }

    //    @Ignore("Expected behavior with Sriki's Algo ;)")
    @Test
    public void shouldResolveWithMultipleDiamondsOnSamePipelines() throws Exception {
        /*
          |-------v
          |    /--P1--\
          git hg       P5
          |    \--P2--/
          +------^
         */
        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs = {"h11", "h12"};
        u.checkinInOrder(hg, hg_revs);

        GitMaterial git = u.wf(new GitMaterial("git"), "folder2");
        String[] git_revs = {"g11", "g12", "g13"};
        u.checkinInOrder(git, git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(hg), u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(hg), u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2));

        String p1_1 = u.runAndPass(p1, "h12", "g11");
        String p2_1 = u.runAndPass(p2, "h11", "g13");

        String p1_2 = u.runAndPass(p1, "h11", "g13");
        String p2_2 = u.runAndPass(p2, "h12", "g13");

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(p1, true, p1_2),
                u.mr(p2, true, p2_2)});

        MaterialRevisions expected = u.mrs(new MaterialRevision[]{
                u.mr(p1, true, p1_2),
                u.mr(p2, true, p2_1)});

        assertThat(getRevisionsBasedOnDependencies(p3, goConfigDao.load(), given), is(expected));
    }

    @Ignore("Expected behavior with Sriki's Algo ;)")
    @Test
    public void shouldResolveSimpleDiamondWithOlderRevisions() {
        /*
              /-->P1--\
            git        P3
              \-->P2--/
         */
        GitMaterial git1 = new GitMaterial("git1");
        String[] git_revs1 = {"g11", "g12", "g13"};
        u.checkinInOrder(git1, git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2));
        CruiseConfig cruiseConfig = goConfigDao.load();

        int i = 1;
        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11");

        String p3_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p3, u.d(i++), p1_1, p2_1);

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g13");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g13");

        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(p2, true, p2_2));

        MaterialRevisions expected = u.mrs(
                u.mr(p1, true, p1_2),
                u.mr(p2, true, p2_2));

        assertThat(getRevisionsBasedOnDependencies(p3, cruiseConfig, given), is(expected));
    }

    @Ignore("In progress")
    @Test
    public void shouldTriggerDependenciesAppropriatelyWithIgnoredMaterials() throws Exception {
        //        -----> App
        //       /        v
        //      git ---> Web ---> Pkg
        //       \        ^
        //        -----> Bus

        GitMaterial ga = u.wf(new GitMaterial("scm"), "app");
        ga.setFilter(new Filter(new IgnoredFiles("web/*.*"), new IgnoredFiles("bus/*.*")));
        GitMaterial gb = u.wf(new GitMaterial("scm"), "bus");
        gb.setFilter(new Filter(new IgnoredFiles("app/*.*"), new IgnoredFiles("web/*.*")));
        GitMaterial gw = u.wf(new GitMaterial("scm"), "web");
        gw.setFilter(new Filter(new IgnoredFiles("app/*.*"), new IgnoredFiles("bus/*.*")));

        ScheduleTestUtil.AddedPipeline app = u.saveConfigWith("app", u.m(ga));
        ScheduleTestUtil.AddedPipeline bus = u.saveConfigWith("bus", u.m(gb));
        ScheduleTestUtil.AddedPipeline web = u.saveConfigWith("web", u.m(app), u.m(bus), u.m(gw));
        ScheduleTestUtil.AddedPipeline pkg = u.saveConfigWith("pkg", u.m(web));
        CruiseConfig cruiseConfig = goConfigDao.load();
        u.checkinFile(ga, "ga1", new File("app/hi.txt"), ModifiedAction.added);
        u.checkinFile(gb, "gb1", new File("bus/hi.txt"), ModifiedAction.added);

        String a_1 = u.runAndPass(app, "ga1");
        String b_1 = u.runAndPass(bus, "gb1");

        MaterialRevisions given = u.mrs(
                u.mr(app, true, a_1),
                u.mr(bus, true, b_1),
                u.mr(gw, true, "gb1")
        );
        assertThat(getRevisionsBasedOnDependencies(web, cruiseConfig, given), is(given));

    }


    @Test
    public void shouldFindCompatibleRevisionWhenDependencyMaterialHasMaterialName() throws Exception {
        //      Third <- Second
        //         |     /
        //         |   /
        //         Last
        //

        SvnMaterial svn = u.wf(new SvnMaterial("svn", "username", "password", false), "folder1");
        String[] svn_revs = {"s1"};
        u.checkinInOrder(svn, svn_revs);

        ScheduleTestUtil.AddedPipeline second = u.saveConfigWith("second", u.m(svn));
        ScheduleTestUtil.AddedPipeline third = u.saveConfigWith("third", u.m(second, "two"));
        ScheduleTestUtil.AddedPipeline last = u.saveConfigWith("last", u.m(third, "three"), u.m(second, "two_2"));

        String second_1 = u.runAndPass(second, "s1");
        System.out.println(second_1);
        String third_1 = u.runAndPass(third, second_1);

        MaterialRevisions given = u.mrs(
                u.mr(third, true, third_1),
                u.mr(second, true, second_1));

        assertThat(getRevisionsBasedOnDependencies(last, goConfigDao.load(), given), is(given));
    }


    @Test
    public void shouldFindCompatibleRevisionWhenSameMaterialHasDiffFolderNamesInGraph() throws Exception {
        //      Second <- Svn
        //         |     /
        //         |   /
        //         Third
        //

        SvnMaterial svn1 = u.wf(new SvnMaterial("svn", "username", "password", false), "one");
        SvnMaterial svn2 = u.wf(new SvnMaterial("svn", "username", "password", false), "two");
        String[] svn_revs = {"s1"};
        u.checkinInOrder(svn1, svn_revs);

        ScheduleTestUtil.AddedPipeline second = u.saveConfigWith("second", u.m(svn1));
        ScheduleTestUtil.AddedPipeline third = u.saveConfigWith("third", u.m(second), u.m(svn2));

        String second_1 = u.runAndPass(second, "s1");


        MaterialRevisions given = u.mrs(
                u.mr(second, true, second_1),
                u.mr(svn2, true, "s1")
        );

        MaterialRevisions materialRevisions = getRevisionsBasedOnDependencies(third, goConfigDao.load(), given);
        assertThat(materialRevisions, is(given));
    }

    @Test
    public void shouldComputeRevisionCorrectlyWhenUpstreamPipelineHasModifications_ForDifferentStages() throws Exception {
        /*
             /-->P1------\    p2(s1) --> p4
           git           P3
             \-->P2(S2)--/

        */
        GitMaterial git = u.wf(new GitMaterial("git"), "f");
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2_s1 = u.saveConfigWith("p2", "s1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2_s2 = u.addStageToPipeline(p2_s1.config.name(), "s2");
        ScheduleTestUtil.MaterialDeclaration p2_material = u.m(new DependencyMaterial(p2_s1.config.name(), new CaseInsensitiveString("s2")));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", p2_material, u.m(p1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(new DependencyMaterial(p2_s1.config.name(), new CaseInsensitiveString("s1"))));

        u.checkinInOrder(git, "g1");
        String p1_1 = u.runAndPass(p1, "g1");
        String p2_s2_1 = u.runAndPass(p2_s2, "g1");
        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2_s2, true, p2_s2_1));

        MaterialRevisions revisionsBasedOnDependencies = getRevisionsBasedOnDependencies(p3, goConfigDao.load(), given);
        assertThat(revisionsBasedOnDependencies, is(given));
    }

    @Test
    public void shouldPickTheRightRevisionsWhenMaterialIsRemovedAndPutBack() {

        GitMaterial git1 = u.wf(new GitMaterial("git1-url"), "git-folder1");
        GitMaterial git2 = u.wf(new GitMaterial("git2-url"), "git-folder2");
        GitMaterial git3 = u.wf(new GitMaterial("git3-url"), "git-folder3");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git2));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2), u.m(git1));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p3), u.m(git2));

        u.checkinInOrder(git1, "git1_1");
        u.checkinInOrder(git2, "git2_1");


        String p1_1 = u.runAndPass(p1, "git1_1", "git2_1");
        String p2_1 = u.runAndPass(p2, "git2_1");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1, "git1_1");
        String p4_1 = u.runAndPass(p4, p3_1, "git2_1");

        u.checkinInOrder(git1, "git1_2");
        u.checkinInOrder(git2, "git2_2");

        String p1_2 = u.runAndPass(p1, "git1_2", "git2_2");
        String p2_2 = u.runAndPass(p2, "git2_2");
        String p3_2 = u.runAndPass(p3, p1_2, p2_2, "git1_2");
        String p4_2 = u.runAndPass(p4, p3_2, "git2_2");

        configHelper.setMaterialConfigForPipeline("P2", git3.config());
        CruiseConfig cruiseConfig = goConfigDao.load();
        p2 = new ScheduleTestUtil.AddedPipeline(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P2")), p2.material);

        u.checkinInOrder(git1, "git1_3");
        u.checkinInOrder(git2, "git2_3");
        u.checkinInOrder(git3, "git3_1");
        String p1_3 = u.runAndPass(p1, "git1_3", "git2_3");
        String p2_3 = u.runAndPass(p2, "git3_1");
        String p3_3 = u.runAndPass(p3, p1_3, p2_3, "git1_3");

        //check wat happens to p4

        MaterialRevisions given = u.mrs(new MaterialRevision[]{
                u.mr(git2, true, "git2_3"),
                u.mr(p3, true, p3_3)});

        MaterialRevisions expected = u.mrs(new MaterialRevision[]{
                u.mr(git2, true, "git2_3"),
                u.mr(p3, true, p3_3)});

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p4, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));

        //bring back git2 in p2

        configHelper.setMaterialConfigForPipeline("P2", git2.config());
        cruiseConfig = goConfigDao.load();
        p2 = new ScheduleTestUtil.AddedPipeline(cruiseConfig.pipelineConfigByName(new CaseInsensitiveString("P2")), p2.material);

        //check wat happend to p4

        given = u.mrs(
                u.mr(git2, true, "git2_3"),
                u.mr(p3, true, p3_3));

        expected = u.mrs(new MaterialRevision[]{
                u.mr(git2, true, "git2_3"),
                u.mr(p3, true, p3_3)});

        finalRevisions = getRevisionsBasedOnDependencies(p4, cruiseConfig, given);
        assertThat(finalRevisions, is(expected));
    }

    @Test
    public void shouldResolveDifferentCasesInAPipelineGraphSimilarToOneInGo01() {

        GitMaterial git1 = u.wf(new GitMaterial("git1-url"), "git-folder1");
        GitMaterial git2 = u.wf(new GitMaterial("git2-url"), "git-folder2");
        GitMaterial git3 = u.wf(new GitMaterial("git3-url"), "git-folder3");
        GitMaterial git4 = u.wf(new GitMaterial("git4-url"), "git-folder4");

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1), u.m(git1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(git2));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("P4", u.m(p3), u.m(p2), u.m(git3));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("P5", u.m(p2), u.m(git3), u.m(git4));
        ScheduleTestUtil.AddedPipeline p6 = u.saveConfigWith("P6", u.m(p3), u.m(git3), u.m(git4));
        ScheduleTestUtil.AddedPipeline p7 = u.saveConfigWith("P7", u.m(p4), u.m(p5), u.m(p6));
        ScheduleTestUtil.AddedPipeline p8 = u.saveConfigWith("P8", u.m(p7));

    }

    @Test
    public void shouldThrowExceptionWhenStageHasPassedButIsNotPresentInModificationsTable() {
        GitMaterial git = u.wf(new GitMaterial("git1-url"), "git-folder1");
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("P3", u.m(p1), u.m(p2));
        u.checkinInOrder(git, "g1", "g2");
        String p1_1 = u.runAndPass(p1, "g1");
        String p2_1 = u.runAndPass(p2, "g1");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1);
        String p1_2 = u.runAndPass(p1, "g2");
        Pipeline p2_2instance = u.scheduleWith(p2, "g2");
        dbHelper.pass(p2_2instance);
        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_2),
                u.mr(p2, false, p2_1)
        );
        MaterialRevisions previous = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_1)
        );
        try {
            getBuildCause(p3, given, previous);
            fail();
        } catch (NoModificationsPresentForDependentMaterialException exception) {
            assertThat(exception.getMessage(), Matchers.containsString(p2_2instance.getFirstStage().getIdentifier().getStageLocator()));
        }

    }

    @Test
    public void shouldComputeRevisionCorrectlyWhen_MoreThan1UpstreamPipelineHasMinimumRevision() throws Exception {
        /*       +----------
             /-->P1---\      v
           git------> P3 -> P4
             \-->P2--/       ^
                  +----------
        */
        GitMaterial git = u.wf(new GitMaterial("git"), "f");
        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2), u.m(git));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(p1), u.m(p2), u.m(p3));

        int i = 0;
        u.checkinInOrder(git, u.d(i++), "g0");
        String p1_1 = u.runAndPass(p1, "g0");
        String p2_1 = u.runAndPass(p2, "g0");
        String p3_1 = u.runAndPass(p3, p1_1, p2_1, "g0");
        String p4_1 = u.runAndPass(p4, p1_1, p2_1, p3_1);

        u.checkinInOrder(git, u.d(i++), "g1");
        String p1_2 = u.runAndPass(p1, "g1");

        u.checkinInOrder(git, u.d(i++), "g2");
        String p2_2 = u.runAndPass(p2, "g2");
        String p3_2 = u.runAndPass(p3, p1_2, p2_2, "g2");

        u.checkinInOrder(git, u.d(i++), "g3");
        String p1_3 = u.runAndPass(p1, "g3");
        String p2_3 = u.runAndPass(p2, "g3");
        String p3_3 = u.runAndPass(p3, p1_3, p2_3, "g3");
        String p4_2 = u.runAndPass(p4, p1_3, p2_3, p3_3);

        String p2_4 = u.runAndPass(p2, "g1");

        String p3_4 = u.runAndPass(p3, p1_2, p2_4, "g1");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(p2, true, p2_4),
                u.mr(p3, true, p3_4));

        MaterialRevisions expected = u.mrs(
                u.mr(p1, true, p1_2),
                u.mr(p2, true, p2_4),
                u.mr(p3, true, p3_4));

        MaterialRevisions revisionsBasedOnDependencies = getRevisionsBasedOnDependencies(p4, goConfigDao.load(), given);
        assertThat(revisionsBasedOnDependencies, is(expected));
    }

    @Test
    public void shouldResolveWithNoPassedBuildOfRootNode() throws Exception {
        /**
         * git -------+
         *  |         |
         *  |         |
         *  v         v
         *  P1 -----> P2
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1"};
        int i = 0;
        u.checkinInOrder(git, u.d(i++), git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1), u.m(git));

        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1");

        setMaxBackTrackLimit(2);

        for (int j = 1; j <= maxBackTrackLimit() + 1; j++) {
            u.runAndFail(p2, u.d(i++), p1_1, "g1");
        }

        MaterialRevisions given = u.mrs(u.mr(p1, true, p1_1), u.mr(git, true, "g1"));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p2, cruiseConfig, given);
        assertThat(finalRevisions, is(given));
    }

    @Test
    public void shouldResolveWithModifiedStageDefinitionOfRootNode() throws Exception {
        /**
         * git -------+
         *  |         |
         *  |         |
         *  v         v
         *  P1 -----> P2
         */

        GitMaterial git = u.wf(new GitMaterial("git"), "folder1");
        String[] git_revs = {"g1", "g2"};
        int i = 0;
        u.checkinInOrder(git, u.d(i++), git_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("P1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("P2", u.m(p1), u.m(git));

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g1");

        setMaxBackTrackLimit(2);

        for (int j = 1; j <= maxBackTrackLimit() + 1; j++) {
            u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), p1_1, "g1");
        }

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g2");

        p2 = u.changeStagenameForToPipeline("P2", "s", "new-stage");
        CruiseConfig cruiseConfig = goConfigDao.load();

        MaterialRevisions given = u.mrs(u.mr(p1, true, p1_2), u.mr(git, true, "g2"));

        MaterialRevisions finalRevisions = getRevisionsBasedOnDependencies(p2, cruiseConfig, given);
        assertThat(finalRevisions, is(given));
    }

    @Test
    public void shouldConsiderFailedBuildOfRootNodeForFinalRevisionComputation() {
        /*
             +---> P1 ---+
             |           v
            git-------> P2
         */
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11", "g12", "g13"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git1), u.m(p1));

        CruiseConfig cruiseConfig = goConfigDao.load();

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11", p1_1);

        String p1_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g12");
        String p2_2 = u.runAndFail(p2, u.d(i++), "g12", p1_2);

        String p1_3 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g13");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_3),
                u.mr(git1, true, "g13"));

        assertThat(getRevisionsBasedOnDependencies(p2, cruiseConfig, given), is(given));
    }

    @Test
    public void shouldResolveWithModifiedMaterialDefinitionOfRoot() throws Exception {
        /*
             +---> P1 ---+
             |           v
            git-------> P2 <---- hg
         */
        int i = 1;
        GitMaterial git1 = u.wf(new GitMaterial("git1"), "folder");
        String[] git_revs1 = {"g11"};
        u.checkinInOrder(git1, u.d(i++), git_revs1);

        HgMaterial hg = u.wf(new HgMaterial("hg", null), "folder1");
        String[] hg_revs1 = {"h11"};
        u.checkinInOrder(hg, u.d(i++), hg_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(git1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(git1), u.m(p1));

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "g11");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "g11", p1_1);
        p2 = u.addMaterialToPipeline(p2, u.m(hg));


        MaterialRevisions given = u.mrs(
                u.mr(git1, true, "g11"),
                u.mr(p1, true, p1_1),
                u.mr(hg, true, "h11"));

        assertThat(getRevisionsBasedOnDependencies(p2, goConfigDao.load(), given), is(given));
    }

    @Test
    public void shouldResolveTriangleDependencyWithPackageMaterial() {
        /*
            +---> P1 ---+
            |           v
           pkg1-------> P2
        */
        int i = 1;
        PackageMaterial pkg1 = (PackageMaterial) MaterialsMother.packageMaterial();
        u.addPackageDefinition((PackageMaterialConfig) pkg1.config());
        String[] pkg_revs1 = {"pkg1-1", "pkg1-2"};
        u.checkinInOrder(pkg1, u.d(i++), pkg_revs1);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(pkg1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(pkg1), u.m(p1));

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "pkg1-1");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "pkg1-1", p1_1);

        MaterialRevisions given = u.mrs(
                u.mr(pkg1, true, "pkg1-2"),
                u.mr(p1, true, p1_1));

        MaterialRevisions expected = u.mrs(
                u.mr(pkg1, true, "pkg1-1"),
                u.mr(p1, true, p1_1));

        assertThat(getRevisionsBasedOnDependencies(p2, goConfigDao.load(), given), is(expected));
    }

    @Test
    public void shouldResolveDiamondDependencyWithPackageMaterial() {
        /*
            +---> P1 ---+
            |           v
           pkg1         P3
            |           ^
            +--> P2 ----+
        */
        int i = 1;
        PackageMaterial pkg1 = (PackageMaterial) MaterialsMother.packageMaterial();
        u.addPackageDefinition((PackageMaterialConfig) pkg1.config());
        String[] pkg_revs = {"pkg1-1", "pkg1-2"};
        u.checkinInOrder(pkg1, u.d(i++), pkg_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(pkg1));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(pkg1));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2));

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "pkg1-1");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "pkg1-1");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "pkg1-2");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_2));

        MaterialRevisions expected = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_1));

        assertThat(getRevisionsBasedOnDependencies(p3, goConfigDao.load(), given), is(expected));
    }

    @Test
    public void shouldResolveDiamondDependencyWithPluggableSCMMaterial() {
        /*
            +---> P1 ---+
            |           v
           scm1         P3
            |           ^
            +--> P2 ----+
        */
        int i = 1;
        PluggableSCMMaterial pluggableSCMMaterial = MaterialsMother.pluggableSCMMaterial();
        u.addSCMConfig(pluggableSCMMaterial.getScmConfig());
        String[] pkg_revs = {"scm1-1", "scm1-2"};
        u.checkinInOrder(pluggableSCMMaterial, u.d(i++), pkg_revs);

        ScheduleTestUtil.AddedPipeline p1 = u.saveConfigWith("p1", u.m(pluggableSCMMaterial));
        ScheduleTestUtil.AddedPipeline p2 = u.saveConfigWith("p2", u.m(pluggableSCMMaterial));
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(p1), u.m(p2));

        String p1_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p1, u.d(i++), "scm1-1");
        String p2_1 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "scm1-1");
        String p2_2 = u.runAndPassWithGivenMDUTimestampAndRevisionStrings(p2, u.d(i++), "scm1-2");

        MaterialRevisions given = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_2));

        MaterialRevisions expected = u.mrs(
                u.mr(p1, true, p1_1),
                u.mr(p2, true, p2_1));

        assertThat(getRevisionsBasedOnDependencies(p3, goConfigDao.load(), given), is(expected));
    }

    @Test
    public void shouldResolveDiamondDependencyWithChildrenDependingOnDifferentStageDependency() throws Exception {
        /*
               +---> P3 ---+
               |           v
        pkg -> P2          P5
               |           ^
               +--> P4 ----+
        */
        GitMaterial git = u.wf(new GitMaterial("git"), "f");
        u.checkinInOrder(git, "g1");

        ScheduleTestUtil.AddedPipeline p2_s1 = u.saveConfigWith("p2", "s1", u.m(git));
        ScheduleTestUtil.AddedPipeline p2_s2 = u.addStageToPipeline(p2_s1.config.name(), "s2");
        ScheduleTestUtil.AddedPipeline p3 = u.saveConfigWith("p3", u.m(new DependencyMaterial(p2_s1.config.name(), new CaseInsensitiveString("s1"))));
        ScheduleTestUtil.AddedPipeline p4 = u.saveConfigWith("p4", u.m(new DependencyMaterial(p2_s1.config.name(), new CaseInsensitiveString("s2"))));
        ScheduleTestUtil.AddedPipeline p5 = u.saveConfigWith("p5", u.m(p3), u.m(p4));

        String p2_s1_1 = u.runAndPass(p2_s1, "g1");
        String p2_s2_1 = u.runAndPass(p2_s2, "g1");
        String p3_1 = u.runAndPass(p3, p2_s1_1);
        String p4_1 = u.runAndPass(p4, p2_s2_1);

        MaterialRevisions given = u.mrs(
                u.mr(p3, true, p3_1),
                u.mr(p4, true, p4_1));

        MaterialRevisions revisionsBasedOnDependencies = getRevisionsBasedOnDependencies(p5, goConfigDao.load(), given);
        assertThat(revisionsBasedOnDependencies, is(given));
    }

    private BuildCause getBuildCause(ScheduleTestUtil.AddedPipeline staging, MaterialRevisions given, MaterialRevisions previous) {
        AutoBuild autoBuild = new AutoBuild(goConfigService, pipelineService, staging.config.name().toString(), systemEnvironment, materialChecker, serverHealthService);
        pipelineTimeline.update();
        return autoBuild.onModifications(given, false, previous);
    }

    private MaterialRevisions getRevisionsBasedOnDependencies(ScheduleTestUtil.AddedPipeline pipeline, CruiseConfig cruiseConfig, MaterialRevisions given) {
        pipelineTimeline.update();
        return pipelineService.getRevisionsBasedOnDependencies(given, cruiseConfig, pipeline.config.name());
    }

}
