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

package com.thoughtworks.go.domain;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * @understands a pipeline which can be compared based on its material checkin (natural) order
 */
public class PipelineTimelineEntry implements Comparable {
    private final String pipelineName;
    private final long id;
    private final int counter;
    private final Map<String, List<Revision>> revisions;
    private PipelineTimelineEntry insertedBefore;
    private PipelineTimelineEntry insertedAfter;
    private double naturalOrder = 0.0;
    private boolean hasBeenUpdated;

    public PipelineTimelineEntry(String pipelineName, long id, int counter, Map<String, List<Revision>> revisions) {
        this.pipelineName = pipelineName;
        this.id = id;
        this.counter = counter;
        this.revisions = revisions;
    }

    public PipelineTimelineEntry(String pipelineName, long id, Integer counter, Map<String, List<Revision>> revisions, double naturalOrder) {
        this(pipelineName, id, counter, revisions);
        this.naturalOrder = naturalOrder;
    }

    public int compareTo(Object o) {
        if (o == null) {
            throw new NullPointerException("Cannot compare this object with null");
        }
        if (o.getClass() != this.getClass()) {
            throw new RuntimeException("Cannot compare '" + o + "' with '" + this + "'");
        }
        if (this.equals(o)) {
            return 0;
        }

        PipelineTimelineEntry that = (PipelineTimelineEntry) o;
        Map<Date, TreeSet<Integer>> earlierMods = new HashMap<>();

        for (String materialFlyweight : revisions.keySet()) {
            List<Revision> thisRevs = this.revisions.get(materialFlyweight);
            List<Revision> thatRevs = that.revisions.get(materialFlyweight);
            if (thisRevs == null || thatRevs == null) {
                continue;
            }
            Revision thisRevision = thisRevs.get(0);
            Revision thatRevision = thatRevs.get(0);
            if (thisRevision == null || thatRevision == null) {
                continue;
            }
            Date thisDate = thisRevision.date;
            Date thatDate = thatRevision.date;
            if (thisDate.equals(thatDate)) {
                continue;
            }
            populateEarlierModification(earlierMods, thisDate, thatDate);
        }
        if (earlierMods.isEmpty()) {
            return counter < that.counter ? -1 : 1;
        }
        TreeSet<Date> sortedModDate = new TreeSet<>(earlierMods.keySet());
        if (hasContentionOnEarliestMod(earlierMods, sortedModDate.first())) {
            return counter < that.counter ? -1 : 1;
        }
        return earlierMods.get(sortedModDate.first()).first();
    }

    public int getCounter() {
        return counter;
    }

    private void populateEarlierModification(Map<Date, TreeSet<Integer>> earlierMods, Date thisDate, Date thatDate) {
        int value = thisDate.before(thatDate) ? -1 : 1;
        Date actual = thisDate.before(thatDate) ? thisDate : thatDate;
        if (!earlierMods.containsKey(actual)) {
            earlierMods.put(actual, new TreeSet<Integer>());
        }
        earlierMods.get(actual).add(value);
    }

    private boolean hasContentionOnEarliestMod(Map<Date, TreeSet<Integer>> earlierMods, Date earliestModDate) {
        return earlierMods.get(earliestModDate).size() > 1;
    }

    public PipelineTimelineEntry insertedBefore() {
        return insertedBefore;
    }

    public PipelineTimelineEntry insertedAfter() {
        return insertedAfter;
    }

    public void setInsertedBefore(PipelineTimelineEntry insertedBefore) {
        if (this.insertedBefore != null) {
            throw bomb("cannot change insertedBefore for: " + this + " with " + insertedBefore);
        }
        this.insertedBefore = insertedBefore;
    }

    public void setInsertedAfter(PipelineTimelineEntry insertedAfter) {
        if (this.insertedAfter != null) {
            throw bomb("cannot change insertedAfter for: " + this + " with " + insertedAfter);
        }
        this.insertedAfter = insertedAfter;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        PipelineTimelineEntry that = (PipelineTimelineEntry) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override public String toString() {
        return "PipelineTimelineEntry{" +
                "pipelineName='" + pipelineName + '\'' +
                ", id=" + id +
                ", counter=" + counter +
                ", revisions=" + revisions +
                ", naturalOrder=" + naturalOrder +
                '}';
    }

    public PipelineTimelineEntry previous() {
        return insertedAfter();
    }

    public double naturalOrder() {
        return naturalOrder;
    }

    public void updateNaturalOrder() {
        double calculatedOrder = calculateNaturalOrder();
        if (this.naturalOrder > 0.0 && this.naturalOrder != calculatedOrder) {
            bomb(String.format("Calculated natural ordering %s is not the same as the existing naturalOrder %s, for pipeline %s, with id %s", calculatedOrder, this.naturalOrder, this.pipelineName, this.id));
        }
        if (this.naturalOrder == 0.0 && this.naturalOrder != calculatedOrder) {
            this.naturalOrder = calculatedOrder;
            this.hasBeenUpdated = true;
        }
    }

    public boolean hasBeenUpdated() {
        return this.hasBeenUpdated;
    }

    private double calculateNaturalOrder() {
        double previous = 0.0;
        if (insertedAfter != null) {
            previous = insertedAfter.naturalOrder;
        }
        if (insertedBefore != null) {
            return (previous + insertedBefore.naturalOrder) / 2.0;
        } else {
            return previous + 1.0;
        }
    }

    public PipelineIdentifier getPipelineLocator() {
        return new PipelineIdentifier(pipelineName, counter, null);
    }

    public Map<String, List<Revision>> revisions() {
        return revisions;
    }

    public static class Revision {
        public final Date date;
        public final String revision;
        public final String folder;
        public final long id;

        public Revision(Date date, String revision, String folder, long id) {
            this.date = date;
            this.revision = revision;
            this.folder = folder;
            this.id = id;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            Revision revision1 = (Revision) o;

            if (date != null ? !date.equals(revision1.date) : revision1.date != null) {
                return false;
            }
            if (revision != null ? !revision.equals(revision1.revision) : revision1.revision != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = date != null ? date.hashCode() : 0;
            result = 31 * result + (revision != null ? revision.hashCode() : 0);
            result = 31 * result + (folder != null ? folder.hashCode() : 0);
            return result;
        }

        @Override public String toString() {
            return "Revision{" +
                    "date=" + date +
                    ", revision='" + revision + '\'' +
                    ", folder='" + folder + '\'' +
                    '}';
        }

        public boolean lessThan(Revision revision) {
            if (this == revision) {
                return true;
            }

//            if (!folder.equals(revision.folder)) {
//                return false;
//            }

            if (date.compareTo(revision.date) < 0) {
                return true;
            }

            return false;
        }
    }
}
