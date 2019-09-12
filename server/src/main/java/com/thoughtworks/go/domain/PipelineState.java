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

import lombok.*;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Table(name = "pipelinestates")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PipelineState extends HibernatePersistedObject {
    public final static PipelineState NOT_LOCKED = new PipelineState("NOT_LOCKED", -100);

    private String pipelineName;
    private boolean locked = false;
    private long lockedByPipelineId;
    @Transient
    @EqualsAndHashCode.Include
    private transient StageIdentifier lockedBy = null;

    private PipelineState(String pipelineName, int id) {
        this(pipelineName);
        setId((long) id);
    }

    public PipelineState(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public PipelineState(String pipelineName, StageIdentifier identifier) {
        this(pipelineName);
        this.lockedBy = identifier;
    }

    public void lock(long lockedByPipelineId) {
        this.lockedByPipelineId = lockedByPipelineId;
        this.locked = true;
    }

    public void unlock() {
        locked = false;
        lockedBy = null;
        lockedByPipelineId = 0;
    }
}
