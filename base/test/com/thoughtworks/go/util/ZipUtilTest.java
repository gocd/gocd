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

package com.thoughtworks.go.util;

import com.googlecode.junit.ext.JunitExtRunner;
import com.googlecode.junit.ext.RunIf;
import com.googlecode.junit.ext.checkers.OSChecker;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.hamcrest.core.Is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

@RunWith(JunitExtRunner.class)
public class ZipUtilTest {
    private File srcDir;
    private File destDir;
    private ZipUtil zipUtil;
    private File childDir1;
    private File file1;
    private File file2;
    private File zipFile;
    private File emptyDir;
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private static String fileContent(File file) throws IOException {
        return IOUtils.toString(new FileInputStream(file));
    }

    @Before
    public void setUp() throws Exception {
        temporaryFolder.create();

        srcDir = temporaryFolder.newFolder("_test1");
        destDir = temporaryFolder.newFolder("_test2");
        emptyDir = new File(srcDir, "_emptyDir");
        emptyDir.mkdir();
        childDir1 = new File(srcDir, "_child1");
        childDir1.mkdir();
        file1 = new File(srcDir, "_file1");
        FileUtils.writeStringToFile(file1, "_file1");
        file2 = new File(childDir1, "_file2");
        FileUtils.writeStringToFile(file2, "_file2");
        zipUtil = new ZipUtil();
    }

    @After
    public void tearDown() {
        temporaryFolder.delete();
    }

    @Test
    public void shouldZipFileAndUnzipIt() throws IOException {
        zipFile = zipUtil.zip(srcDir, temporaryFolder.newFile(), Deflater.NO_COMPRESSION);
        assertThat(zipFile.isFile(), is(true));

        zipUtil.unzip(zipFile, destDir);
        File baseDir = new File(destDir, srcDir.getName());

        assertIsDirectory(new File(baseDir, emptyDir.getName()));
        assertIsDirectory(new File(baseDir, childDir1.getName()));

        File actual1 = new File(baseDir, file1.getName());
        assertThat(actual1.isFile(), is(true));
        assertThat(fileContent(actual1), is(fileContent(file1)));

        File actual2 = new File(baseDir, childDir1.getName() + File.separator + file2.getName());
        assertThat(actual2.isFile(), is(true));
        assertThat(fileContent(actual2), is(fileContent(file2)));
    }

    @Test
    public void shouldZipFileContentsAndUnzipIt() throws IOException {
        zipFile = zipUtil.zip(srcDir, temporaryFolder.newFile(), Deflater.NO_COMPRESSION);
        assertThat(zipFile.isFile(), is(true));

        zipUtil.unzip(zipFile, destDir);
        File baseDir = new File(destDir, srcDir.getName());

        assertIsDirectory(new File(baseDir, emptyDir.getName()));
        assertIsDirectory(new File(baseDir, childDir1.getName()));

        File actual1 = new File(baseDir, file1.getName());
        assertThat(actual1.isFile(), is(true));
        assertThat(fileContent(actual1), is(fileContent(file1)));

        File actual2 = new File(baseDir, childDir1.getName() + File.separator + file2.getName());
        assertThat(actual2.isFile(), is(true));
        assertThat(fileContent(actual2), is(fileContent(file2)));
    }

    @Test
    public void shouldZipFileContentsOnly() throws IOException {
        zipFile = zipUtil.zipFolderContents(srcDir, temporaryFolder.newFile(), Deflater.NO_COMPRESSION);
        assertThat(zipFile.isFile(), is(true));

        zipUtil.unzip(zipFile, destDir);

        assertIsDirectory(new File(destDir, emptyDir.getName()));
        assertIsDirectory(new File(destDir, childDir1.getName()));

        File actual1 = new File(destDir, file1.getName());
        assertThat(actual1.isFile(), is(true));
        assertThat(fileContent(actual1), is(fileContent(file1)));

        File actual2 = new File(destDir, childDir1.getName() + File.separator + file2.getName());
        assertThat(actual2.isFile(), is(true));
        assertThat(fileContent(actual2), is(fileContent(file2)));
    }

    @Test
    @RunIf(value = OSChecker.class, arguments = OSChecker.LINUX)
    public void shouldZipFileWhoseNameHasSpecialCharactersOnLinux() throws IOException {
        File specialFile = new File(srcDir, "$`#?@!()?-_{}^'~.+=[];,a.txt");
        FileUtils.writeStringToFile(specialFile, "specialFile");

        zipFile = zipUtil.zip(srcDir, temporaryFolder.newFile(), Deflater.NO_COMPRESSION);
        zipUtil.unzip(zipFile, destDir);
        File baseDir = new File(destDir, srcDir.getName());

        File actualSpecialFile = new File(baseDir, specialFile.getName());
        assertThat(actualSpecialFile.isFile(), is(true));
        assertThat(fileContent(actualSpecialFile), is(fileContent(specialFile)));
    }

