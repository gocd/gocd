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

EnvironmentVariables = function() {

    function captureAndStopPropagation(self) {
        if (!insideModalBox(self)) {
            return;
        }

        function handle_tabbing(e) {
            if (e.keyCode === Event.KEY_TAB && jQuery(e.target).attr('tagName') === 'INPUT') {
                var currentFocusElement = e.target;
                var modifierPressed = e.shiftKey;
                var next = findNextFocusElement(self, currentFocusElement, modifierPressed);
                next && jQuery(next).focus();

                e.stopPropagation();
                e.preventDefault();
            }
        }
        function insideModalBox(self) {
            return self.container.parents(Util.MB_CONTENT).size() == 1;
        }

        if (Prototype.Browser.Gecko) {
            self.container.keypress(handle_tabbing);
        } else {
            self.container.keyup(handle_tabbing);
        }
    }

    function findNextFocusElement(self, currentFocusElement, modifierPressed) {
        var found_at = null;
        var next = null;
        var inputFormFields = self.container.find("input.form_input");
        inputFormFields.each(function(index) {
            if (this === currentFocusElement) {
                found_at = index;
            }
        });
        if (found_at !== null) {
            next = inputFormFields.get(modifierPressed ? found_at - 1 : found_at + 1);
        }
        return next;
    }

    function init(container, row_creator, validators, postAddCallback, onRemoveCallback) {
        this.container = container;
        this.row_creator = row_creator;
        this.rows = jQuery([]);
        this.validators = validators;
        this.validators && this.validators.rows(this.rows);
        this.postAddCallback = postAddCallback;
        this.onRemoveCallback = onRemoveCallback;
        captureAndStopPropagation(this);
        var self = this;
        container.children().each(function() {
            var row = jQuery(this);
            self.row_creator.registerExistingRow(row, removerFunction(self));
            addRowForValidations(self, row);
        });
    }

    init.prototype.registerAddButton = function(button) {
        var self = this;
        button.click(function() {self.addDefaultRow();});
    };

     init.prototype.addDefaultRow = function(row) {
         add(this);
         jQuery(".has_go_tip").tipTip({activation : "click"});
     };

    init.prototype.registerFinishButton = function(button) {
        var self = this;
    };

    function removerFunction(self) {
        return function(row) {
            if (self.onRemoveCallback) {
                self.onRemoveCallback(row);
            }
            Util.remove_from_array(self.rows, row);
        };
    }

    function add(self) {
        var row = self.row_creator.createRow(removerFunction(self));
        addRowForValidations(self, row);
        self.container.append(row);
        forceFocusOnNewRow(row);
        if (self.postAddCallback) {
            self.postAddCallback(row);
        }
    }

    function addRowForValidations(self, row) {
        self.validators && self.validators.wireValidationCallbacks(row);
        self.rows.push(row);
    }

    function forceFocusOnNewRow(row) {
        var firstTextField = row.find('input.form_input').get(0);
        jQuery(firstTextField).focus();
    }

    init.RowCreator = function(template_element, wrapper_element_type, remove_button_selector, useInnerHtmlInsteadOfTextFromTemplate) {
        this.template_text = useInnerHtmlInsteadOfTextFromTemplate ? template_element.html() : template_element.text();
        this.remove_button_selector = remove_button_selector;
        this.wrapper_element_type = wrapper_element_type;
    };

    init.RowCreator.prototype.createRow = function(on_remove) {
        var row = createWrapperElement(this.wrapper_element_type, this.template_text);
        this.registerExistingRow(row, on_remove);
        return row;
    };

    init.RowCreator.prototype.registerExistingRow = function(row, on_remove) {
        row.find(this.remove_button_selector).click(function() {
            on_remove(row);
            row.remove();
        });
    };

    function createWrapperElement(tag_type, template_text) {
        if(tag_type){
          return jQuery(document.createElement(tag_type)).html(template_text);
        } else {
          return jQuery(template_text);
        }
    }

    function mapAssoc(array, fn) {
        var mapped = {};
        for(var key in array) {
            mapped[key] = fn(array[key], key);
        }
        return mapped;
    }

    init.Validators = function(selector_events_mapping, validators) {
        this.selector_events_mapping = mapAssoc(selector_events_mapping, jQuery);
        this.validators = jQuery(validators);
    };

    init.Validators.prototype.rows = function(rows) {
        this.rows = rows;
    };

    init.Validators.prototype.wireValidationCallbacks = function(row) {
        var self = this;
        mapAssoc(this.selector_events_mapping, function(events, selector) {
            events.each(function() {
                row.find(selector).bind(this, function() {
                    self.isValid();
                });
            });
        });
    };

    init.Validators.prototype.isValid = function() {
        var self = this;
        var isValid = true;
        this.rows.each(function() {
            var row = this;
            var isRowValid = true;
            self.validators.each(function() {
                isRowValid = isRowValid && this.validate(row, self.rows);
            });
            isValid = isValid && isRowValid;
        });
        return isValid;
    };

    init.BaseValidator = function(error_field_selector) {
        this.error_field_selector = error_field_selector;
        this.error_message = ".error_message";
    };

    init.BaseValidator.prototype.validate = function(row, rows) {
        var isValid = this.isValid(row, rows);
        var message = isValid ? '' : this.error_message;
        setValidationMessage(this, row, message);
        return isValid;
    };

    function setValidationMessage(self, row, message) {
        row.find(self.error_field_selector).html(message);
    }

    function name(self, row) {
        return row.find(self.name_selector).val();
    }

    function assign_validator_fields(self, name_selector, error_message) {
        self.name_selector = name_selector;
        self.error_message = error_message;
    }

    return init;
}();