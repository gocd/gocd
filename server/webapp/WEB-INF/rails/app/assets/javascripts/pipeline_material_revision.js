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

function init_trigger_popup() {
    $("close").observe("click", function() {
        Modalbox.hide();
    });
    MaterialDetailsToggler.start_observing(function(cur_summary, cur_detail) {
        var input = $(cur_detail).down('.autocomplete-input');
        if (input) {
            input.trigger_autosuggest();
        }
    });
    $$(".variable").each(function (variable) {
        var defaultvalue = variable.down("input[type='hidden']").value;
        var text_box = variable.down("input[type='text']");
        text_box.observe("change", function(event) {
            var message = variable.down(".message");
            var element = Event.element(event);
            if (element.value === defaultvalue) {
                message.addClassName("hidden");
                environmentVariableDirtyTracker.undoUpdate(text_box.id);
            } else {
                message.removeClassName("hidden");
                environmentVariableDirtyTracker.update(text_box.id);
            }
        });

    });
}

function disable_unchanged(){
    $$(".variable").each(function (variable) {        
        var txtbox = variable.down("input[type='text']");
        if(txtbox.value==variable.down("input[type='hidden']").value){
            txtbox.disable();            
        }
    });
}