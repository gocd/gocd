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

DefaultText = function() {
    var CSS_CLASS = "shadedDefaultValue";

    function clear(self) {
        if(noUserText(self) && (self.dom.value == self.defaultText)) {
            self.dom.value = '';
            self.dom.removeClassName(CSS_CLASS);
        }
    }

    function noUserText(self) {
        return self.userText.empty();
    }

    function resetDefaultText(self) {
        if(self.dom.value.empty()) {
            self.dom.value = self.defaultText;
            self.dom.addClassName(CSS_CLASS);
        }
    }

    function registerUserInput(self) {
        self.userText = self.dom.value;
    }

    function init(element, defaultText) {
        this.defaultText = defaultText;
        this.dom = element;
        this.userText = "";
        resetDefaultText(this);
        var self = this;
        Event.observe(element, 'focus', function() { clear(self); });
        Event.observe(element, 'blur', function() { resetDefaultText(self); });
        Event.observe(element, 'keyup', function () { registerUserInput(self); });
    }

    init.prototype.clear = function() { clear(this); };

    return init;
}();