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

package com.thoughtworks.go.domain.feed.stage;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.thoughtworks.go.domain.StageIdentifier;
import com.thoughtworks.go.domain.StageResult;
import com.thoughtworks.go.domain.feed.Author;
import com.thoughtworks.go.domain.feed.FeedEntry;
import com.thoughtworks.go.server.ui.MingleCard;
import com.thoughtworks.go.util.GoConstants;

/**
 * @understands an atom feed entry
 */
public class StageFeedEntry implements FeedEntry {
    private long id;
    private long pipelineId;
    private StageIdentifier identifier;
    private long entryId;
    private Date updateDate;
    private StageResult stageResult;
    private List<Author> authors = new ArrayList<>();
    private List<MingleCard> mingleCards = new ArrayList<>();
    private String approvedBy;
    private String approvalType;

    protected StageFeedEntry() {
    }

    public StageFeedEntry(long id, long pipelineId, StageIdentifier identifier, long entryId, Date updateDate, StageResult result) {
        this();
        this.id = id;
        this.pipelineId = pipelineId;
        this.identifier = identifier;
        this.entryId = entryId;
        this.updateDate = updateDate;
        this.stageResult = result;
    }

    public StageFeedEntry(long id, long pipelineId, StageIdentifier identifier, long entryId, Date updateDate, StageResult result, String approvalType, String approvedBy) {
        this(id, pipelineId, identifier, entryId, updateDate, result);
        this.approvalType = approvalType;
        this.approvedBy = approvedBy;
    }

    public String getResult() {
        return stageResult.name();
    }

    public Date getUpdatedDate() {
        return updateDate;
    }

    public long getId() {
        return id;
    }

    public long getEntryId() {
        return entryId;
    }

    public String getTitle() {
        return String.format("%s(%s) stage %s(%s) %s", identifier.getPipelineName(), identifier.getPipelineCounter(), identifier.getStageName(), identifier.getStageCounter(), stageResult);
    }

    public StageIdentifier getStageIdentifier() {
        return identifier;
    }

    public long getPipelineId() {
        return pipelineId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        StageFeedEntry that = (StageFeedEntry) o;

        if (entryId != that.entryId) {
            return false;
        }
        if (id != that.id) {
            return false;
        }
        if (pipelineId != that.pipelineId) {
            return false;
        }
        if (identifier != null ? !identifier.equals(that.identifier) : that.identifier != null) {
            return false;
        }
        if (stageResult != that.stageResult) {
            return false;
        }
        if (updateDate != null ? !updateDate.equals(that.updateDate) : that.updateDate != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (id ^ (id >>> 32));
        result = 31 * result + (int) (pipelineId ^ (pipelineId >>> 32));
        result = 31 * result + (identifier != null ? identifier.hashCode() : 0);
        result = 31 * result + (int) (entryId ^ (entryId >>> 32));
        result = 31 * result + (updateDate != null ? updateDate.hashCode() : 0);
        result = 31 * result + (stageResult != null ? stageResult.hashCode() : 0);
        return result;
    }

    @Override public String toString() {
        return "StageFeedEntry{" +
                "id=" + id +
                ", pipelineId=" + pipelineId +
                ", identifier=" + identifier +
                ", entryId=" + entryId +
                ", updateDate=" + updateDate +
                ", stageResult=" + stageResult +
                '}';
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public List<MingleCard> getMingleCards() {
        return mingleCards;
    }

    public void addCard(MingleCard mingleCard) {
        if (!mingleCards.contains(mingleCard)) {
            mingleCards.add(mingleCard);
        }
    }

    public void addAuthor(Author author) {
        if (!authors.contains(author)) {
            authors.add(author);
        }
    }

    public boolean isManuallyTriggered() {
        return approvalType.equals(GoConstants.APPROVAL_MANUAL);
    }

    public String getApprovedBy() {
        return approvedBy;
    }
}
