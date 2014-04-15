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

package com.thoughtworks.go.feature;

import static java.lang.String.format;
import static com.thoughtworks.go.util.GoConstants.CRUISE_FREE;

public enum EnterpriseFeature {
    MULTIPLE_PIPELINE_GROUP {
        public String messageForFreeEdition() {
            return format("You cannot have more than one Pipeline group in %s.", CRUISE_FREE);
        }
    },
    OPERATE_PERMISSION {
        public String messageForFreeEdition() {
            return format("You cannot configure security permissions in %s.", CRUISE_FREE);
        }
    },
    VIEW_PERMISSION {
        public String messageForFreeEdition() {
            return format("You cannot configure security permissions in %s.", CRUISE_FREE);
        }
    },
    ENVIRONMENTS{
        public String messageForFreeEdition() {
            return format("You have configured the feature, Environments, that is not available in %s.",CRUISE_FREE);
        }
    },
    TEMPLATES{
        public String messageForFreeEdition() {
            return format("You have configured the feature, Templates, that is not available in %s.", CRUISE_FREE);
        }
    };

    public abstract String messageForFreeEdition();
}
