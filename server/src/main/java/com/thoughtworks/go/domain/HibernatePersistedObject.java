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

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;
import org.hibernate.annotations.GenericGenerator;

import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.MappedSuperclass;

@EqualsAndHashCode
@ToString
@Getter
@Setter
@Accessors(chain = true)
@MappedSuperclass
public abstract class HibernatePersistedObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @GenericGenerator(strategy = "identity", name = "identity")
    protected Long id;

    public boolean persisted() {
        if (id == null) {
            return false;
        } else {
            return id != -1;
        }
    }
}
