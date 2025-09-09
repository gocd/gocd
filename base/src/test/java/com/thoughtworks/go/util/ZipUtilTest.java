/*
 * Copyright Thoughtworks, Inc.
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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class ZipUtilTest {
    @TempDir
    Path tempDir;

    private File srcDir;
    private File destDir;
    private ZipUtil zipUtil;
    private File childDir1;
    private File file1;
    private File file2;
    private File zipFile;
    private File emptyDir;

    private static String fileContent(File file) throws IOException {
        return Files.readString(file.toPath());
    }

    @BeforeEach
    void setUp() throws Exception {
        srcDir = createDirectoryInTempDir("_test1");
        destDir = createDirectoryInTempDir("_test2");
        emptyDir = new File(srcDir, "_emptyDir");
        emptyDir.mkdir();
        childDir1 = new File(srcDir, "_child1");
        childDir1.mkdir();
        file1 = new File(srcDir, "_file1");
        Files.writeString(file1.toPath(), "_file1", UTF_8);
        file2 = new File(childDir1, "_file2");
        Files.writeString(file2.toPath(), "_file2", UTF_8);
        zipUtil = new ZipUtil();
    }

    @Test
    void shouldZipFileAndUnzipIt() throws IOException {
        zipFile = zipUtil.zip(srcDir, createFileInTempDir(), Deflater.NO_COMPRESSION);
        assertThat(zipFile.isFile()).isTrue();

        zipUtil.unzip(zipFile, destDir);
        File baseDir = new File(destDir, srcDir.getName());

        assertIsDirectory(new File(baseDir, emptyDir.getName()));
        assertIsDirectory(new File(baseDir, childDir1.getName()));

        File actual1 = new File(baseDir, file1.getName());
        assertThat(actual1.isFile()).isTrue();
        assertThat(fileContent(actual1)).isEqualTo(fileContent(file1));

        File actual2 = new File(baseDir, childDir1.getName() + File.separator + file2.getName());
        assertThat(actual2.isFile()).isTrue();
        assertThat(fileContent(actual2)).isEqualTo(fileContent(file2));
    }

    private File createFileInTempDir() throws IOException {
        return Files.createFile(tempDir.resolve("file.txt")).toFile();
    }

    @Test
    void shouldZipFileContentsAndUnzipIt() throws IOException {
        zipFile = zipUtil.zip(srcDir, createFileInTempDir(), Deflater.NO_COMPRESSION);
        assertThat(zipFile.isFile()).isTrue();

        zipUtil.unzip(zipFile, destDir);
        File baseDir = new File(destDir, srcDir.getName());

        assertIsDirectory(new File(baseDir, emptyDir.getName()));
        assertIsDirectory(new File(baseDir, childDir1.getName()));

        File actual1 = new File(baseDir, file1.getName());
        assertThat(actual1.isFile()).isTrue();
        assertThat(fileContent(actual1)).isEqualTo(fileContent(file1));

        File actual2 = new File(baseDir, childDir1.getName() + File.separator + file2.getName());
        assertThat(actual2.isFile()).isTrue();
        assertThat(fileContent(actual2)).isEqualTo(fileContent(file2));
    }

    @Test
    void shouldZipFileContentsOnly() throws IOException {
        zipFile = zipUtil.zipFolderContents(srcDir, createFileInTempDir(), Deflater.NO_COMPRESSION);
        assertThat(zipFile.isFile()).isTrue();

        zipUtil.unzip(zipFile, destDir);

        assertIsDirectory(new File(destDir, emptyDir.getName()));
        assertIsDirectory(new File(destDir, childDir1.getName()));

        File actual1 = new File(destDir, file1.getName());
        assertThat(actual1.isFile()).isTrue();
        assertThat(fileContent(actual1)).isEqualTo(fileContent(file1));

        File actual2 = new File(destDir, childDir1.getName() + File.separator + file2.getName());
        assertThat(actual2.isFile()).isTrue();
        assertThat(fileContent(actual2)).isEqualTo(fileContent(file2));
    }

    @Test
    @EnabledOnOs(OS.LINUX)
    void shouldZipFileWhoseNameHasSpecialCharactersOnLinux() throws IOException {
        File specialFile = new File(srcDir, "$`#?@!()?-_{}^'~.+=[];,a.txt");
        Files.writeString(specialFile.toPath(), "specialFile", UTF_8);

        zipFile = zipUtil.zip(srcDir, createFileInTempDir(), Deflater.NO_COMPRESSION);
        zipUtil.unzip(zipFile, destDir);
        File baseDir = new File(destDir, srcDir.getName());

        File actualSpecialFile = new File(baseDir, specialFile.getName());
        assertThat(actualSpecialFile.isFile()).isTrue();
        assertThat(fileContent(actualSpecialFile)).isEqualTo(fileContent(specialFile));
    }

    @Test
    void shouldReadContentsOfAFileWhichIsInsideAZip() throws Exception {
        Files.writeString(new File(srcDir, "some-file.txt").toPath(), "some-text-here", UTF_8);
        zipFile = zipUtil.zip(srcDir, createFileInTempDir(), Deflater.NO_COMPRESSION);

        try (ZipInputStream zip = new ZipInputStream(new FileInputStream(zipFile))) {
            String someStuff = zipUtil.getFileContentInsideZip(zip, "some-file.txt");
            assertThat(someStuff).isEqualTo("some-text-here");
        }
    }

    @Test
    void shouldZipMultipleFolderContentsAndExcludeRootDirectory() throws IOException {
        File folderOne = createDirectoryInTempDir("a-folder1");
        Files.writeString(new File(folderOne, "folder1-file1.txt").toPath(), "folder1-file1", UTF_8);
        Files.writeString(new File(folderOne, "folder1-file2.txt").toPath(), "folder1-file2", UTF_8);

        File folderTwo = createDirectoryInTempDir("a-folder2");
        Files.writeString(new File(folderTwo, "folder2-file1.txt").toPath(), "folder2-file1", UTF_8);
        Files.writeString(new File(folderTwo, "folder2-file2.txt").toPath(), "folder2-file2", UTF_8);

        File targetZipFile = tempDir.resolve("final1.zip").toFile();

        ZipBuilder zipBuilder = zipUtil.zipContentsOfMultipleFolders(targetZipFile, true);
        zipBuilder.add("folder-one", folderOne);
        zipBuilder.add("folder-two", folderTwo);
        zipBuilder.done();

        assertContent(targetZipFile, "folder-one/folder1-file1.txt", "folder1-file1");
        assertContent(targetZipFile, "folder-one/folder1-file2.txt", "folder1-file2");
        assertContent(targetZipFile, "folder-two/folder2-file1.txt", "folder2-file1");
        assertContent(targetZipFile, "folder-two/folder2-file2.txt", "folder2-file2");
    }

    private File createDirectoryInTempDir(String folderName) throws IOException {
        return Files.createDirectory(tempDir.resolve(folderName)).toFile();
    }

    @Test
    void shouldZipMultipleFolderContentsWhenNotExcludingRootDirectory() throws IOException {

        File folderOne = createDirectoryInTempDir("folder1");
        Files.writeString(new File(folderOne, "folder1-file1.txt").toPath(), "folder1-file1", UTF_8);
        Files.writeString(new File(folderOne, "folder1-file2.txt").toPath(), "folder1-file2", UTF_8);

        File folderTwo = createDirectoryInTempDir("folder2");
        Files.writeString(new File(folderTwo, "folder2-file1.txt").toPath(), "folder2-file1", UTF_8);
        Files.writeString(new File(folderTwo, "folder2-file2.txt").toPath(), "folder2-file2", UTF_8);

        File targetZipFile = tempDir.resolve("final2.zip").toFile();

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
    void shouldPreserveFileTimestampWhileGeneratingTheZipFile() throws Exception {
        File file = createFileInTempDir();
        file.setLastModified(1297989100000L); // Set this to any date in the past which is greater than the epoch
        File zip = zipUtil.zip(file, tempDir.resolve("foo.zip").toFile(), Deflater.DEFAULT_COMPRESSION);

        try (ZipFile actualZip = new ZipFile(zip.getAbsolutePath())) {
            ZipEntry entry = actualZip.getEntry(file.getName());
            assertThat(entry.getTime()).isEqualTo(file.lastModified());
        }
    }

    @Test
    void shouldThrowUpWhileTryingToUnzipIfAnyOfTheFilePathsInArchiveHasAPathContainingDotDotSlashPath() throws URISyntaxException, IOException {
        try {
            zipUtil.unzip(new File(getClass().getResource("/archive_traversal_attack.zip").toURI()), destDir);
            fail("squash.zip is capable of causing archive traversal attack and hence should not be allowed.");
        } catch (IllegalPathException e) {
            assertThat(e.getMessage()).isEqualTo("File ../2.txt is outside extraction target directory");
        }
    }

    @Test
    void shouldReadContentFromFileInsideZip() throws IOException {
        try (ZipInputStream zip = new ZipInputStream(getClass().getResourceAsStream("/dummy-plugins.zip"))) {
            String contents = zipUtil.getFileContentInsideZip(zip, "version.txt");
            assertThat(contents).isEqualTo("13.3.0(17222-4c7fabcb9c9e9c)");
        }
    }

    @Test
    void shouldReturnNullIfTheFileByTheNameDoesNotExistInsideZip() throws IOException {
        try (ZipInputStream zip = new ZipInputStream(getClass().getResourceAsStream("/dummy-plugins.zip"))) {
            String contents = zipUtil.getFileContentInsideZip(zip, "does_not_exist.txt");
            assertThat(contents).isNull();
        }
    }

    private void assertContent(File targetZipFile, String file, String expectedContent) throws IOException {
        try (ZipFile actualZip = new ZipFile(targetZipFile)) {
            ZipEntry entry = actualZip.getEntry(file);
            assertThat(entry).isNotNull();
            try (InputStream entryStream = actualZip.getInputStream(entry)) {
                assertThat(new String(entryStream.readAllBytes(), UTF_8)).isEqualTo(expectedContent);
            }
        }
    }

    private void assertIsDirectory(File file) {
        assertThat(file.exists()).as("File " + file.getPath() + " should exist").isTrue();
        assertThat(file.isDirectory()).as("File " + file.getPath() + " should be a directory").isTrue();
    }
}
