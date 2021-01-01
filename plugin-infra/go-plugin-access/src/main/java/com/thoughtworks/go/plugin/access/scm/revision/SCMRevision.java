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
package com.thoughtworks.go.plugin.access.scm.revision;

import com.thoughtworks.go.plugin.access.scm.exceptions.InvalidSCMRevisionDataException;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SCMRevision {
    private static Pattern DATA_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");
    private static final String DATA_KEY_EMPTY_MESSAGE = "Key names cannot be null or empty.";

    private String revision;
    private Date timestamp;
    private String user;
    private String revisionComment;
    private Map<String, String> data;
    private List<ModifiedFile> modifiedFiles;

    public SCMRevision() {
    }

    public SCMRevision(String revision, Date timestamp, String user, String revisionComment, Map<String, String> data, List<ModifiedFile> modifiedFiles) {
        this.revision = revision;
        this.timestamp = timestamp;
        this.user = user;
        this.revisionComment = revisionComment;
        validateDataKeys(data);
        this.data = data;
        this.modifiedFiles = modifiedFiles;
    }

    private void validateDataKeys(Map<String, String> data) {
        if (data != null) {
            for (String key : data.keySet()) {
                validateDataKey(key);
            }
        }
    }

    public String getRevision() {
        return revision;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public String getUser() {
        return user;
    }

    public String getRevisionComment() {
        return revisionComment;
    }

    public Map<String, String> getData() {
        return data;
    }

    public String getDataFor(String key) {
        return data.get(key);
    }

    public void addData(String key, String value) throws InvalidSCMRevisionDataException {
        validateDataKey(key);
        data.put(key, value);
    }

    public void validateDataKey(String key) throws InvalidSCMRevisionDataException {
        if (key == null || key.isEmpty()) {
            throw new InvalidSCMRevisionDataException(DATA_KEY_EMPTY_MESSAGE);
        }
        Matcher matcher = DATA_KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            throw new InvalidSCMRevisionDataException(dataKeyInvalidMessage(key));
        }
    }

    private String dataKeyInvalidMessage(String key) {
        return String.format("Key '%s' is invalid. Key names should consists of only alphanumeric characters and/or underscores.", key);
    }

    public List<ModifiedFile> getModifiedFiles() {
        return modifiedFiles;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        SCMRevision that = (SCMRevision) o;

        if (revision != null ? !revision.equals(that.revision) : that.revision != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return revision != null ? revision.hashCode() : 0;
    }
}
