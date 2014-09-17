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

AgentFilter = function() {

    function init(filterBox, filterHelp, relativeElement, autocompleteUrls) {
        this.filterBox = filterBox;
        this.filterHelp = filterHelp;
        this.relativeElement = relativeElement;
        this.autocompleteUrls = autocompleteUrls;
    }

    function hookUpHelpClose(self, popup) {
        jQuery(self.filterBox).keydown(function() {
            popup.close();
        });
    }

    function getUrlVars() {
        var vars = [], hash;
        var hashes = window.location.href.slice(window.location.href.indexOf('?') + 1).split('&');
        for (var i = 0; i < hashes.length; i++) {
            hash = hashes[i].split('=');
            vars.push(hash[0]);
            vars[hash[0]] = hash[1];
        }
        return vars;
    }

    init.prototype.createHelp = function() {
        var filterHelpPopup = new MicroContentPopup(this.filterHelp, new MicroContentPopup.NoOpHandler());
        var filterHelpPopupShower = new MicroContentPopup.ClickShower(filterHelpPopup);
        filterHelpPopupShower.bindShowButton(this.filterBox, this.relativeElement);
        hookUpHelpClose(this, filterHelpPopup);
    };

    function readyToRequest(filterBoxEnhanced) {
        return filterBoxEnhanced.val().indexOf(":") != -1;
    }

    function keyword(key) {
        return jQuery.trim(key).substring(0, key.indexOf(":"));
    }

    function urlToUse(self, filterBoxEnhanced) {
        var key = filterBoxEnhanced.val();
        return self.autocompleteUrls[keyword(key)];
    }

    function termToSearch(filterBoxEnhanced){
        var key = filterBoxEnhanced.val();
        var val = jQuery.trim(key).substring(key.indexOf(":") + 1);
        return jQuery.trim(val);
    }

    init.prototype.hookupAutocomplete = function() {
        var filterBoxEnhanced = jQuery(this.filterBox);
        var self = this;
        filterBoxEnhanced.autocomplete(null, {
            width: filterBoxEnhanced.outerWidth(),
            cacheLength: 0,
            selectFirst: false,
            formatResult: function(row) {
                return keyword(filterBoxEnhanced.val()) + ":" + row;
            },
            dataLoader: function(options, term, success, input, parse) {
                if (!readyToRequest(filterBoxEnhanced)) {
                    return;
                }
                var url = urlToUse(self, filterBoxEnhanced);
                if (!url) return;
                jQuery.ajax({
                    // try to leverage ajaxQueue plugin to abort previous requests
                    mode: "abort",
                    // limit abortion to this input
                    data: { "q" : termToSearch(filterBoxEnhanced)},
                    port: "autocomplete" + input.name,
                    url: url,
                    dataType: "text",
                    success: function(data) {
                        success(term, parse(data));
                    }
                });
            }
        });
    };

    return init;
}();