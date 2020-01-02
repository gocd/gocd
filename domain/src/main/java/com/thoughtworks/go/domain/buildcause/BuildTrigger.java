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
package com.thoughtworks.go.domain.buildcause;

import java.io.Serializable;

import com.thoughtworks.go.domain.MaterialRevisions;

/**
 * @understands how a pipeline was triggered
 */
public class BuildTrigger implements Serializable {
    private String message;
    private final boolean forced;
    private final Priority priority;
    private final String dbName;

    //Careful! These values are used on the db to encode the buildtrigger and should not be changed without a migration
    static final String MODIFICATION_BUILD_CAUSE = "ModificationBuildCause";
    static final String EXTERNAL_BUILD_CAUSE = "ExternalBuildCause";
    static final String FORCED_BUILD_CAUSE = "ManualForcedBuildCause";

    private BuildTrigger(String message, boolean forced, Priority priority, String dbName) {
        if (dbName==null) throw new IllegalArgumentException("dbName cannot be null");

        this.message = message;
        this.forced = forced;
        this.priority = priority;
        this.dbName = dbName;
    }

    public String getMessage() {
        return message;
    }

    public boolean isForced() {
        return forced;
    }

    public boolean trumps(BuildTrigger other) {
        return priority.trumps(other.priority);
    }

    public String getDbName() {
        return dbName;
    }

    /**
     * @deprecated encapsulate this properly when we use Hibernate
     */
    public void setMessage(String message) {
        this.message = message;
    }

    static BuildTrigger forNeverRun() {
        return new BuildTrigger("NULL", false, Priority.MODIFICATION, FORCED_BUILD_CAUSE);
    }

    static BuildTrigger forModifications(MaterialRevisions materialRevisions) {
        String message = "No modifications";
        if (materialRevisions != null && !materialRevisions.isEmpty()) {
            message = materialRevisions.buildCauseMessage();
        }
        return new BuildTrigger(message, false, Priority.MODIFICATION, MODIFICATION_BUILD_CAUSE);
    }

    static BuildTrigger forExternal() {
        return new BuildTrigger("triggered by svn external changes", false, Priority.MODIFICATION, EXTERNAL_BUILD_CAUSE);
    }

    static BuildTrigger forForced(String message) {
        return new BuildTrigger(message, true, Priority.MANUAL, FORCED_BUILD_CAUSE);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        BuildTrigger trigger = (BuildTrigger) o;

        if (forced != trigger.forced) {
            return false;
        }
        if (dbName != null ? !dbName.equals(trigger.dbName) : trigger.dbName != null) {
            return false;
        }
        if (message != null ? !message.equals(trigger.message) : trigger.message != null) {
            return false;
        }
        if (priority != trigger.priority) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = message != null ? message.hashCode() : 0;
        result = 31 * result + (forced ? 1 : 0);
        result = 31 * result + (priority != null ? priority.hashCode() : 0);
        result = 31 * result + (dbName != null ? dbName.hashCode() : 0);
        return result;
    }

}
