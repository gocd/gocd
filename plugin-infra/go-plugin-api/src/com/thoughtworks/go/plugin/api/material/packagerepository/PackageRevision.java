/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.plugin.api.material.packagerepository;

import com.thoughtworks.go.plugin.api.material.packagerepository.exceptions.InvalidPackageRevisionDataException;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents specific revision of the package. Package revision consists of revision, timestamp, user, revision comment and addition data. Additional data is key vale map.
 * Each entry added to additional data will be provided to agent as environment variable.
 */
@Deprecated
//Will be moved to internal scope
public class PackageRevision {
    private static Pattern DATA_KEY_PATTERN = Pattern.compile("[a-zA-Z0-9_]*");
    private static final String DATA_KEY_EMPTY_MESSAGE = "Key names cannot be null or empty.";

    private String revision;

    private Date timestamp;

    private String user;

    private String revisionComment;

    private String trackbackUrl;

    private Map<String, String> data;

    public PackageRevision(String revision, Date timestamp, String user) {
        this(revision, timestamp, user, new HashMap<String, String>());
    }

    public PackageRevision(String revision, Date timestamp, String user, Map<String, String> data) {
        this(revision, timestamp, user, null, null, data);
    }

    public PackageRevision(String revision, Date timestamp, String user, String revisionComment, String trackbackUrl) {
        this(revision, timestamp, user, revisionComment, trackbackUrl, new HashMap<String, String>());
    }

    public PackageRevision(String revision, Date timestamp, String user, String revisionComment, String trackbackUrl, Map<String, String> data) {
        this.revision = revision;
        this.timestamp = timestamp;
        this.user = user;
        this.revisionComment = revisionComment;
        this.trackbackUrl = trackbackUrl;
        validateDataKeys(data);
        this.data = data;
    }

    private void validateDataKeys(Map<String, String> data) {
        if (data != null) {
            for (String key : data.keySet()) {
                validateDataKey(key);
            }
        }
    }

    /**
     * Gets revision string
     *
     * @return revision string
     */
    public String getRevision() {
        return revision;
    }

    /**
     * Gets revision timestamp
     *
     * @return revision timestamp
     */
    public Date getTimestamp() {
        return timestamp;
    }

    /**
     * Gets user associated with revision
     *
     * @return user associated with revision
     */
    public String getUser() {
        return user;
    }

    /**
     * Gets comment associated with revision
     *
     * @return comment associated with revision
     */
    public String getRevisionComment() {
        return revisionComment;
    }

    /**
     * Gets url which can provide information about producer of package revision
     *
     * @return url which can provide information about producer of package revision
     */
    public String getTrackbackUrl() {
        return trackbackUrl;
    }

    /**
     * Gets additional data related to package revision
     *
     * @return additional data related to package revision
     */
    public Map<String, String> getData() {
        return data;
    }

    /**
     * Gets additional data related to package revision for given key
     *
     * @param key for additional data
     * @return additional data related to package revision for given key
     */
    public String getDataFor(String key) {
        return data.get(key);
    }

    /**
     * Adds additional data related to the package revision
     *
     * @param key   for additional data
     * @param value for additional data
     * @throws InvalidPackageRevisionDataException if the key is null or empty
     */
    public void addData(String key, String value) throws InvalidPackageRevisionDataException {
        validateDataKey(key);
        data.put(key, value);
    }

    public void validateDataKey(String key) throws InvalidPackageRevisionDataException {
        if (key == null || key.isEmpty()) {
            throw new InvalidPackageRevisionDataException(DATA_KEY_EMPTY_MESSAGE);
        }
        Matcher matcher = DATA_KEY_PATTERN.matcher(key);
        if (!matcher.matches()) {
            throw new InvalidPackageRevisionDataException(dataKeyInvalidMessage(key));
        }
    }

    private String dataKeyInvalidMessage(String key) {
        return String.format("Key '%s' is invalid. Key names should consists of only alphanumeric characters and/or underscores.", key);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PackageRevision that = (PackageRevision) o;

        if (revision != null ? !revision.equals(that.revision) : that.revision != null) {
            return false;
        }
        if (timestamp != null ? !timestamp.equals(that.timestamp) : that.timestamp != null) {
            return false;
        }
        if (user != null ? !user.equals(that.user) : that.user != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = revision != null ? revision.hashCode() : 0;
        result = 31 * result + (timestamp != null ? timestamp.hashCode() : 0);
        result = 31 * result + (user != null ? user.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PackageRevision{" +
                "revision='" + revision + '\'' +
                ", timestamp=" + timestamp +
                ", user='" + user + '\'' +
                ", revisionComment='" + revisionComment + '\'' +
                ", trackbackUrl='" + trackbackUrl + '\'' +
                ", data=" + data +
                '}';
    }

}
