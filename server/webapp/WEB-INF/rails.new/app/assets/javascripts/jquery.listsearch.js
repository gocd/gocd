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

var deBounceHandler = {
    deBounceSearch: function (callback) {
        return jQuery.debounce(250, callback)
    }
};

(function ($) {
    $.fn.listsearch = function (options) {
        var settings = $.extend({
            elementsClass: '',
            highlight: false,
            searchForTextIn: '.text',
            clearButton: '',
            highlightClass: ''
        }, options);

        var targetElements = settings.elementsClass;
        var clearButtonClass = settings.clearButton;
        var searchForTextIn = settings.searchForTextIn;
        var highlightClass = settings.highlightClass;

        var textBoxContents = function() {
            return textBox.val().trim();
        };

        var ClearButton = function(textboxToAddClearButtonTo, clearButtonClass) {
            var clearSearch = function() {
                textboxToAddClearButtonTo.val('').trigger('keyup').focus();
            };

            var hookUpEvents = function(clearButton) {
                clearButton.click(function () { clearSearch(); });

                textboxToAddClearButtonTo.keyup(function (e) {
                    if (e.keyCode === 27) {
                        clearSearch();
                    }
                });
            };

            var showIfNecessary = function() {
                $('.' + clearButtonClass).hide();
                if (textBoxContents().length > 0) {
                    $('.' + clearButtonClass).show();
                }
            };

            var initialize = function () {
                if (clearButtonClass && clearButtonClass.length > 0) {
                    textboxToAddClearButtonTo.parent().prepend("<button title='Clear' class='icon-pipeline " + clearButtonClass + "'></button>");

                    var clearButton = $('.' + clearButtonClass);
                    clearButton.hide();
                    hookUpEvents(clearButton);
                }
            };

            return {
                initialize: initialize,
                showIfNecessary: showIfNecessary
            };
        };

        var CountHolder = function(textboxToAddCountHolderTo) {
            var countTextHolder;
            var countHolder;

            var showIfNecessary = function() {
                countTextHolder.hide();
                if (textBoxContents().length > 0) {
                    countTextHolder.show();
                }
            };

            var setCountTo = function(countToSet) {
                countHolder.text(countToSet);
            };

            var initialize = function() {
                textboxToAddCountHolderTo.parent().append("<div style='background: #fff8c1; left: 0; padding: 5px; position: absolute; right: 10px; top: 24px; display:none;' class='count'>Total <span id='found'></span> matches found.</div>");
                countTextHolder = textboxToAddCountHolderTo.siblings('.count');
                countHolder = $(countTextHolder).find("#found");
            };

            return {
                setCountTo: setCountTo,
                showIfNecessary: showIfNecessary,
                initialize: initialize
            };
        };

        var search = function() {
            $(searchForTextIn).unhighlight({className: highlightClass});
            $(targetElements).css('display', 'block');
            $(targetElements).parent().css('display', 'block');

            var q = textBoxContents();
            if (q && q.length > 0) {
                $(targetElements).css('display', 'none');
                $(targetElements).parent().css('display', 'none');
                $(searchForTextIn).highlight(q, {className: highlightClass});

                var highlightedTargetElements = $(searchForTextIn + " ." + highlightClass).parents(targetElements);
                highlightedTargetElements.css('display', 'block');
                highlightedTargetElements.parent().css('display', 'block');

                countHolder.setCountTo(highlightedTargetElements.length);
            }
        };

        var doSearch = function() {
            search();
            clearButton.showIfNecessary();
            countHolder.showIfNecessary();
        };

        var textBox = this;
        textBox.wrap("<div class='search-with-clear'></div>");

        var clearButton = new ClearButton(textBox, clearButtonClass);
        var countHolder = new CountHolder(textBox);

        clearButton.initialize();
        countHolder.initialize();

        textBox.keyup(deBounceHandler.deBounceSearch(doSearch));

        return {
            forceSearch: doSearch
        }
    };

}(jQuery));