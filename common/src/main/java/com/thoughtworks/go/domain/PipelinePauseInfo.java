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

import java.io.Serializable;
import java.util.Date;

public class PipelinePauseInfo implements Serializable {
    private boolean paused;
    private String pauseCause;
    private String pauseBy;
    private Date pausedAt;

    public static final PipelinePauseInfo NULL = notPaused();

    public static PipelinePauseInfo notPaused() {
        return new PipelinePauseInfo(false, "", "", null);
    }

    public static PipelinePauseInfo paused(String pauseCause, String pauseBy, Date pausedAt) {
        return new PipelinePauseInfo(true, pauseCause, pauseBy, pausedAt);
    }

    public static PipelinePauseInfo paused(String pauseCause, String pauseBy) {
        return new PipelinePauseInfo(true, pauseCause, pauseBy);
    }

    protected PipelinePauseInfo() {}

    public PipelinePauseInfo(boolean paused, String pauseCause, String pauseBy, Date pausedAt) {
        this.paused = paused;
        this.pauseCause = pauseCause;
        this.pauseBy = pauseBy;
        this.pausedAt = pausedAt;
    }

    public PipelinePauseInfo(boolean paused, String pauseCause, String pauseBy) {
        this(paused, pauseCause, pauseBy, null);
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public String getPauseCause() {
        return (pauseCause == null) ? "" : pauseCause;
    }

    public void setPauseCause(String pauseCause) {
        this.pauseCause = pauseCause;
    }

    public String getPauseBy() {
        return (pauseBy == null) ? "" : pauseBy;
    }

    public void setPauseBy(String pauseBy) {
        this.pauseBy = pauseBy;
    }

    public Date getPausedAt() {
        return pausedAt;
    }

    public void setPausedAt(Date pausedAt) {
        this.pausedAt = pausedAt;
    }

    @Override
    public String toString() {
        return "PipelinePauseInfo{" +
                "paused=" + paused +
                ", pauseCause='" + pauseCause + '\'' +
                ", pauseBy='" + pauseBy + '\'' +
                ", pausedAt=" + pausedAt +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelinePauseInfo that = (PipelinePauseInfo) o;

        if (paused != that.paused) {
            return false;
        }
        if (pauseBy != null ? !pauseBy.equals(that.pauseBy) : that.pauseBy != null) {
            return false;
        }
        if (pauseCause != null ? !pauseCause.equals(that.pauseCause) : that.pauseCause != null) {
            return false;
        }
        if (pausedAt != null ? !pausedAt.equals(that.pausedAt) : that.pausedAt != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (paused ? 1 : 0);
        result = 31 * result + (pauseCause != null ? pauseCause.hashCode() : 0);
        result = 31 * result + (pauseBy != null ? pauseBy.hashCode() : 0);
        result = 31 * result + (pausedAt != null ? pausedAt.hashCode() : 0);
        return result;
    }
}
