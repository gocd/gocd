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
package com.thoughtworks.go.server.domain;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;

public interface PipelineLockStatusChangeListener {
    void lockStatusChanged(Event event);

    class Event {
        private EventType eventType;
        private final String pipelineName;

        public static Event lock(String pipelineName) {
            return new Event(EventType.LOCKED, pipelineName);
        }

        public static Event unLock(String pipelineName) {
            return new Event(EventType.UNLOCKED, pipelineName);
        }

        private Event(EventType eventType, String pipelineName) {
            this.eventType = eventType;
            this.pipelineName = pipelineName;
        }

        @Override
        public boolean equals(Object o) {
            return EqualsBuilder.reflectionEquals(this, o);
        }

        @Override
        public int hashCode() {
            return HashCodeBuilder.reflectionHashCode(this);
        }

        @Override
        public String toString() {
            return ToStringBuilder.reflectionToString(this);
        }

        public String pipelineName() {
            return pipelineName;
        }
    }

    enum EventType {LOCKED, UNLOCKED}
}
