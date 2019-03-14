/*
 * Copyright 2019 ThoughtWorks, Inc.
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
import org.junit.jupiter.api.Test;

import java.io.File;

import static com.thoughtworks.go.domain.BuildCommand.uploadArtifact;
import static com.thoughtworks.go.domain.JobResult.Failed;
import static com.thoughtworks.go.domain.JobResult.Passed;
import static com.thoughtworks.go.matchers.ConsoleOutMatcherJunit5.assertConsoleOut;
import static org.assertj.core.api.Assertions.assertThat;

class UploadArtifactCommandExecutorTest extends BuildSessionBasedTestCase {

    @Test
    void uploadSingleFileArtifact() throws Exception {
        File targetFile = new File(sandbox, "foo");
        assertThat(targetFile.createNewFile()).isTrue();
        runBuild(uploadArtifact("foo", "foo-dest", false).setWorkingDirectory(sandbox.getPath()), Passed);
        assertThat(artifactsRepository.getFileUploaded().size()).isEqualTo(1);
        assertThat(artifactsRepository.getFileUploaded().get(0).file).isEqualTo(targetFile);
        assertThat(artifactsRepository.getFileUploaded().get(0).destPath).isEqualTo("foo-dest");
        assertThat(artifactsRepository.getFileUploaded().get(0).buildId).isEqualTo("build1");
    }

    @Test
    void uploadMultipleArtifact() throws Exception {
        File dir = new File(sandbox, "foo");
        assertThat(dir.mkdirs()).isTrue();
        assertThat(new File(dir, "bar").createNewFile()).isTrue();
        assertThat(new File(dir, "baz").createNewFile()).isTrue();
        runBuild(uploadArtifact("foo/*", "foo-dest", false), Passed);
        assertThat(artifactsRepository.getFileUploaded().size()).isEqualTo(2);
        assertThat(artifactsRepository.getFileUploaded().get(0).file).isEqualTo(new File(dir, "bar"));
        assertThat(artifactsRepository.getFileUploaded().get(1).file).isEqualTo(new File(dir, "baz"));
    }

    @Test
    void shouldUploadMatchedFolder() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/fail.png", "logs/pic/pass.png", "README");
        runBuild(uploadArtifact("**/*", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic"), "mypic/logs")).isTrue();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "README"), "mypic")).isTrue();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/fail.png"), "mypic/logs/pic")).isFalse();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/pass.png"), "mypic/logs/pic")).isFalse();
    }

    @Test
    void shouldNotUploadFileContainingFolderAgain() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/fail.png", "logs/pic/pass.png", "README");
        runBuild(uploadArtifact("logs/pic/*.png", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/pass.png"), "mypic")).isTrue();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic/fail.png"), "mypic")).isTrue();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic"), "mypic")).isFalse();
    }

    @Test
    void shouldUploadFolderWhenMatchedWithWildCards() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");
        runBuild(uploadArtifact("logs/pic-*", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1/pass.png"), "mypic")).isFalse();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1/fail.png"), "mypic")).isFalse();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-2/cancel.png"), "mypic")).isFalse();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-2/complete.png"), "mypic")).isFalse();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1"), "mypic")).isTrue();
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-2"), "mypic")).isTrue();
    }

    @Test
    void shouldUploadFolderWhenDirectMatch() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");
        runBuild(uploadArtifact("logs/pic-1", "mypic", false), Passed);
        assertThat(artifactsRepository.isFileUploaded(new File(sandbox, "logs/pic-1"), "mypic")).isTrue();
    }

    @Test
    void shouldFailBuildWhenNothingMatched() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");

        runBuild(uploadArtifact("logs/picture", "mypic", false), Failed);
        assertThat(artifactsRepository.getFileUploaded().size()).isEqualTo(0);
        assertConsoleOut(console.output()).printedRuleDoesNotMatchFailure(sandbox.getPath(), "logs/picture");
    }

    @Test
    void shouldFailBuildWhenSourceDirectoryDoesNotExist() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic-1/fail.png", "logs/pic-1/pass.png", "logs/pic-2/cancel.png", "logs/pic-2/complete.png", "README");
        runBuild(uploadArtifact("not-Exist-Folder", "mypic", false), Failed);
        assertConsoleOut(console.output()).printedRuleDoesNotMatchFailure(sandbox.getPath(), "not-Exist-Folder");
    }

    @Test
    void shouldFailBuildWhenNothingMatchedUsingMatcherStartDotStart() {
        runBuild(uploadArtifact("target/pkg/*.*", "MYDEST", false), Failed);
        assertConsoleOut(console.output()).printedRuleDoesNotMatchFailure(sandbox.getPath(), "target/pkg/*.*");
    }

    @Test
    void shouldNotFailBuildWhenNothingMatchedWhenIngnoreUnmatchError() {
        runBuild(uploadArtifact("target/pkg/*.*", "MYDEST", true), Passed);
        assertConsoleOut(console.output()).printedRuleDoesNotMatchFailure(sandbox.getPath(), "target/pkg/*.*");
    }

    @Test
    void shouldFailBuildWhenUploadErrorHappened() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/pass.png", "logs/pic-1/pass.png");
        artifactsRepository.setUploadError(new RuntimeException("upload failed!!"));
        runBuild(uploadArtifact("**/*.png", "mypic", false), Failed);
        assertThat(artifactsRepository.getFileUploaded().size()).isEqualTo(0);
    }

    @Test
    void shouldStillFailBuildWhenIgnoreUnmatchErrorButUploadErrorHappened() throws Exception {
        FileUtil.createFilesByPath(sandbox, "logs/pic/pass.png", "logs/pic-1/pass.png");
        artifactsRepository.setUploadError(new RuntimeException("upload failed!!"));
        runBuild(uploadArtifact("**/*.png", "mypic", true), Failed);
        assertThat(artifactsRepository.getFileUploaded().size()).isEqualTo(0);
    }
}
