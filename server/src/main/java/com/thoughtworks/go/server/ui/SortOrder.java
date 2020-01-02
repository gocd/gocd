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
package com.thoughtworks.go.server.ui;

import java.util.Comparator;

/**
* @understands sort direction
*/
public enum SortOrder {
    ASC {
        @Override
        public <T> Comparator<T> comparator(final Comparator<T> comparator) {
            return comparator;
        }
    },
    DESC {
        @Override
        public <T> Comparator<T> comparator(final Comparator<T> comparator) {
            return (one, other) -> comparator.compare(one, other) * -1;
        }
    };

    public abstract <T> Comparator<T> comparator(final Comparator<T> comparator);

    public static SortOrder orderFor(String order) {
        return "DESC".equals(order) ? DESC : ASC;
    }
}
