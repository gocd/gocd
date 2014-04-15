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

package com.thoughtworks.go.licensing;

public enum Edition {
    Free() {
        @Override
        public String getDisplayName() {
            return "Community";
        }

        @Override
        public String getDisplayType() {
            return "community";
        }
    },
    OpenSource() {
        @Override
        public String getDisplayName() {
            return "Open Source";
        }

        @Override
        public String getDisplayType() {
            return "opensource";
        }
    },
    Professional() {
        @Override
        public boolean isEnterprise() {
            return true;
        }
    },
    Enterprise() {
        @Override
        public boolean isEnterprise() {
            return true;
        }
    },
    Empty(),
    NoLicense();


    Edition() {
    }

    public boolean isEnterprise() {
        return false;
    }

    public String getDisplayName() {
        return toString();
    }

    public String getDisplayType() {
        return toString().toLowerCase();
    }
}


