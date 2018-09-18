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

FormFieldsTable = function() {

    function hide(self) {
        return function() {
            jQuery(self.tableSelector).addClass("hidden");
            jQuery(self.emptyTableMessageContainer).removeClass("hidden");
        };
    }

    function init(options) {
        this.addControl = options.addControl;
        this.fields = options.fields;
        this.rowManager = options.rowManager;
        this.appendTo = options.appendTo;
        this.errorMessageContainer = options.errorMessageContainer;
        this.valuePreProcessor = options.valuePreProcessor || function(x, y) {return {name: x, value: y};};
        this.emptyTableMessageContainer = options.emptyTableMessageContainer;
        this.tableSelector = options.tableSelector;
        var self = this;
        this.rowManager.hide(hide(this));
        this.addControl.click(function() {
            clearMessage(self);
            createNewRow(self);
        });
    }

    function clearMessage(self) {
        self.errorMessageContainer.html('');
    }
    
    function createNewRow(self) {
        var fieldValues = {};
        self.fields.each(function(field) {
            var fieldTuple = self.valuePreProcessor(field.attr('name'), field.val());
            fieldValues[fieldTuple.name] = fieldTuple.value;
        });
        var dom = self.rowManager.rowDom(fieldValues);
        if (dom !== null) {
            self.appendTo.append(dom);
            clearFieldText(self.fields);
            jQuery(self.emptyTableMessageContainer).addClass("hidden");
            jQuery(self.tableSelector).removeClass("hidden");
        }
    }

    function clearFieldText(fields) {
        fields.each(function(field) {
            field.val("");
        });
    }

    init.Row = function(templateMarkup, sorroundingElem, validator) {
        this.templateMarkup = templateMarkup;
        this.sorroundingElem = sorroundingElem;
        this.validator = validator;
    };

    init.Row.prototype.hide = function(hideFn) {
        this.hideFn = hideFn;
    };

    init.Row.prototype.preProcess = function() {
    };

    init.Row.prototype.rowDom = function (fieldValues) {
        if (!this.validator.isValid(fieldValues)) {
            return null;
        }
        var interpolatedMarkup = this.templateMarkup;
        for (var name in fieldValues) {
            if (name) {
                var namePattern = "\\$\\{" + name + "\\}";
                interpolatedMarkup = interpolatedMarkup.replace(RegExp(namePattern, "g"), fieldValues[name]);
            }
        }
        var elem = jQuery(document.createElement(this.sorroundingElem));
        elem.html(interpolatedMarkup);
        this.preProcess(elem, fieldValues);
        return elem;
    };

    init.DeleteableRow = function(templateMarkup, sorroundingElem, validator) {
        this.templateMarkup = templateMarkup;
        this.sorroundingElem = sorroundingElem;
        this.validator = validator;
    };

    init.DeleteableRow.prototype = new init.Row('', '', null);

    init.DeleteableRow.prototype.preProcess = function(dom, fieldValues) {
        var self = this;
        jQuery(dom).find('.delete_action').click(function() {
            dom.remove();
            if (jQuery('.delete_action').length === 0) {
                self.hideFn();
            }
            self.validator.removed(fieldValues);
            return false;
        });
    };
    return init;
}();
