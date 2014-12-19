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

var WordBreaker = Class.create();

WordBreaker.prototype = {
    initialize : function(unit) {
        this.unit = unit;
        _inserter = this;
    },
    insert: function() {
        $$('.wbrSensitive').each(this.word_break);
    },
    word_break: function(element) {
        var html_in_element = element.innerHTML ? element.innerHTML.toLowerCase() : '';
        if (html_in_element.indexOf(_inserter.word_break_element()) > -1) {
            return;
        }
        element.update(_inserter.break_text(element.innerHTML));
    },
    break_text : function(text) {
        var textArray = text.toArray();
        if (!textArray) return;
        var content = '';
        for(var i = 0; i < textArray.length;i++) {
            if ((i + 1) % _inserter.unit == 0) {
                content += (textArray[i] + _inserter.word_break_element());
            } else {
                content += textArray[i];
            }
        }
        return content;
    },
    word_break_element: function() {
        return Prototype.Browser.Gecko ? '<wbr>' : '&shy;';
    }
}
var $$WordBreaker = new WordBreaker(5);

Event.observe(window, 'load', function() {
    $$WordBreaker.insert();
});

