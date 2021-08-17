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
package com.thoughtworks.go.config.materials;

import com.thoughtworks.go.domain.materials.MaterialConfig;
import com.thoughtworks.go.helper.MaterialConfigsMother;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.regex.Pattern;

import static com.thoughtworks.go.helper.MaterialConfigsMother.tfs;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class IgnoreTest {
    private MaterialConfig hgMaterialConfig = MaterialConfigsMother.hgMaterialConfig();
    private String separator = File.separator;

    @Test
    public void shouldIncludeWhenTheTextDoesnotMatchDocumentUnderRoot() {
        IgnoredFiles ignore = new IgnoredFiles("a.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "b.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.doc1"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "B" + separator + "a.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "a.doc"), is(false));
    }

    @Test
    public void shouldIgnoreWhenTheTextDoesMatchDocumentUnderRoot() {
        IgnoredFiles ignore = new IgnoredFiles("a.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.doc"), is(true));
    }

    @Test
    public void shouldIncludeWhenTheTextUnderRootIsNotADocument() {
        IgnoredFiles ignore = new IgnoredFiles("*.doc");


        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.pdf"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.doc.aaa"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "a.doc"), is(false));
    }

    @Test
    public void shouldIgnoreWhenTheTextUnderRootIsADocument() {
        IgnoredFiles ignore = new IgnoredFiles("*.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.doc"), is(true));
    }

    @Test
    public void shouldIncludeWhenTextIsNotDocumentInChildOfRootFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("*/*.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "B" + separator + "c.doc"), is(false));
    }


    @Test
    public void shouldIgnoreWhenTextIsDocumentInChildOfRootFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("*/*.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "a.doc"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "B" + separator + "c.doc"), is(true));
    }

    @Test
    public void shouldNormalizeRegex() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("*\\*.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "a.doc"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "B" + separator + "c.doc"), is(true));
    }


    @Test
    public void shouldIncludeWhenTextIsNotADocumentInAnyFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/*.doc");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.pdf"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "B" + separator + "a.pdf"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "B" + separator + "a.pdf"), is(false));
    }

    @Test
    public void shouldIgnoreWhenTextIsADocumentInAnyFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/*.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "a.doc"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "a.doc"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "B" + separator + "a.doc"), is(true));
    }

    @Test
    public void shouldIgnoreWhenTextIsADocumentInAnyFolderWhenDirectoryWildcardNotInTheBegining() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("foo*/**/*.doc");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "foo-hi/bar/a.doc"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "bar/baz/b.doc"), is(false));
    }

    @Test
    public void shouldIgnoreWhenTextIsADocumentInAnyFolderWhenDirectoryWildcardNotInTheBeginingAndTerminatesInWildcard() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/foo*/**/*");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "foo-hi/bar/a.doc"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "bar/baz/b.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "hello-world/woods/foo-hi/bar/a.doc"), is(true));
    }

    @Test
    public void shouldIncludeIfTheTextIsNotADocumentInTheSpecifiedFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("ROOTFOLDER/*.doc");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "shouldNotBeIgnored.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "ANYFOLDER" + separator + "shouldNotBeIgnored.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "ROOTFOLDER" + separator + ""
                + "ANYFOLDER" + separator + "shouldNotBeIgnored.doc"), is(false));
    }

    @Test
    public void shouldIncludIgnoreIfTextIsADocumentInTheSpecifiedFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("ROOTFOLDER/*.doc");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "ROOTFOLDER" + separator + "a.doc"), is(true));
    }

    @Test
    public void shouldIncludeIfTextIsNotUnderAGivenFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/DocumentFolder/*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "shouldNotBeIgnored.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig,
                "A" + separator + "DocumentFolder" + separator + "B" + separator + "d.doc"), is(false));
    }

    @Test
    public void shouldIgnoreIfTextIsUnderAGivenFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/DocumentFolder/*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "DocumentFolder" + separator + "d.doc"), is(true));
    }

    @Test
    public void shouldIncludeIfTextIsNotUnderAFolderMatchingTheGivenFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/*Test*/*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "A" + separator + "shouldNotBeIgnored.doc"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "B/NotIgnored" + separator + "isIgnored"), is(false));
    }

    @Test
    public void shouldIgnoreIfTextIsUnderAFolderMatchingTheGivenFolder() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("**/*Test*/*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "B" + separator + "SomethingTestThis" + separator + "isIgnored"),
                is(true));
    }

    @Test
    public void shouldIgnoreEverythingUnderAFolderWithWildcards() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("Test/**/*.*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "foo.txt"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "subdir" + separator + "foo.txt"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "subdir" + separator + "subdir" + separator + "foo.txt"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "subdir" + separator + "subdir" + separator + "foo"), is(false));
    }

    @Test
    public void shouldIgnoreEverythingUnderAFolderWithWildcardsWithoutExtension() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("Test/**/*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "foo.txt"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "subdir" + separator + "foo.txt"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "subdir" + separator + "subdir" + separator + "foo.txt"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "Test" + separator + "subdir" + separator + "subdir" + separator + "foo"), is(true));
    }

    @Test
    public void shouldSkipDiot() throws Exception {
        IgnoredFiles ignore = new IgnoredFiles("helper/*.*");

        assertThat(ignore.shouldIgnore(hgMaterialConfig, "helper" + separator + "configuration_reference.html"), is(true));
        assertThat(ignore.shouldIgnore(hgMaterialConfig,
                "helper" + separator + "conf-docs" + separator + "configuration_reference.html"), is(false));
        assertThat(ignore.shouldIgnore(hgMaterialConfig,
                "helper" + separator + "resources" + separator + "images" + separator + "cruise" + separator
                        + "dependent_build.png"),
                is(false));
    }

    @Test
    public void shouldAddErrorToItsErrorCollection() {
        IgnoredFiles ignore = new IgnoredFiles("helper/*.*");
        ignore.addError("pattern", "not allowed");
        assertThat(ignore.errors().on("pattern"), is("not allowed"));
    }

    @Test
    public void shouldEscapeAllValidSpecialCharactersInPattern() {//see mingle #5700
        hgMaterialConfig = tfs();
        IgnoredFiles ignore = new IgnoredFiles("$/tfs_repo/Properties/*.*");
        assertThat(ignore.shouldIgnore(hgMaterialConfig, "$/tfs_repo" + separator + "Properties" + separator + "AssemblyInfo.cs"), is(true));
    }

    @Test
    public void understandPatternPunct() {
        assertThat(Pattern.matches("a\\.doc", "a.doc"), is(true));
        assertThat(Pattern.matches("\\p{Punct}", "*"), is(true));
        assertThat(Pattern.matches("\\p{Punct}", "{"), is(true));
        assertThat(Pattern.matches("\\p{Punct}", "]"), is(true));
        assertThat(Pattern.matches("\\p{Punct}", "-"), is(true));
        assertThat(Pattern.matches("\\p{Punct}", "."), is(true));
    }


}
