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

var CruiseAutocomplete = {
    materialSearch: function(should_select, pipeline_name, fingerprint, id_of_text_field, afterSelectedFn) {
        var autocompleter = new Ajax.Autocompleter(id_of_text_field, id_of_text_field + "-content", "/go/pipelines/material_search", {
            parameters: 'pipeline_name=' + pipeline_name + '&fingerprint=' + fingerprint,
            paramName: 'search',
            method: 'get',
            minChars: -1,
            onShow: function(element, update){
                var materialPopupContainers = $$("div.change_materials");
                if (!(materialPopupContainers.first()  && materialPopupContainers.first().visible())) {
                    return;
                }
                if(!update.style.position || update.style.position=='absolute') {
                  update.style.position = 'fixed';
                  Position.clone(element, update, {
                    setHeight: false,
                    offsetTop: element.offsetHeight,
                    setWidth: false
                  });
                }
                Effect.Appear(update,{duration:0.15});
              },
            updateElement: function(selectedElement) {
                var revisionElement = $(selectedElement).select('.revision')[0];
                var longRevision = revisionElement.readAttribute('title');
                var shortRevision = revisionElement.innerHTML;

                var text_field = $(id_of_text_field);
                text_field.value = longRevision;
                text_field.focus();

                if (afterSelectedFn) { afterSelectedFn(shortRevision, longRevision); }
              }
        });
        autocompleter.onFocus = function() {
            this.hasFocus = true;
            this.onObserverEvent();
        };
        var focus_callback = autocompleter.onFocus.bindAsEventListener(autocompleter);
        Event.observe($(id_of_text_field), "focus", focus_callback);
        $(id_of_text_field).trigger_autosuggest = focus_callback;
        if (should_select) autocompleter.onFocus();
    }
  };
