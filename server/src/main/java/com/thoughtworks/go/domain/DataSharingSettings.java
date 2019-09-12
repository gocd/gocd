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
import java.sql.Timestamp;

@EqualsAndHashCode(doNotUseGetters = true, callSuper = true)
@ToString(callSuper = true)
@Getter
@Setter
@Accessors(chain = true)
@NoArgsConstructor
@Entity
@Cacheable
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class DataSharingSettings extends HibernatePersistedObject {
    private boolean allowSharing = true;
    private String updatedBy;
    private Timestamp updatedOn;
}