    @Test
    public void shouldReadContentsOfAFileWhichIsInsideAZip() throws Exception {
        FileUtils.writeStringToFile(new File(srcDir, "some-file.txt"), "some-text-here");
        zipFile = zipUtil.zip(srcDir, temporaryFolder.newFile(), Deflater.NO_COMPRESSION);

        String someStuff = zipUtil.getFileContentInsideZip(new ZipInputStream(new FileInputStream(zipFile)), "some-file.txt");

        assertThat(someStuff, Is.is("some-text-here"));
    }

    @Test
    public void shouldZipMultipleFolderContentsAndExcludeRootDirectory() throws IOException {
        File folderOne = temporaryFolder.newFolder("a-folder1");
        FileUtils.writeStringToFile(new File(folderOne, "folder1-file1.txt"), "folder1-file1");
        FileUtils.writeStringToFile(new File(folderOne, "folder1-file2.txt"), "folder1-file2");

        File folderTwo = temporaryFolder.newFolder("a-folder2");
        FileUtils.writeStringToFile(new File(folderTwo, "folder2-file1.txt"), "folder2-file1");
        FileUtils.writeStringToFile(new File(folderTwo, "folder2-file2.txt"), "folder2-file2");

        File targetZipFile = temporaryFolder.newFile("final1.zip");

        ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(targetZipFile, true);
        zipBuilder.add("folder-one", folderOne);
        zipBuilder.add("folder-two", folderTwo);
        zipBuilder.done();

        assertContent(targetZipFile, "folder-one/folder1-file1.txt", "folder1-file1");
        assertContent(targetZipFile, "folder-one/folder1-file2.txt", "folder1-file2");
        assertContent(targetZipFile, "folder-two/folder2-file1.txt", "folder2-file1");
        assertContent(targetZipFile, "folder-two/folder2-file2.txt", "folder2-file2");
    }

    @Test
    public void shouldZipMultipleFolderContentsWhenNotExcludingRootDirectory() throws IOException {

        File folderOne = temporaryFolder.newFolder("folder1");
        FileUtils.writeStringToFile(new File(folderOne, "folder1-file1.txt"), "folder1-file1");
        FileUtils.writeStringToFile(new File(folderOne, "folder1-file2.txt"), "folder1-file2");

        File folderTwo = temporaryFolder.newFolder("folder2");
        FileUtils.writeStringToFile(new File(folderTwo, "folder2-file1.txt"), "folder2-file1");
        FileUtils.writeStringToFile(new File(folderTwo, "folder2-file2.txt"), "folder2-file2");

        File targetZipFile = temporaryFolder.newFile("final2.zip");

        ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(targetZipFile, false);
        zipBuilder.add("folder-one", folderOne);
        zipBuilder.add("folder-two", folderTwo);
        zipBuilder.done();

        assertContent(targetZipFile, "folder-one/folder1/folder1-file1.txt", "folder1-file1");
        assertContent(targetZipFile, "folder-one/folder1/folder1-file2.txt", "folder1-file2");
        assertContent(targetZipFile, "folder-two/folder2/folder2-file1.txt", "folder2-file1");
        assertContent(targetZipFile, "folder-two/folder2/folder2-file2.txt", "folder2-file2");
    }

    @Test
    public void shouldPreserveFileTimestampWhileGeneratingTheZipFile() throws Exception {
        File file = temporaryFolder.newFile("foo.txt");
        file.setLastModified(1297989100000L); // Set this to any date in the past which is greater than the epoch
        File zip = zipUtil.zip(file, temporaryFolder.newFile("foo.zip"), Deflater.DEFAULT_COMPRESSION);

        ZipFile actualZip = new ZipFile(zip.getAbsolutePath());
        ZipEntry entry = actualZip.getEntry(file.getName());

        assertThat(entry.getTime(), is(file.lastModified()));
    }

    @Test
    public void shouldThrowUpWhileTryingToUnzipIfAnyOfTheFilePathsInArchiveHasAPathContainingDotDotSlashPath() throws URISyntaxException, IOException {
        try {
            zipUtil.unzip(new File(getClass().getResource("/archive_traversal_attack.zip").toURI()), destDir);
            fail("squash.zip is capable of causing archive traversal attack and hence should not be allowed.");
        } catch (IllegalPathException e) {
            assertThat(e.getMessage(), is("File ../2.txt is outside extraction target directory"));
        }
    }

    private void assertContent(File targetZipFile, String file, String expectedContent) throws IOException {
        ZipFile actualZip = new ZipFile(targetZipFile);
        ZipEntry entry = actualZip.getEntry(file);
        assertThat(entry, is(notNullValue()));
        assertThat(IOUtils.toString(actualZip.getInputStream(entry)), is(expectedContent));
    }

    private void assertIsDirectory(File file) {
        assertThat("File " + file.getPath() + " should exist", file.exists(), is(true));
        assertThat("File " + file.getPath() + " should be a directory", file.isDirectory(), is(true));
    }
}
