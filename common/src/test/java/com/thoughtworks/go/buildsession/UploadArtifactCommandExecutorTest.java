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
package com.thoughtworks.go.buildsession;

import com.thoughtworks.go.util.FileUtil;
import org.junit.Test;

import java.io.File;

import static com.thoughtworks.go.domain.BuildCommand.uploadArtifact;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.matchers.ConsoleOutMatcher.printedRuleDoesNotMatchFailure;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertTrue;

public class UploadArtifactCommandExecutorTest extends BuildSessionBasedTestCase {

    @Test
    public void uploadSingleFileArtifact() throws Exception {
        File targetFile = new File(sandbox, "foo");
        assertTrue(targetFile.createNewFile());
        runBuild(uploadArtifact("foo", "foo-dest", false).setWorkingDirectory(sandbox.getPath()), Passed);
        assertThat(artifactsRepository.getFileUploaded().size(), is(1));
        assertThat(artifactsRepository.getFileUploaded().get(0).file, is(targetFile));
        assertThat(artifactsRepository.getFileUploaded().get(0).destPath, is("foo-dest"));
        assertThat(artifactsRepository.getFileUploaded().get(0).buildId, is("build1"));
    }

    @Test
    public void uploadMultipleArtifact() throws Exception {
        File dir = new File(sandbox, "foo");
        assertTrue(dir.mkdirs());
        assertTrue(new File(dir, "bar").createNewFile());
        assertTrue(new File(dir, "baz").createNewFile());
        runBuild(uploadArtifact("foo/*", "foo-dest", false), Passed);
        assertThat(artifactsRepository.getFileUploaded().size(), is(2));
        assertThat(artifactsRepository.getFileUploaded().get(0).file, is(new File(dir, "bar")));
        assertThat(artifactsRepository.getFileUploaded().get(1).file, is(new File(dir, "baz")));
    }

    @Test
    public void shouldUploadMatchedFolder() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/fail.png", "logs/pic/pass.png", "README");
        runBuild(uploadArtifact("**/*", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic"), "mypic/logs"), is(true));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "README"), "mypic"), is(true));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/fail.png"), "mypic/logs/pic"), is(false));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/pass.png"), "mypic/logs/pic"), is(false));
    }

    @Test
    public void shouldNotUploadFileContainingFolderAgain() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/fail.png", "logs/pic/pass.png", "README");
        runBuild(uploadArtifact("logs/pic/*.png", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/pass.png"), "mypic"), is(true));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/fail.png"), "mypic"), is(true));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic"), "mypic"), is(false));
    }

    @Test
    public void shouldUploadFolderWhenMatchedWithWildCards() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");
        runBuild(uploadArtifact("logs/pic-*", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1/pass.png"), "mypic"), is(false));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1/fail.png"), "mypic"), is(false));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-2/cancel.png"), "mypic"), is(false));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-2/complete.png"), "mypic"), is(false));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1"), "mypic"), is(true));
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-2"), "mypic"), is(true));
    }

    @Test
    public void shouldUploadFolderWhenDirectMatch() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");
        runBuild(uploadArtifact("logs/pic-1", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1"), "mypic"), is(true));
    }

    @Test
    public void shouldFailBuildWhenNothingMatched() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");

        runBuild(uploadArtifact("logs/picture", "mypic", false), Failed);
        assertThat(artifactsRepository.getFileUploaded().size(), is(0));
        assertThat(console.output(), printedRuleDoesNotMatchFailure(sandbox.getPath(), "logs/picture"));
    }

    @Test
    public void shouldFailBuildWhenSourceDirectoryDoesNotExist() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");
        runBuild(uploadArtifact("not-Exist-Folder", "mypic", false), Failed);
        assertThat(console.output(), printedRuleDoesNotMatchFailure(sandbox.getPath(), "not-Exist-Folder"));
    }

    @Test
    public void shouldFailBuildWhenNothingMatchedUsingMatcherStartDotStart() throws Exception {
        runBuild(uploadArtifact("target/pkg/*.*", "MYDEST", false), Failed);
        assertThat(console.output(), printedRuleDoesNotMatchFailure(sandbox.getPath(), "target/pkg/*.*"));
    }

    @Test
    public void shouldNotFailBuildWhenNothingMatchedWhenIngnoreUnmatchError() throws Exception {
        runBuild(uploadArtifact("target/pkg/*.*", "MYDEST", true), Passed);
        assertThat(console.output(), printedRuleDoesNotMatchFailure(sandbox.getPath(), "target/pkg/*.*"));
    }

    @Test
    public void shouldFailBuildWhenUploadErrorHappened() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/pass.png", "logs/pic-1/pass.png");
        artifactsRepository.setUploadError(new RuntimeException("upload failed!!"));
        runBuild(uploadArtifact("**/*.png", "mypic", false), Failed);
        assertThat(artifactsRepository.getFileUploaded().size(), is(0));
    }

    @Test
    public void shouldStillFailBuildWhenIgnoreUnmatchErrorButUploadErrorHappened() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/pass.png", "logs/pic-1/pass.png");
        artifactsRepository.setUploadError(new RuntimeException("upload failed!!"));
        runBuild(uploadArtifact("**/*.png", "mypic", true), Failed);
        assertThat(artifactsRepository.getFileUploaded().size(), is(0));
    }

}