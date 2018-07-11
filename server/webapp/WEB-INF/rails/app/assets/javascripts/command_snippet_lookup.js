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

CommandSnippetLookup = function(lookupTextbox, autoCompleteUrl) {
    var hookupAutocomplete = function() {
        var textBox = jQuery(lookupTextbox);
        textBox.autocomplete(null, {
            minChars: 1,
            width: textBox.outerWidth(),
            cacheLength: 0,
            max: 50,
            delay: 600,
            dataLoader: function(options, term, success, input, parse) {
                var enteredText = jQuery.trim(textBox.val());
                if(enteredText.empty()) {
                    return;
                }

                jQuery.ajax({
                    // try to leverage ajaxQueue plugin to abort previous requests
                    mode: "abort",
                    // limit abortion to this input
                    data: { "lookup_prefix" : enteredText},
                    port: "autocomplete" + input.name,
                    url: autoCompleteUrl,
                    dataType: "text",
                    success: function(data) {
                        success(term, parse(data));
                    }
                });
            }
        });
    };

    return {
        hookupAutocomplete: hookupAutocomplete
    };
};
