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
package com.thoughtworks.go.util.validators;

import java.io.File;
import java.io.IOException;
import java.util.zip.ZipInputStream;

import com.thoughtworks.go.util.SystemEnvironment;

public class DatabaseValidator extends ZipValidator {

    @Override
    public Validation validate(Validation validation) {
        File destDir = new File(new SystemEnvironment().getPropertyImpl("user.dir"), "db");
        destDir.mkdirs();
        try {
            unzip(new ZipInputStream(this.getClass().getResourceAsStream("/defaultFiles/h2deltas.zip")), destDir);
        } catch (IOException e) {
            validation.addError(e);
        }

        File dbFile = new File(destDir, "h2db/cruise.h2.db");

        if (dbFile.exists() && dbFile.canWrite() && dbFile.canRead()) {
            return Validation.SUCCESS;
        } else {
            try {
                unzip(new ZipInputStream(this.getClass().getResourceAsStream("/defaultFiles/h2db.zip")), destDir);
            } catch (Exception e) {
                return validation.addError(e);
            }
        }
        return validation;
    }

}
