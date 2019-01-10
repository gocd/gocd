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

package com.thoughtworks.go.domain;

import com.thoughtworks.go.util.DirectoryScanner;
import com.thoughtworks.go.util.FileUtil;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

public class DirectoryScannerTest {
    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File baseFolder;
    private DirectoryScanner ds = new DirectoryScanner();

    private File getBaseDir() {
        return new File(baseFolder, "tmp");
    }

    @Before
	public void setUp() throws Exception {
        baseFolder = temporaryFolder.newFolder("test");
        File tmpFolder = new File(baseFolder, "tmp");
        tmpFolder.mkdirs();
        new File(tmpFolder, "alpha").mkdirs();
    }

    @After
	public void tearDown() {
        FileUtils.deleteQuietly(baseFolder);
    }

    @Test
    public void shouldScanEmptyFolder() {
        ds.setBasedir(getBaseDir()).setIncludes(new String[]{"alpha"}).scan();

        compareFiles(ds, new String[]{}, new String[]{"alpha"});
    }

    @Test
    public void shouldScanAllSubFoldersAndFiles() throws IOException {
        String[] data = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir()).setIncludes(new String[]{"alpha/"}).scan();

        compareFiles(ds, data,
                new String[]{"alpha", "alpha/beta", "alpha/beta/gamma"});
    }



    @Test
	public void shouldScanEmptyIncludeFiles() throws IOException {
        String[] files = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), files);

        ds.setBasedir(getBaseDir()).scan();

        compareFiles(ds, files, new String[]{"", "alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
	public void testFullPathMatchesCaseSensitive() throws IOException {
        String[] data = {"alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);
        ds.setBasedir(getBaseDir()).setIncludes(new String[]{"alpha/beta/gamma/GAMMA.XML"}).scan();
        compareFiles(ds, new String[]{}, new String[]{});
    }

    @Test
	public void testFullPathMatchesCaseInsensitive() throws IOException {
        String[] data = {"alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);
        ds.setCaseSensitive(false)
                .setBasedir(getBaseDir())
                .setIncludes(new String[]{"alpha/beta/gamma/GAMMA.XML"})
                .scan();
        compareFiles(ds, data, new String[]{});
    }

    @Test
	public void test2ButCaseInsensitive() throws IOException {
        String[] data = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir()).setIncludes(new String[]{"ALPHA/"}).setCaseSensitive(false).scan();

        compareFiles(ds, data, new String[]{"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    // father and child pattern test
    @Test
	public void testOrderOfIncludePatternsIrrelevant() throws IOException {
        String[] expectedFiles = {"alpha/beta/beta.xml",
                "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), expectedFiles);
        String[] expectedDirectories = {"alpha/beta", "alpha/beta/gamma"};

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/be?a/**", "alpha/beta/gamma/"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
        // redo the test, but the 2 include patterns are inverted
        ds = this.ds;
        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/beta/gamma/", "alpha/be?a/**"});
        ds.scan();
        compareFiles(ds, expectedFiles, expectedDirectories);
    }

    @Test
	public void testPatternsDifferInCaseScanningSensitive() throws IOException {
        String[] expectedFiles = {"alpha/beta/beta.xml",
                "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), expectedFiles);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/", "ALPHA/"});
        ds.scan();
        compareFiles(ds, new String[]{"alpha/beta/beta.xml",
                "alpha/beta/gamma/gamma.xml"},
                new String[]{"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
	public void testPatternsDifferInCaseScanningInsensitive() throws IOException {
        String[] files = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), files);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/", "ALPHA/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, files,
                new String[]{"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
	public void shouldNotMatchWhenHasTrailingSpace() throws IOException {
        String[] files = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), files);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/beta ",});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, new String[]{}, new String[]{});
    }

    @Test
	public void testFullpathDiffersInCaseScanningSensitive() throws IOException {
        String[] data = {"alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "alpha/beta/gamma/gamma.xml",
                "alpha/beta/gamma/GAMMA.XML"
        });
        ds.scan();
        compareFiles(ds, data, new String[]{});
    }

    @Test
	public void testFullpathDiffersInCaseScanningInsensitive() throws IOException {
        String[] data = {"alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "alpha/beta/gamma/gamma.xml",
                "alpha/beta/gamma/GAMMA.XML"
        });
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, data, new String[]{});
    }

    @Test
	public void testParentDiffersInCaseScanningSensitive() throws IOException {
        String[] data = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/", "ALPHA/beta/"});
        ds.scan();
        compareFiles(ds, data, new String[]{"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
	public void testParentDiffersInCaseScanningInsensitive() throws IOException {
        String[] data = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{"alpha/", "ALPHA/beta/"});
        ds.setCaseSensitive(false);
        ds.scan();
        compareFiles(ds, data, new String[]{"alpha", "alpha/beta", "alpha/beta/gamma"});
    }

    @Test
	public void testExcludeOneFile() throws IOException {
        String[] data = {"alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "**/*.xml"
        });
        ds.setExcludes(new String[]{
                "alpha/beta/b*xml"
        });
        ds.scan();
        compareFiles(ds, data,
                new String[]{});
    }

    @Test
	public void testExcludeHasPrecedence() throws IOException {
        String[] data = {"alpha/beta/gamma/gamma.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "alpha/**"
        });
        ds.setExcludes(new String[]{
                "alpha/**"
        });
        ds.scan();
        compareFiles(ds, new String[]{}, new String[]{});

    }

    @Test
	public void testAlternateIncludeExclude() {
        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "alpha/**",
                "alpha/beta/gamma/**"
        });
        ds.setExcludes(new String[]{
                "alpha/beta/**"
        });
        ds.scan();
        compareFiles(ds, new String[]{},
                new String[]{"alpha"});

    }

    @Test
	public void testAlternateExcludeInclude() throws IOException {
        new File(getBaseDir(), "emtpyFolder").mkdirs();

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "emtpyFolder/*.*"
        });
        ds.scan();
        compareFiles(ds, new String[]{}, new String[]{});
    }

    @Test
    public void shouldNotUploadTheSubFolderWhenItDoesNotContainDot() {
        new File(getBaseDir(), "emtpyFolder/emptySubFolder").mkdirs();

        ds.setBasedir(getBaseDir());
        ds.setIncludes(new String[]{
                "emtpyFolder/**/*.*"
        });
        ds.scan();
        compareFiles(ds, new String[]{}, new String[]{});
    }

    @Test
    public void testShouldSearchFileUnderTheSandBox() throws IOException {
        File file = new File(baseFolder, "another/another.xml");
        file.getParentFile().mkdirs();
        file.createNewFile();

        String[] data = {"alpha/beta/beta.xml", "alpha/beta/gamma/gamma.xml", "delta/delta.xml"};
        FileUtil.createFilesByPath(getBaseDir(), data);

        File sandbox = getBaseDir();
        ds.setBasedir(sandbox);
        ds.setIncludes(new String[]{new File(baseFolder, "another").getAbsolutePath() + "/**/*.xml"});
        ds.scan();

        compareFiles(ds, new String[]{}, new String[]{});
    }

    private void compareFiles(DirectoryScanner ds, String[] expectedFiles,
                              String[] expectedDirectories) {
        String includedFiles[] = ds.getIncludedFiles();
        String includedDirectories[] = ds.getIncludedDirectories();
        assertThat("expected " + Arrays.asList(expectedFiles) + " but " + Arrays.asList(includedFiles)
                , includedFiles.length, is(expectedFiles.length));
        assertThat("directories present: ", includedDirectories.length, is(expectedDirectories.length));

        TreeSet files = new TreeSet();
        for (int counter = 0; counter < includedFiles.length; counter++) {
            files.add(includedFiles[counter].replace(File.separatorChar, '/'));
        }

        TreeSet directories = new TreeSet();
        for (int counter = 0; counter < includedDirectories.length; counter++) {
            directories.add(includedDirectories[counter]
                    .replace(File.separatorChar, '/'));
        }

        String currentfile;
        Iterator i = files.iterator();
        int counter = 0;
        while (i.hasNext()) {
            currentfile = (String) i.next();
            assertThat(currentfile, is(expectedFiles[counter]));
            counter++;
        }
        String currentdirectory;
        Iterator dirit = directories.iterator();
        counter = 0;
        while (dirit.hasNext()) {
            currentdirectory = (String) dirit.next();
            assertThat(currentdirectory, is(expectedDirectories[counter]));
            counter++;
        }
    }
}
