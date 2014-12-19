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

EditPopupHandler = function() {

    function has_element(self, selector) {
        return !!find_element(self, selector);
    }

    function find_new_field(self) {
        return find_element(self, ".new_field");
    }

    function find_element(self, selector) {
        return self.popup.panel.down(selector);
    }

    function show_error_message(self, should_show) {
        show_error(self, ".no_selection_error", should_show);
    }

    function show_error(self, error_selector, should_show) {
        show_element(self, error_selector, should_show);
        show_element(self, ".add_panel", !should_show);
        show_element(self, ".scrollable_panel", !should_show);
    }

    function show_element(self, selector, show) {
        var element = find_element(self, selector);
        if (show) {
            element.removeClassName('hidden');
        } else {
            element.addClassName('hidden');
        }
    }

    function init(load_url, form, are_rows_selected_function, validator_function, operation_field_name, apply_operation_name, add_operation_name) {
        this.default_text_fields = [];
        this.form = form;
        this.selector_url = load_url;
        this.has_new_field = !!validator_function;
        this.validator_function = validator_function;
        this.are_rows_selected_function = are_rows_selected_function;
        this.convert_to_apply = Util.set_value(operation_field_name, apply_operation_name);
        this.convert_to_add = Util.set_value(operation_field_name, add_operation_name);
    }

    init.prototype.after_attach = function(popup) {
        this.popup = popup;
    };

    init.prototype.before_show = function() {
        var self = this;
        if (this.are_rows_selected_function()) {
            show_error_message(self, false);
            var checkbox_panel = find_element(self, '.scrollable_panel');
            checkbox_panel.innerHTML = "";
            checkbox_panel.appendChild(self.popup.loadingDiv);
            var options = Form.serialize(self.form, true);
            AjaxRefreshers.disableAjax();
            new Ajax.Updater(checkbox_panel, self.selector_url, {parameters: options, evalScripts: true,onComplete: function() {
                self.on_popup_population();
                AjaxRefreshers.enableAjax();
            }});
            show_error_message(self, false);
        } else {
            show_error_message(self, true);
        }
    };

    init.prototype.on_popup_population = function() {};
    
    init.prototype.after_close = function(){};

    init.prototype.allow_propagation_of = function(event) {
        return event.target.hasClassName('apply_button') || !!event.target.up('.apply_button');
    };

    init.AddOnlyHandler = function() {
        sub_init.prototype = new init();

        function sub_init() {
            init.apply(this, arguments);
        }

        sub_init.prototype.on_popup_population = function() {
            show_error(this, ".no_environments_error", !has_element(this, ".selectors"));
        };

        return sub_init;
    }();


    init.AddEditHandler = function() {

        function validate_resource_name(self, new_field_validator) {
            show_element(self, ".validation_message", !new_field_validator(find_new_field(self).getValue()));
        }

        function apply_resources_button(self) {
            return find_element(self, '.apply_button');
        }

        function clear_all_default_text_fields(self) {
            self.default_text_fields.each(function(default_text_model) {
                default_text_model.clear();
            });
        }

        function switch_to_modify_mode(self) {
            var button = apply_resources_button(self);
            button.stopObserving('click', self.convert_to_add);
            button.observe('click', self.convert_to_apply);
            button.innerHTML = "<span>Apply</span>";
            find_new_field(self).addClassName("hidden");
        }

        function switch_to_add_mode(self) {
            var button = apply_resources_button(self);
            button.stopObserving('click', self.convert_to_apply);
            button.observe('click', self.convert_to_add);
            button.innerHTML = "<span>Add</span>";
            find_new_field(self).removeClassName('hidden');
        }

        function sub_init() {
            init.apply(this, arguments);
        }

        sub_init.prototype = new init();

        sub_init.prototype.after_attach = function() {
            init.prototype.after_attach.apply(this, arguments);

            var self = this;

            Event.observe(apply_resources_button(this), 'click', function() {
                clear_all_default_text_fields(self);
            });
            Event.observe(find_new_field(this), "keyup", function () {
                validate_resource_name(self, self.validator_function);
            });
            find_new_field(this).observe("keydown", function (evt) {
                if (evt.keyCode == Event.KEY_RETURN) {
                    apply_resources_button(self).click();
                }
                return true;
            });
        };

        sub_init.prototype.after_close = function(){};

        sub_init.prototype.before_show = function() {
            init.prototype.before_show.call(this);
            switch_to_add_mode(this);
        };

        sub_init.prototype.setDefaultText = function(field, default_text) {
            this.default_text_fields.push(new DefaultText(field, default_text));
        };

        sub_init.prototype.tristate_clicked = function() {
            switch_to_modify_mode(this);
        };

        return sub_init;
    }();

    return init;
}();
