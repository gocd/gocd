/*
 * Copyright Thoughtworks, Inc.
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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.util.*;

import static com.thoughtworks.go.util.ExceptionUtils.bomb;

/**
 * Understands a pipeline which can be compared based on its material checkin (natural) order
 */
public class PipelineTimelineEntry implements Comparable<PipelineTimelineEntry> {

    private final String pipelineName;
    private final long id;
    private final int counter;
    private final Map<String, List<Revision>> revisionsByFingerprint;

    private PipelineTimelineEntry insertedBefore;
    private PipelineTimelineEntry insertedAfter;
    private double naturalOrder;
    private boolean hasBeenUpdated;

    @TestOnly
    public PipelineTimelineEntry(String pipelineName, long id, int counter, Map<String, List<Revision>> revisionsByFingerprint) {
        this(pipelineName, id, counter, revisionsByFingerprint, 0.0);
    }

    public PipelineTimelineEntry(String pipelineName, long id, Integer counter, Map<String, List<Revision>> revisionsByFingerprint, double naturalOrder) {
        this.pipelineName = pipelineName;
        this.id = id;
        this.counter = counter;
        this.revisionsByFingerprint = revisionsByFingerprint;
        this.naturalOrder = naturalOrder;
    }

    public boolean addRevision(String fingerprint, Revision rev) {
        return revisions().computeIfAbsent(fingerprint, k -> new ArrayList<>()).add(rev);
    }

    @Override
    public int compareTo(PipelineTimelineEntry o) {
        if (this.equals(o)) {
            return 0;
        }

        return comparingByRevisions() // Compare across materials based on earliest revision date on matching materials
            .thenComparingInt(t -> t.counter) // Fallback to counter if revision dates are inconclusive
            .thenComparingLong(t -> t.id) // then tie-break on ID in the worst case "bug" scenario where pipelines with different capitalization have ended up with identical counters. See the tests.
            .compare(this, o);
    }

    private static @NotNull Comparator<PipelineTimelineEntry> comparingByRevisions() {
        return Comparator.comparing(t -> t.revisionsByFingerprint, (revisionsByFingerprint1, revisionsByFingerprint2) -> {
            EarliestRev earliestAcrossMaterials = EarliestRev.MAX_VALUE;

            for (Map.Entry<String, List<Revision>> fingerprintRevs : revisionsByFingerprint1.entrySet()) {
                List<Revision> o1Revs = fingerprintRevs.getValue();
                if (o1Revs == null) {
                    continue;
                }
                List<Revision> o2Revs = revisionsByFingerprint2.get(fingerprintRevs.getKey());
                if (o2Revs == null) {
                    continue;
                }

                earliestAcrossMaterials = earliestAcrossMaterials.chooseEarliest(o1Revs.get(0), o2Revs.get(0));
            }

            return earliestAcrossMaterials.asCompareToResult();
        });
    }

    record EarliestRev(Date date, int thisComparedToEarliest) {
        static final EarliestRev MAX_VALUE = new EarliestRev(new Date(Long.MAX_VALUE), 0);

        boolean isInconclusive() {
            return this == MAX_VALUE || isSameDirectionAs(0);
        }

        private boolean isSameDirectionAs(int compareTo) {
            return thisComparedToEarliest == compareTo;
        }

        @NotNull EarliestRev chooseEarliest(@Nullable Revision leftEarliestRev, @Nullable Revision rightEarlierRev) {
            if (leftEarliestRev == null || rightEarlierRev == null) {
                return this; // shouldn't happen, but be safe
            }
            int leftComparedToRight = leftEarliestRev.date.compareTo(rightEarlierRev.date);
            if (leftComparedToRight == 0) {
                return this; // They are equal; earliest across materials remains unchanged
            } else {
                // We have two revs with different dates in same material. Which one is earlier?
                Date candidateNewMinimum = leftComparedToRight < 0 ? leftEarliestRev.date : rightEarlierRev.date;

                // Is the date earlier than our current minimum; and how does it relate to "this"?
                return chooseEarliest(candidateNewMinimum, leftComparedToRight);
            }
        }

        private @NotNull EarliestRev chooseEarliest(@NotNull Date candidateNewMinimum, int leftComparedToRight) {
            int newComparedToExisting = candidateNewMinimum.compareTo(date);

            if (newComparedToExisting > 0) {
                return this; // new date greater than current minimum - leave minimum as-is
            } else if (newComparedToExisting == 0 && (isInconclusive() || isSameDirectionAs(leftComparedToRight))) {
                return this; // new date candidate is same, and comparison inconclusive or same relationship to - leave as-is
            } else if (newComparedToExisting == 0) {
                return new EarliestRev(date, 0); // same date, but different implied comparison - now it's inconclusive
            } else {
                return new EarliestRev(candidateNewMinimum, leftComparedToRight); // new date is earlier with clear relationship to "this" Revision
            }
        }

        private int asCompareToResult() {
            return isInconclusive() ? 0 : thisComparedToEarliest;
        }
    }

    public int getCounter() {
        return counter;
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

    public long getId() {
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
        return Long.hashCode(id);
    }

    @Override
    public String toString() {
        return "PipelineTimelineEntry{" +
            "pipelineName='" + pipelineName + '\'' +
            ", id=" + id +
            ", counter=" + counter +
            ", revisions=" + revisionsByFingerprint +
            ", naturalOrder=" + naturalOrder +
            '}';
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
        double previous = insertedAfter != null ? insertedAfter.naturalOrder : 0.0;
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
        return revisionsByFingerprint;
    }

    public record Revision(@NotNull Date date, String revision, long id) {
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Revision revision1 = (Revision) o;
            return Objects.equals(date, revision1.date) && Objects.equals(revision, revision1.revision);
        }

        @Override
        public int hashCode() {
            return Objects.hash(date, revision);
        }

        @Override
        public @NotNull String toString() {
            return new StringJoiner(", ", "Revision[", "]")
                .add("date=" + date)
                .add("revision='" + revision + "'")
                .toString();
        }

        public boolean lessThan(Revision revision) {
            if (this == revision) {
                return true;
            }

            return date.compareTo(revision.date) < 0;
        }
    }
}
