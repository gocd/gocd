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

package com.thoughtworks.go.util;

import java.util.Map;
import java.util.HashMap;

/**
 * Helpers for using ibatis
 */
public class IBatisUtil {
    public static IBatisArgument arguments(String key, Object value) {
        return new IBatisArgument(key, value);
    }

    public static class IBatisArgument {
        private Map<String, Object> map = new HashMap<>();

        private IBatisArgument(String key, Object value) {
            map.put(key, value);
        }

        public IBatisArgument and(String key, Object value) {
            map.put(key, value);
            return this;
        }

        public Map<String, Object> asMap() {
            return map;
        }

        public void addParams(Map<String, Object> sqlCriteria) {
            map.putAll(sqlCriteria);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            IBatisArgument that = (IBatisArgument) o;

            if (map != null ? !map.equals(that.map) : that.map != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return map != null ? map.hashCode() : 0;
        }
    }
}
