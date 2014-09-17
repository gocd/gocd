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
 *************************GO-LICENSE-END**********************************/

EnvironmentVariablesFormFieldValidator = function() {

    function value(self, fieldValues) {
        return jQuery.trim(fieldValues[self.uniqueField]);
    }

    function indexOf(self, value) {
        var index = -1;
        jQuery.grep(self.addedUniqueNames, function(val, i) {
            if (val === value) {
                index = i;
                return true;
            }
        });
        return index;
    }

    function found(self, value) {
        return indexOf(self, value) !== -1;
    }

    var init = function(uniqueField, errorMessageContainer) {
        this.uniqueField = uniqueField;
        this.addedUniqueNames = [];
        this.errorMessageContainer = errorMessageContainer;
    };

    init.prototype.isValid = function(fieldValues) {
        var uniqueValue = value(this, fieldValues);
        if (found(this, uniqueValue)) {
            this.errorMessageContainer.html('A variable with this name has already been added');
            return false;
        }
        if(uniqueValue === '') {
            this.errorMessageContainer.html('Name cannot be blank');            
            return false;
        }
        if(uniqueValue.match(/ /)) {
            this.errorMessageContainer.html('Name cannot have spaces');
            return false;
        }
        this.addedUniqueNames.push(uniqueValue);
        return true;
    };

    init.prototype.removed = function(fieldValues) {
        this.addedUniqueNames.splice(indexOf(this, value(self, fieldValues)), 1);
    };

    return init;
}();
