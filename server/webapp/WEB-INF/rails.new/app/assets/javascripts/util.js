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

Util = function() {
    var namespaces = {};

    function enable(element, enabled) {
        var elem = $(element);
        elem.disabled = !enabled;
        enabled ? elem.removeClassName("disabled") : elem.addClassName("disabled");
    }

    function namespace(key) {
        return namespaces[key] || (namespaces[key] = $H());
    }

    return {
        disable_or_enable_submittable_fields: function(submittable_field_ids) {
            return function() {
                var should_enable = this.checked;
                for(var i = 0; i < submittable_field_ids.length; i++) {
                    enable(submittable_field_ids[i], should_enable);
                }
            };
        },
        really_stop_propagation_and_default_action: function(event) {//this one really really stops propagation on IE and other lesser privileged browsers, this is the only reliable way in the whole of milky-way to stop events on IE. -Sara & JJ
            var jq_evt = jQuery.Event(event);
            jq_evt.stopPropagation();
            jq_evt.preventDefault();
            jq_evt.stopImmediatePropagation();
        },
        domUpdatingCallback: function(name_domid_mapping, container, name_reader) {
            return function() {
                var name = name_reader.apply(this);
                container.html(jQuery('#' + name_domid_mapping[name]).text());
            };
        },
        on_load: jQuery,
        loadPage: function(url) {
            window.location = url;
        },
        buildCausePopupCreator: function(popup_panel_id) {
            return function() {
                var node = $(popup_panel_id);
                var microContentPopup = new MicroContentPopup(node, new MicroContentPopup.NeverCloseHandler());
                var shower = new MicroContentPopup.ClickShower(microContentPopup, {cleanup: true});
                Util.namespace('build_cause').set(popup_panel_id, shower);
            };
        },
        set_value: function(field, value) {
            return function() {
                $(field).value = value;
            };
        },
        disable: function(element) {
            enable(element, false);
        },
        enable: function(element) {
            enable(element, true);
        },

        timestamp_in_millis: function(timestamp) {
            return new Date(timestamp * 1000).getTime();
        },

        refresh_child_text: function(id, text, className) {
            $(id).getElementsBySelector("p").each(function(p) {
                $(id).removeChild(p);
            });
            var childElement = document.createElement("p");
            childElement.innerHTML = text;
            jQuery(childElement).addClass(className);
            $(id).appendChild(childElement);
        },
        validate_name_type: function(name) {
            return /^[a-zA-Z0-9_\-][a-zA-Z0-9_\-.]*$/.test(name);
        },
        are_any_rows_selected : function(class_selector) {
            return function() {
                var has_rows_selected = false;
                $(document.body).getElementsBySelector(class_selector).each(function(checkbox) {
                    has_rows_selected = has_rows_selected || checkbox.checked;
                });
                return has_rows_selected;
            };
        },
        remove_from_array: function(array, element_to_be_removed) {
            var index = jQuery.inArray(element_to_be_removed, array);
            if (index != -1) {
                array.splice(index, 1);
            }
        },

        escapeDotsFromId: function(theId) {
            return '#' + theId.replace(/(:|\.)/g,'\\$1');
        },

        namespace: namespace,

        flash_message: function(text) {
            return "<p class='error'>" + text + "</p>";
        },

        ajax_modal: function() {
            function create_default_modal_body_wrapper(text) {
                return '<div class="ajax_modal_body flash">' + Util.flash_message(text) + '</div>';
            }

            return function(url, options, error_create_wrapper, use_modal_box_with_special_properties) {
                options.autoFocusing = false; //doesn't work in IE without this.
                var request = jQuery.ajax({url: url});
                request.done(function() {
                    if(use_modal_box_with_special_properties){
                          ModalBoxWhichClosesAutoCompleteDropDownBeforeClosing.show(request.responseText, options);
                    }
                    Modalbox.show(request.responseText, options);
                });
                request.fail(function() {
                    Modalbox.show((error_create_wrapper || create_default_modal_body_wrapper)(request.responseText), options);
                });
            };
        }(),

        MB_CONTENT: '#MB_content',

        client_timestamp: function() {
            return Math.round(((new Date()).getTime() - Date.UTC(1970, 0, 1)) / 1000);
        },

        bindPasswordField: function(checkBox, passwordField) {
            passwordField.val("**********");
            checkBox.click(function() {
                var isChecked = checkBox.is(":checked");
                if (isChecked) {
                    passwordField.removeAttr("disabled");
                    passwordField.val("");
                } else {
                    passwordField.attr("disabled", true);
                    passwordField.val("**********");
                }
            });
        },

        server_timestamp: function() {
            var client_server_timestamp_delta = null;

            function compute_delta() {
                var server_timestamp = jQuery('#server_timestamp').val();
                return Util.client_timestamp() - server_timestamp;
            }

            function enforce_delta_computation() {
                if (client_server_timestamp_delta === null) {
                    client_server_timestamp_delta = compute_delta();
                }
            }

            //            Util.on_load(enforce_delta_computation);
            return function() {
                enforce_delta_computation();
                return Util.client_timestamp() - client_server_timestamp_delta;
            };
        }(),

        click_load: function(options) {
            Util.on_load(function() {
                var target = options.target;
                var url = options.url;
                var update = options.update;
                var spinnerContainer = options.spinnerContainer;
                jQuery(target).click(function(_) {
                    jQuery.ajax({
                        url: url,
                        beforeSend: function() {
                           spinny(spinnerContainer);
                        },
                        success: function(html) {
                            jQuery(update).html(html);
                        },
                        complete: function() {
                           removeSpinny(spinnerContainer);
                        }
                    });
                });
            });
        }
    };
}();

Util.on_load(function() {
    jQuery('.close_modalbox_control').live('click', function() {
        Modalbox.hide();
        return false;
    });
});

ViewportPredicate = function() {
    var y, x, dy, dx;

    jQuery(window).resize(reset_caches);
    jQuery(window).scroll(reset_offsets);
    jQuery(reset_caches);

    function reset_caches() {
        dy = jQuery(window).height();
        dx = jQuery(window).width();
        reset_offsets();
    }

    function reset_offsets() {
        y = jQuery(document).scrollTop();
        x = jQuery(document).scrollLeft();
    }

    function port_y() {
        return y;
    }

    function port_x() {
        return x;
    }

    function port_dy() {
        return dy;
    }

    function port_dx() {
        return dx;
    }

    function is_in_viewport(elem) {
        var offset = elem.offset();
        var y_off = port_y();
        var height = port_dy();
        var y = offset.top;
        var y_in_view = ((y >= y_off) && (y <= y_off + height)) || ((y <= y_off) && ((y + elem.height()) >= y_off));
        if (y_in_view) {
            var x_off = port_x();
            var width = port_dx();
            var x = offset.left;
            return ((x >= x_off) && (x <= x_off + width)) || ((x <= x_off) && ((x + elem.width()) >= x_off));
        }
        return false;
    }

    function in_y(min, max) {
        var y_off = port_y();
        var height = port_dy();
        return ((min >= y_off) && (min <= y_off + height)) || ((min <= y_off) && (max >= y_off));
    }

    return {
        dom_visible: function(elem) {
            var jq_elem = jQuery(elem);
            return is_in_viewport(jq_elem);
        },
        in_y: in_y
    };
}();
