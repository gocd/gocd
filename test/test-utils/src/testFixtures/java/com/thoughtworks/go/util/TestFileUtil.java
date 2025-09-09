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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class TestFileUtil {
    public static File createTestFile(File testFolder, String path) throws Exception {
        File subfile = new File(testFolder, path);
        subfile.createNewFile();
        subfile.deleteOnExit();
        return subfile;
    }

    public static File createTestFolder(File parent, String folderName) {
        File subDir = new File(parent, folderName);
        subDir.mkdirs();
        subDir.deleteOnExit();
        return subDir;
    }

    public static File resourceToTempFile(String resourcePath) throws Exception {
        return resourceToTempPath(resourcePath).toFile();
    }

    public static Path resourceToTempPath(String resourcePath) throws IOException {
        Path tempPath = Files.createTempFile(Path.of(resourcePath).getFileName().toString(), null);
        try (InputStream is = openStream(resourcePath)) {
            Files.write(tempPath, is.readAllBytes());
        }
        return tempPath;
    }

    public static String resourceToString(String resourcePath) throws IOException {
        try (InputStream inputStream = openStream(resourcePath)) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static InputStream openStream(String resourcePath) {
        return Objects.requireNonNull(TestFileUtil.class.getResourceAsStream(resourcePath), "Could not find resource at " + resourcePath);
    }
}
