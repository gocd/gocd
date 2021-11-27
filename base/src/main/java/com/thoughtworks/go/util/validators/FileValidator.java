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
package com.thoughtworks.go.util.validators;

import com.thoughtworks.go.util.ConfigDirProvider;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Objects;

import static java.text.MessageFormat.format;

public class FileValidator implements Validator {
    private final String fileName;
    private final String srcDir;
    private final String destDir;
    private final boolean shouldReplace;

    public static FileValidator defaultFile(final String fileName) {
        return defaultFile(fileName, true);
    }

    public static FileValidator defaultFile(final String fileName, final boolean shouldReplace) {
        String userDir = System.getProperty("user.dir");
        return new FileValidator(fileName, "/defaultFiles", userDir, shouldReplace);
    }

    public static FileValidator configFile(String fileName, final ConfigDirProvider configDirProvider) {
        return configFile(fileName, false, configDirProvider);
    }

    public static FileValidator configFileAlwaysOverwrite(String fileName, final ConfigDirProvider configDirProvider) {
        return configFile(fileName, true, configDirProvider);
    }

    private static FileValidator configFile(String fileName, boolean shouldReplace, final ConfigDirProvider configDirProvider) {
        return new FileValidator(fileName, "/defaultFiles/config", configDirProvider.getConfigDir(), shouldReplace);
    }

    private FileValidator(String fileName, String srcDir, String destDir, boolean shouldReplace) {
        this.fileName = fileName;
        this.srcDir = srcDir;
        this.destDir = destDir;
        this.shouldReplace = shouldReplace;
    }

    @Override
    public Validation validate(Validation validation) {
        File file = new File(destDir, fileName);
        if (!shouldReplace && file.exists()) {
            if (file.canWrite() && file.canRead()) {
                return Validation.SUCCESS;
            } else {
                String message = format("File {0} is not readable or writeable.", file.getAbsolutePath());
                return validation.addError(new RuntimeException(message));
            }
        }
        // Pull out the file from the class path
        try (InputStream input = this.getClass().getResourceAsStream(srcDir + "/" + fileName)) {
            if (input == null) {
                String message = format("Resource {0}/{1} does not exist in the classpath", srcDir, fileName);
                return validation.addError(new RuntimeException(message));
            }
            // Make sure the dir exists
            file.getParentFile().mkdirs();
            try (FileOutputStream output = new FileOutputStream(file)) {
                IOUtils.copy(input, output);
            }
        } catch (Exception e) {
            return handleExceptionDuringFileHandling(validation, e);
        }
        return Validation.SUCCESS;
    }

    private Validation handleExceptionDuringFileHandling(Validation validation, Exception e) {
        String message = format("Error trying to validate file {0}: {1}", fileName, e);
        return validation.addError(new RuntimeException(message));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FileValidator that = (FileValidator) o;

        if (shouldReplace != that.shouldReplace) return false;
        if (!Objects.equals(fileName, that.fileName)) return false;
        if (!Objects.equals(srcDir, that.srcDir)) return false;
        return Objects.equals(destDir, that.destDir);
    }

    @Override
    public int hashCode() {
        int result = fileName != null ? fileName.hashCode() : 0;
        result = 31 * result + (srcDir != null ? srcDir.hashCode() : 0);
        result = 31 * result + (destDir != null ? destDir.hashCode() : 0);
        result = 31 * result + (shouldReplace ? 1 : 0);
        return result;
    }
}
