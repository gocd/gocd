/*
 * Copyright 2022 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
Util = function() {
    return {
        really_stop_propagation_and_default_action: function(event) {//this one really really stops propagation on IE and other lesser privileged browsers, this is the only reliable way in the whole of milky-way to stop events on IE. -Sara & JJ
            var jq_evt = jQuery.Event(event);
            jq_evt.stopPropagation();
            jq_evt.preventDefault();
            jq_evt.stopImmediatePropagation();
        },

        on_load: jQuery,

        loadPage: function(url) {
            window.location = url;
        },

        escapeDotsFromId: function(theId) {
            return '#' + theId.replace(/(:|\.)/g,'\\$1');
        },
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
