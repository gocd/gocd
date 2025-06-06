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

import org.apache.commons.io.FilenameUtils;

import java.io.File;

public class FilenameUtil {
    public static boolean isNormalizedDirectoryPathInsideNormalizedParentDirectory(String parent, String subdirectory) {
        final String normalizedParentPath = FilenameUtils.normalize(parent + File.separator);
        final String normalizedSubDirPath = FilenameUtils.normalize(subdirectory + File.separator);
        return !normalizedParentPath.isBlank() && !normalizedSubDirPath.isBlank() && normalizedSubDirPath.startsWith(normalizedParentPath);
    }
    public static boolean isNormalizedPathOutsideWorkingDir(String path) {
        final String normalize = FilenameUtils.normalize(path);
        final String prefix = FilenameUtils.getPrefix(normalize);
        return normalize != null && (prefix == null || prefix.isBlank());
    }

    public static String join(File defaultWorkingDir, String actualFileToUse) {
        if (actualFileToUse == null) {
            return FilenameUtils.separatorsToUnix(defaultWorkingDir.getPath());
        }
        return applyBaseDirIfRelativeAndNormalize(defaultWorkingDir, new File(actualFileToUse));
    }

    public static String applyBaseDirIfRelativeAndNormalize(File baseDir, File actualFileToUse) {
        return FilenameUtils.separatorsToUnix(applyBaseDirIfRelative(baseDir, actualFileToUse).getPath());
    }

    public static File applyBaseDirIfRelative(File baseDir, File actualFileToUse) {
        if (actualFileToUse == null) {
            return baseDir;
        }
        if (actualFileToUse.isAbsolute()) {
            return actualFileToUse;
        }

        if (baseDir.getPath().isBlank()) {
            return actualFileToUse;
        }

        return new File(baseDir, actualFileToUse.getPath());

    }
}


