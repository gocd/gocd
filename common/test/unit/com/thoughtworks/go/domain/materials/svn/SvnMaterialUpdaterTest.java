package com.thoughtworks.go.domain.materials.svn;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.buildsession.BuildSessionBasedTestCase;
import com.thoughtworks.go.config.materials.svn.SvnMaterial;
import com.thoughtworks.go.domain.BuildCommand;
import com.thoughtworks.go.domain.JobResult;
import com.thoughtworks.go.domain.materials.RevisionContext;
import com.thoughtworks.go.helper.SvnTestRepo;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import com.thoughtworks.go.util.command.InMemoryStreamConsumer;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;

@RunWith(JunitExtRunner.class)
public class SvnMaterialUpdaterTest extends BuildSessionBasedTestCase {
    private SvnTestRepo testRepo;
    private File workingCopy;
    private String baseDir;
    private String svnUserName;
    private String svnPassword;

    @Before
    public void setUp() throws IOException {
        testRepo = new SvnTestRepo();
        baseDir = "xyz";
        svnUserName = "user";
        svnPassword = "pass";
        workingCopy = new File(sandbox, baseDir);
    }

    private void checkout(File workingCopy, String revision, SvnTestRepo svnTestRepo) throws IOException {
        new SvnCommand("figerprint", svnTestRepo.projectRepositoryUrl()).checkoutTo(new InMemoryStreamConsumer(), workingCopy, new SubversionRevision(revision));
    }

    private void updateMaterial(String revision, JobResult expectedResult) {
        runBuild(buildCommand(revision), expectedResult);
    }

    private BuildCommand buildCommand(String revision) {
        SvnMaterialUpdater materialUpdater = new SvnMaterialUpdater(new SvnMaterial(testRepo.projectRepositoryUrl(), svnUserName, svnPassword, false));
        return materialUpdater.updateTo(baseDir, new RevisionContext(new SubversionRevision(revision)));
    }

    @Test
    public void testFreshCheckout() throws IOException {
        updateMaterial("3", JobResult.Passed);
        assertThat(getWorkingDirInfo(workingCopy).getUrl(), is(testRepo.projectRepositoryUrl()));
        assertThat(getWorkingDirInfo(workingCopy).getRevision(), is("3"));
    }

    @Test
    public void freshCheckoutWhenWorkingDirNotExists() throws IOException {
        baseDir = "foo/bar/baz";
        workingCopy = new File(sandbox, baseDir);
        updateMaterial("3", JobResult.Passed);
        assertThat(getWorkingDirInfo(workingCopy).getUrl(), is(testRepo.projectRepositoryUrl()));
        assertThat(getWorkingDirInfo(workingCopy).getRevision(), is("3"));
    }


    private SvnCommand.SvnInfo getWorkingDirInfo(File workingCopy) throws IOException {
        SvnCommand svnCommand = new SvnCommand("fingerprint", testRepo.projectRepositoryUrl());
        return svnCommand.workingDirInfo(workingCopy);
    }

    @Test
    public void shouldLogRepoInfoToConsoleOutWithOutFolder() throws Exception {
        updateMaterial("3", JobResult.Passed);
        assertThat(console.output(), containsString("Start updating files at revision 3 from " + testRepo.projectRepositoryUrl()));
    }

    @Test
    public void shouldCheckoutForDirtyWorkingCopy() throws IOException {
        workingCopy.mkdirs();
        File dirt = new File(workingCopy, "dirt");
        dirt.createNewFile();
        updateMaterial("3", JobResult.Passed);
        assertThat(getWorkingDirInfo(workingCopy).getRevision(), is("3"));
        assertThat(dirt.exists(), is(false));
    }

    @Test
    public void shouldCheckoutIfSvnRepositoryChanged() throws IOException {
        updateMaterial("3", JobResult.Passed);
        assertThat(console.output(), containsString("Checked out revision 3"));

        testRepo = new SvnTestRepo();
        updateMaterial("4", JobResult.Passed);
        assertThat(console.output(), containsString("Checked out revision 4"));

        assertThat(getWorkingDirInfo(workingCopy).getUrl(), is(testRepo.projectRepositoryUrl()));
    }

    @Test
    public void shouldUpdateForValidSvnWorkingCopy() throws IOException {
        workingCopy.mkdirs();
        SvnTestRepo repo2 = new SvnTestRepo();
        checkout(workingCopy, "2", repo2);
        updateMaterial("3", JobResult.Passed);
        assertThat(getWorkingDirInfo(workingCopy).getRevision(), is("3"));
    }


    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldRecogniseSvnAsTheSameIfURLContainsSpaces() throws Exception {
        testRepo = new SvnTestRepo("a directory with spaces");
        updateMaterial("3", JobResult.Passed);
        assertThat(console.output(), containsString("Checked out revision 3"));

        console.clear();
        updateMaterial("4", JobResult.Passed);
        assertThat(console.output(), containsString("Updated to revision 4"));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldRecogniseSvnAsTheSameIfURLContainsChineseCharacters() throws Exception {
        testRepo = new SvnTestRepo("a directory with 司徒空在此");
        updateMaterial("3", JobResult.Passed);
        assertThat(console.output(), containsString("Checked out revision 3"));

        console.clear();
        updateMaterial("4", JobResult.Passed);
        assertThat(console.output(), containsString("Updated to revision 4"));
    }

    @Test
    public void shouldFailBuildWithTheSecretHiddenWhenUpdateToFails() {
        updateMaterial(SubversionRevision.HEAD.getRevision(), JobResult.Passed);
        console.clear();
        updateMaterial("-1", JobResult.Failed);
        assertThat(console.output(), containsString("--password ******"));
    }

    @Test public void shouldRevertWorkingCopy() throws Exception {
        updateMaterial(SubversionRevision.HEAD.getRevision(), JobResult.Passed);
        File file = workingCopy.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return !name.equals(".svn");
            }
        })[0];
        file.delete();
        assertThat(file.exists(), is(false));
        updateMaterial(SubversionRevision.HEAD.getRevision(), JobResult.Passed);
        assertThat(file.exists(), is(true));
    }


    @Test
    public void shouldNotAddEmptyPasswordWhenUsernameIsProvidedWithNoPassword() throws IOException {
        svnPassword = "";
        updateMaterial("-1", JobResult.Failed);
        assertThat(console.lastLine(), containsString("--username " + svnUserName));
        assertThat(console.lastLine(), not(containsString("--password")));
    }

    @Test
    public void shouldNotAddUserNameOrPasswordWhenUsernameIsNotProvided() throws IOException {
        svnUserName = "";
        svnPassword = "pass";
        updateMaterial("-1", JobResult.Failed);
        assertThat(console.lastLine(), not(containsString("--username ")));
        assertThat(console.lastLine(), not(containsString("--password")));
    }
}