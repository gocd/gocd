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
package com.thoughtworks.go.domain;

public class PipelineState extends PersistentObject {
    private String pipelineName;
    private boolean locked = false;
    private long lockedByPipelineId;

    private transient StageIdentifier lockedBy = null;
    public final static PipelineState NOT_LOCKED = new PipelineState("NOT_LOCKED", -100);

    public PipelineState() {
    }

    private PipelineState(String pipelineName, int id) {
        this(pipelineName);
        this.id = id;
    }

    public PipelineState(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public PipelineState(String pipelineName, StageIdentifier identifier) {
        this(pipelineName);
        this.lockedBy = identifier;
    }

    public StageIdentifier getLockedBy() {
        return lockedBy;
    }

    public void setLockedBy(StageIdentifier lockedBy) {
        this.lockedBy = lockedBy;
    }

    public boolean isLocked() {
        return locked;
    }

    public void lock(long lockedByPipelineId) {
        this.lockedByPipelineId = lockedByPipelineId;
        locked = true;
    }

    public void unlock() {
        locked = false;
        lockedBy = null;
        lockedByPipelineId = 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PipelineState that = (PipelineState) o;

        if (locked != that.locked) return false;
        if (lockedByPipelineId != that.lockedByPipelineId) return false;
        if (!pipelineName.equals(that.pipelineName)) return false;
        return lockedBy != null ? lockedBy.equals(that.lockedBy) : that.lockedBy == null;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + pipelineName.hashCode();
        result = 31 * result + (locked ? 1 : 0);
        result = 31 * result + (int) (lockedByPipelineId ^ (lockedByPipelineId >>> 32));
        result = 31 * result + (lockedBy != null ? lockedBy.hashCode() : 0);
        return result;
    }

    public long getLockedByPipelineId() {
        return lockedByPipelineId;
    }
}
