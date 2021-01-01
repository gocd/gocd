/*
 * Copyright 2021 ThoughtWorks, Inc.
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
package com.thoughtworks.go.util;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractAssert;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.migrationsupport.rules.EnableRuleMigrationSupport;
import org.junit.rules.TemporaryFolder;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.util.FileUtil.isSubdirectoryOf;
import static com.thoughtworks.go.util.FileUtilTest.FilePathMatcher.assertPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@EnableRuleMigrationSupport
public class FileUtilTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeEach
    void setUp() throws Exception {
        temporaryFolder.create();
    }

    @AfterEach
    void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    @Test
    void shouldBeHiddenIfFileStartWithDot() {
        assertThat(FileUtil.isHidden(new File(".svn"))).isTrue();
    }

    @Test
    void shouldBeHiddenIfFileIsHidden() {
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.isHidden()).thenReturn(true);
        assertThat(FileUtil.isHidden(mockFile)).isTrue();
    }

    @Test
    void shouldUseSpeficiedFolderIfAbsolute() {
        final File absolutePath = new File("zx").getAbsoluteFile();
        assertThat(FileUtil.applyBaseDirIfRelative(new File("xyz"), absolutePath)).isEqualTo(absolutePath);
    }

    @Test
    void shouldUseSpeficiedFolderIfBaseDirIsEmpty() throws Exception {
        assertThat(FileUtil.applyBaseDirIfRelative(new File(""), new File("zx"))).isEqualTo(new File("zx"));
    }

    @Test
    void shouldAppendToDefaultIfRelative() throws Exception {
        final File relativepath = new File("zx");
        assertThat(FileUtil.applyBaseDirIfRelative(new File("xyz"), relativepath)).isEqualTo(new File("xyz", relativepath.getPath()));
    }

    @Test
    void shouldUseDefaultIfActualisNull() throws Exception {
        final File baseFile = new File("xyz");
        assertThat(FileUtil.applyBaseDirIfRelative(baseFile, null)).isEqualTo(baseFile);
    }

    @Test
    void shouldCreateUniqueHashForFolders() throws Exception {
        File file = new File("c:a/b/c/d/e");
        File file2 = new File("c:foo\\bar\\baz");
        assertThat(FileUtil.filesystemSafeFileHash(file).matches("[0-9a-zA-Z\\.\\-]*")).isTrue();
        assertThat(FileUtil.filesystemSafeFileHash(file2)).isNotEqualTo(FileUtil.filesystemSafeFileHash(file));
    }

    @Test
    void shouldDetectSubfolders() throws Exception {
        assertThat(isSubdirectoryOf(new File("a"), new File("a"))).isTrue();
        assertThat(isSubdirectoryOf(new File("a"), new File("a/b"))).isTrue();
        assertThat(isSubdirectoryOf(new File("a"), new File("aaaa"))).isFalse();
        assertThat(isSubdirectoryOf(new File("a/b/c/d"), new File("a/b/c/d/e"))).isTrue();
        assertThat(isSubdirectoryOf(new File("a/b/c/d/e"), new File("a/b/c/d"))).isFalse();
        assertThat(isSubdirectoryOf(new File("/a/b"), new File("c/d"))).isFalse();
    }

    @Test
    void shouldDetectSubfoldersWhenUsingRelativePaths() throws Exception {
        File parent = new File("/a/b");
        assertThat(isSubdirectoryOf(parent, new File(parent, "../../.."))).isFalse();
    }

    @Test
    @DisabledOnOs(OS.WINDOWS)
    void shouldCreateFileURIForFile() {
        assertThat(FileUtil.toFileURI(new File("/var/lib/foo/"))).isEqualTo("file:///var/lib/foo");
        assertThat(FileUtil.toFileURI(new File("/var/a dir with spaces/foo"))).isEqualTo("file:///var/a%20dir%20with%20spaces/foo");
        assertThat(FileUtil.toFileURI(new File("/var/司徒空在此/foo"))).isEqualTo("file:///var/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4/foo");
    }


    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldCreateFileURIForFileOnWindows() {
        assertThat(FileUtil.toFileURI(new File("c:\\foo")).startsWith("file:///c:/foo")).isTrue();
        assertThat(FileUtil.toFileURI(new File("c:\\a dir with spaces\\foo")).startsWith("file:///c:/a%20dir%20with%20spaces/foo")).isTrue();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldReturnFalseForInvalidWindowsUNCFilePath() {
        assertThat(FileUtil.isAbsolutePath("\\\\host\\")).isFalse();
        assertThat(FileUtil.isAbsolutePath("\\\\host")).isFalse();
    }

    @Test
    @EnabledOnOs(OS.WINDOWS)
    void shouldReturnTrueForValidWindowsUNCFilePath() {
        assertThat(FileUtil.isAbsolutePath("\\\\host\\share")).isTrue();
        assertThat(FileUtil.isAbsolutePath("\\\\host\\share\\dir")).isTrue();
    }

    @Test
    void FolderIsEmptyWhenItHasNoContents() throws Exception {
        File folder = temporaryFolder.newFolder();
        assertThat(FileUtil.isFolderEmpty(folder)).isTrue();
    }

    @Test
    void FolderIsNotEmptyWhenItHasContents() throws Exception {
        File folder = temporaryFolder.newFolder();
        new File(folder, "subfolder").createNewFile();
        assertThat(FileUtil.isFolderEmpty(folder)).isFalse();
    }

    @Test
    void shouldReturnCanonicalPath() throws IOException {
        File f = temporaryFolder.newFolder();
        assertThat(FileUtil.getCanonicalPath(f)).isEqualTo(f.getCanonicalPath());
        File spyFile = spy(new File("/xyz/non-existent-file"));
        IOException canonicalPathException = new IOException("Failed to build the canonical path");
        when(spyFile.getCanonicalPath()).thenThrow(canonicalPathException);
        try {
            FileUtil.getCanonicalPath(spyFile);
        } catch (RuntimeException e) {
            assertThat(e.getCause()).isEqualTo(canonicalPathException);
        }
    }

    @Test
    void shouldRemoveLeadingFilePathFromAFilePathOnWindows() {
        File file = new File("/var/command-repo/default/windows/echo.xml");
        File base = new File("/var/command-repo/default");

        assertPath(FileUtil.removeLeadingPath(base.getAbsolutePath(), file.getAbsolutePath())).isSameAs("/windows/echo.xml");
        assertPath(FileUtil.removeLeadingPath(new File("/var/command-repo/default/").getAbsolutePath(), new File("/var/command-repo/default/windows/echo.xml").getAbsolutePath())).isSameAs("/windows/echo.xml");
        assertThat(FileUtil.removeLeadingPath("/some/random/path", "/var/command-repo/default/windows/echo.xml")).isEqualTo("/var/command-repo/default/windows/echo.xml");
        assertPath(FileUtil.removeLeadingPath(new File("C:/blah").getAbsolutePath(), new File("C:/blah/abcd.txt").getAbsolutePath())).isSameAs("/abcd.txt");
        assertPath(FileUtil.removeLeadingPath(new File("C:/blah/").getAbsolutePath(), new File("C:/blah/abcd.txt").getAbsolutePath())).isSameAs("/abcd.txt");
        assertPath(FileUtil.removeLeadingPath(null, new File("/blah/abcd.txt").getAbsolutePath())).isSameAs(new File("/blah/abcd.txt").getAbsolutePath());
        assertPath(FileUtil.removeLeadingPath("", new File("/blah/abcd.txt").getAbsolutePath())).isSameAs(new File("/blah/abcd.txt").getAbsolutePath());
    }

    @Test
    void shouldReturnTrueIfDirectoryIsReadable() {
        File readableDirectory = mock(File.class);
        when(readableDirectory.canRead()).thenReturn(true);
        when(readableDirectory.canExecute()).thenReturn(true);
        when(readableDirectory.listFiles()).thenReturn(new File[]{});
        assertThat(FileUtil.isDirectoryReadable(readableDirectory)).isTrue();

        File unreadableDirectory = mock(File.class);
        when(readableDirectory.canRead()).thenReturn(false);
        when(readableDirectory.canExecute()).thenReturn(false);
        assertThat(FileUtil.isDirectoryReadable(unreadableDirectory)).isFalse();

        verify(readableDirectory).canRead();
        verify(readableDirectory).canExecute();
        verify(readableDirectory).listFiles();
        verify(unreadableDirectory).canRead();
        verify(unreadableDirectory, never()).canExecute();
    }

    @Test
    void shouldCalculateSha1Digest() throws IOException {
        File tempFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(tempFile, "12345", UTF_8);
        assertThat(FileUtil.sha1Digest(tempFile)).isEqualTo("jLIjfQZ5yojbZGTqxg2pY0VROWQ=");
    }

    static class FilePathMatcher extends AbstractAssert<FilePathMatcher, String> {

        public FilePathMatcher(String consoleOut) {
            super(consoleOut, FilePathMatcher.class);
        }

        public static FilePathMatcher assertPath(String actual) {
            return new FilePathMatcher(actual);
        }

        public FilePathMatcher isSameAs(String expected) {
            final String osSpecificPath = expected.replace('/', File.separatorChar);
            if (!StringUtils.equals(actual, osSpecificPath)) {
                failWithMessage("The actual path: [<%s>] does not match the expected path [<%s>]", actual, osSpecificPath);
            }

            return this;
        }
    }

}
