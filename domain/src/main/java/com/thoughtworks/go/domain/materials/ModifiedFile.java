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
package com.thoughtworks.go.domain.materials;

import com.thoughtworks.go.domain.PersistentObject;

import java.io.Serializable;

public class ModifiedFile extends PersistentObject implements Serializable {

    @SuppressWarnings("PMD.UnusedPrivateField") // used by hibernate
    private long modificationId;

    private String fileName;
    private String folderName;
    private ModifiedAction action = ModifiedAction.unknown;
    public static final int MAX_NAME_LENGTH = 1024;

    public ModifiedFile() {
    }

    public ModifiedFile(String fileName, String folderName, ModifiedAction action) {
        this.fileName = truncateNameIfNecessary(fileName);
        this.folderName = folderName;
        this.action = action;
        truncateNameIfNecessary(fileName);
    }

    public void setModificationId(long modificationId) {
        this.modificationId = modificationId;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ModifiedFile)) {
            return false;
        }

        ModifiedFile mod = (ModifiedFile) o;

        boolean folderNamesAreEqual = (folderName != null)
                ? folderName.equals(mod.folderName)
                : (mod.folderName == null);

        return (action.equals(mod.action)
                && fileName.equals(mod.fileName)
                && folderNamesAreEqual);
    }

    @Override
    public int hashCode() {
        int code = 1;
        if (fileName != null) {
            code += fileName.hashCode() * 2;
        }
        if (folderName != null) {
            code += folderName.hashCode() * 5;
        }
        if (action != null) {
            code += action.hashCode() * 7;
        }
        return code;
    }

    public String getFileName() {
        return fileName;
    }

    public ModifiedAction getAction() {
        return action;
    }

    @Override
    public String toString() {
        return this.fileName;
    }

    private static String truncateNameIfNecessary(String fileName) {
        return fileName.length() > MAX_NAME_LENGTH ? fileName.substring(0, MAX_NAME_LENGTH) : fileName;
    }

}
