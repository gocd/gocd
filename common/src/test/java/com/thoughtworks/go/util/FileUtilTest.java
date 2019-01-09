/*
 * Copyright 2017 ThoughtWorks, Inc.
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

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.thoughtworks.go.junitext.EnhancedOSChecker;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;

import static com.thoughtworks.go.junitext.EnhancedOSChecker.DO_NOT_RUN_ON;
import static com.thoughtworks.go.junitext.EnhancedOSChecker.WINDOWS;
import static com.thoughtworks.go.util.FileUtil.isSubdirectoryOf;
import static com.thoughtworks.go.util.TestUtils.isSameAsPath;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(JunitExtRunner.class)
public class FileUtilTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Before
    public void setUp() throws Exception {
        temporaryFolder.create();
    }

    @After
    public void tearDown() throws Exception {
        temporaryFolder.delete();
    }

    @Test
    public void shouldBeHiddenIfFileStartWithDot() {
        assertTrue(FileUtil.isHidden(new File(".svn")));
    }

    @Test
    public void shouldBeHiddenIfFileIsHidden() {
        File mockFile = Mockito.mock(File.class);
        Mockito.when(mockFile.isHidden()).thenReturn(true);
        assertTrue(FileUtil.isHidden(mockFile));
    }

    @Test
    public void shouldUseSpeficiedFolderIfAbsolute() throws Exception {
        final File absolutePath = new File("zx").getAbsoluteFile();
        assertThat(FileUtil.applyBaseDirIfRelative(new File("xyz"), absolutePath), is(absolutePath));
    }

    @Test
    public void shouldUseSpeficiedFolderIfBaseDirIsEmpty() throws Exception {
        assertThat(FileUtil.applyBaseDirIfRelative(new File(""), new File("zx")), is(new File("zx")));
    }

    @Test
    public void shouldAppendToDefaultIfRelative() throws Exception {
        final File relativepath = new File("zx");
        assertThat(FileUtil.applyBaseDirIfRelative(new File("xyz"), relativepath),
                is(new File("xyz", relativepath.getPath())));
    }

    @Test
    public void shouldUseDefaultIfActualisNull() throws Exception {
        final File baseFile = new File("xyz");
        assertThat(FileUtil.applyBaseDirIfRelative(baseFile, null), is(baseFile));
    }

    @Test
    public void shouldCreateUniqueHashForFolders() throws Exception {
        File file = new File("c:a/b/c/d/e");
        File file2 = new File("c:foo\\bar\\baz");
        assertThat(FileUtil.filesystemSafeFileHash(file).matches("[0-9a-zA-Z\\.\\-]*"), is(true));
        assertThat(FileUtil.filesystemSafeFileHash(file2), not(FileUtil.filesystemSafeFileHash(file)));
    }

    @Test
    public void shouldDetectSubfolders() throws Exception {
        assertThat(isSubdirectoryOf(new File("a"), new File("a")), is(true));
        assertThat(isSubdirectoryOf(new File("a"), new File("a/b")), is(true));
        assertThat(isSubdirectoryOf(new File("a"), new File("aaaa")), is(false));
        assertThat(isSubdirectoryOf(new File("a/b/c/d"), new File("a/b/c/d/e")), is(true));
        assertThat(isSubdirectoryOf(new File("a/b/c/d/e"), new File("a/b/c/d")), is(false));
        assertThat(isSubdirectoryOf(new File("/a/b"), new File("c/d")), is(false));
    }

    @Test
    public void shouldDetectSubfoldersWhenUsingRelativePaths() throws Exception {
        File parent = new File("/a/b");
        assertThat(isSubdirectoryOf(parent, new File(parent, "../../..")), is(false));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {DO_NOT_RUN_ON, WINDOWS})
    public void shouldCreateFileURIForFile() {
        assertThat(FileUtil.toFileURI(new File("/var/lib/foo/")), is("file:///var/lib/foo"));
        assertThat(FileUtil.toFileURI(new File("/var/a dir with spaces/foo")), is("file:///var/a%20dir%20with%20spaces/foo"));
        assertThat(FileUtil.toFileURI(new File("/var/司徒空在此/foo")), is("file:///var/%E5%8F%B8%E5%BE%92%E7%A9%BA%E5%9C%A8%E6%AD%A4/foo"));
    }


    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldCreateFileURIForFileOnWindows() {
        assertThat(FileUtil.toFileURI(new File("c:\\foo")).startsWith("file:///c:/foo"), is(true));
        assertThat(FileUtil.toFileURI(new File("c:\\a dir with spaces\\foo")).startsWith("file:///c:/a%20dir%20with%20spaces/foo"), is(true));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldReturnFalseForInvalidWindowsUNCFilePath() {
        assertThat(FileUtil.isAbsolutePath("\\\\host\\"), is(false));
        assertThat(FileUtil.isAbsolutePath("\\\\host"), is(false));
    }

    @Test
    @RunIf(value = EnhancedOSChecker.class, arguments = {EnhancedOSChecker.WINDOWS})
    public void shouldReturnTrueForValidWindowsUNCFilePath() {
        assertThat(FileUtil.isAbsolutePath("\\\\host\\share"), is(true));
        assertThat(FileUtil.isAbsolutePath("\\\\host\\share\\dir"), is(true));
    }

    @Test
    public void FolderIsEmptyWhenItHasNoContents() throws Exception {
        File folder = temporaryFolder.newFolder();
        assertThat(FileUtil.isFolderEmpty(folder), is(true));
    }

    @Test
    public void FolderIsNotEmptyWhenItHasContents() throws Exception {
        File folder = temporaryFolder.newFolder();
        new File(folder, "subfolder").createNewFile();
        assertThat(FileUtil.isFolderEmpty(folder), is(false));
    }

    @Test
    public void shouldReturnCanonicalPath() throws IOException {
        File f = temporaryFolder.newFolder();
        assertThat(FileUtil.getCanonicalPath(f), is(f.getCanonicalPath()));
        File spyFile = spy(new File("/xyz/non-existent-file"));
        IOException canonicalPathException = new IOException("Failed to build the canonical path");
        when(spyFile.getCanonicalPath()).thenThrow(canonicalPathException);
        try {
            FileUtil.getCanonicalPath(spyFile);
        } catch (RuntimeException e) {
            assertThat(e.getCause(), is(canonicalPathException));
        }
    }

    @Test
    public void shouldRemoveLeadingFilePathFromAFilePath() throws Exception {
        File file = new File("/var/command-repo/default/windows/echo.xml");
        File base = new File("/var/command-repo/default");

        assertThat(FileUtil.removeLeadingPath(base.getAbsolutePath(), file.getAbsolutePath()), isSameAsPath("/windows/echo.xml"));
        assertThat(FileUtil.removeLeadingPath(new File("/var/command-repo/default/").getAbsolutePath(), new File("/var/command-repo/default/windows/echo.xml").getAbsolutePath()), isSameAsPath("/windows/echo.xml"));
        assertThat(FileUtil.removeLeadingPath("/some/random/path", "/var/command-repo/default/windows/echo.xml"), is("/var/command-repo/default/windows/echo.xml"));
        assertThat(FileUtil.removeLeadingPath(new File("C:/blah").getAbsolutePath(), new File("C:/blah/abcd.txt").getAbsolutePath()), isSameAsPath("/abcd.txt"));
        assertThat(FileUtil.removeLeadingPath(new File("C:/blah/").getAbsolutePath(), new File("C:/blah/abcd.txt").getAbsolutePath()), isSameAsPath("/abcd.txt"));
        assertThat(FileUtil.removeLeadingPath(null, new File("/blah/abcd.txt").getAbsolutePath()), isSameAsPath(new File("/blah/abcd.txt").getAbsolutePath()));
        assertThat(FileUtil.removeLeadingPath("", new File("/blah/abcd.txt").getAbsolutePath()), isSameAsPath(new File("/blah/abcd.txt").getAbsolutePath()));
    }

    @Test
    public void shouldReturnTrueIfDirectoryIsReadable() throws IOException {
        File readableDirectory = mock(File.class);
        when(readableDirectory.canRead()).thenReturn(true);
        when(readableDirectory.canExecute()).thenReturn(true);
        when(readableDirectory.listFiles()).thenReturn(new File[]{});
        assertThat(FileUtil.isDirectoryReadable(readableDirectory), is(true));

        File unreadableDirectory = mock(File.class);
        when(readableDirectory.canRead()).thenReturn(false);
        when(readableDirectory.canExecute()).thenReturn(false);
        assertThat(FileUtil.isDirectoryReadable(unreadableDirectory), is(false));

        verify(readableDirectory).canRead();
        verify(readableDirectory).canExecute();
        verify(readableDirectory).listFiles();
        verify(unreadableDirectory).canRead();
        verify(unreadableDirectory, never()).canExecute();
    }

    @Test
    public void shouldCalculateSha1Digest() throws IOException {
        File tempFile = temporaryFolder.newFile();
        FileUtils.writeStringToFile(tempFile, "12345", UTF_8);
        assertThat(FileUtil.sha1Digest(tempFile), is("jLIjfQZ5yojbZGTqxg2pY0VROWQ="));
    }

}
