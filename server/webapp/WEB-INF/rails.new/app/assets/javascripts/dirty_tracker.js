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

DirtyTracker = function() {

    function _setClass(self) {
        self.elementToBeUpdated.addClassName('updated');
    }
    
    function _removeClass(self) {
        self.elementToBeUpdated.removeClassName('updated');
    }

    function _update(self, index) {
        self.indexToUpdated.set(index, true);
        _setClass(self);
    }

    function _undoUpdate(self, index) {
        self.indexToUpdated.set(index, false);
        var hasAtLeastOneChange = self.indexToUpdated.inject(false, function(acc, pair) {
            return acc || pair.value;
        });
        if (!hasAtLeastOneChange) {
            _removeClass(self);
        }
    }

    return function(elementId) {
        this.elementToBeUpdated = $(elementId);
        this.indexToUpdated = new Hash();

        var self = this;

        return {
            update: function(key) {
                _update(self, key);
            },
            undoUpdate: function(key) {
                _undoUpdate(self, key);
            }
        };
    };
}();