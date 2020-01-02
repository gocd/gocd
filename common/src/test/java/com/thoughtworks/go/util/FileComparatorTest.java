/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import java.io.File;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class FileComparatorTest {

    private File file1 = new FileStub("a", false);
    private File file2 = new FileStub("b", false);
    private File folder1 = new FileStub("c", true);
    private File folder2 = new FileStub("d", true);

    private FileComparator fileComparator = new FileComparator();

    @Test
    public void shouldBeAlphabeticForSameType() {
        assertThat(fileComparator.compare(file1, file2) < 0, is(true));
        assertThat(fileComparator.compare(folder1, folder2) < 0, is(true));
    }

    @Test
    public void folderShouldBeLessThanFile() {
        assertThat(fileComparator.compare(file1, folder1) > 0, is(true));
    }

    private static class FileStub extends File {
        private boolean directory;

        public FileStub(String name, boolean isDirectory) {
            super(name);
            directory = isDirectory;
        }

        @Override
        public boolean isDirectory() {
            return directory;
        }

        @Override
        public boolean isFile() {
            return !directory;
        }
    }

}
