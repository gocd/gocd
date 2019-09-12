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
package com.thoughtworks.go.server.domain.user;

import com.thoughtworks.go.config.CaseInsensitiveString;
import com.thoughtworks.go.domain.HibernatePersistedObject;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.sql.Timestamp;

import static org.apache.commons.codec.digest.DigestUtils.sha256Hex;

@Getter
@Setter
@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Accessors(chain = true)
@Entity
@Table(name = "pipelineSelections")
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class PipelineSelections extends HibernatePersistedObject {
    public static final int CURRENT_SCHEMA_VERSION = 2;
    public static final PipelineSelections ALL = new PipelineSelections(Filters.defaults(), null, null);

    private Long userId;
    @Column(name = "lastUpdate")
    private Timestamp lastUpdated;
    private int version;

    @Transient // persisted via getters/setters
    private Filters viewFilters = Filters.defaults();
    @Transient
    private String etag;

    public PipelineSelections() {
        this(Filters.defaults(), null, null);
    }

    public PipelineSelections(Filters filters, Timestamp date, Long userId) {
        update(filters, date, userId);
    }

    @Column(name = "filters")
    @Access(AccessType.PROPERTY)
    public String getFilters() {
        return Filters.toJson(this.viewFilters);
    }

    @Column(name = "filters")
    @Access(AccessType.PROPERTY)
    public void setFilters(String filters) {
        this.viewFilters = Filters.fromJson(filters);
        updateEtag();
    }

    public void update(Filters filters, Timestamp date, Long userId) {
        this.userId = userId;
        this.lastUpdated = date;
        this.viewFilters = null == filters ? Filters.defaults() : filters;
        this.version = CURRENT_SCHEMA_VERSION;
        updateEtag();
    }

    private void updateEtag() {
        this.etag = sha256Hex(this.getFilters().getBytes());
    }

    public DashboardFilter namedFilter(String name) {
        return viewFilters.named(name);
    }

    /**
     * Allows pipeline to be visible to entire filter set; generally used as an
     * after-hook on pipeline creation.
     *
     * @param pipelineToAdd - the name of the pipeline
     * @return true if any filters were modified, false if all filters are unchanged
     */
    public boolean ensurePipelineVisible(CaseInsensitiveString pipelineToAdd) {
        boolean modified = false;

        for (DashboardFilter f : viewFilters.filters()) {
            modified = modified || f.allowPipeline(pipelineToAdd);
        }

        return modified;
    }

}
