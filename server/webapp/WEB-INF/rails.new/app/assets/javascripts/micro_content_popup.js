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

MicroContentPopup = function() {

    var opened_popup = null;
    var panels = [];
    var handlers = [];
    var cleanupList = [];
    var ignoreNextClose = false;

    Event.observe(window, 'load', function () {
        init.register();
    });

    init.register = function(){
        Event.observe($(document), 'click', close_opened_popup);
    }

    function reset_opened_popup(new_popup) {
        close_opened_popup();
        opened_popup = new_popup;
    }

    function find_element(self, selector) {
        return self.panel && find_elements(self, selector)[0];
    }

    function find_elements(self, selector) {
        return self.panel.getElementsBySelector(selector);
    }

    function close_opened_popup() {
        if (ignoreNextClose) {
            ignoreNextClose = false;
            return;
        }
        if (opened_popup) {
            opened_popup.close();
        }
    }

    function is_open(self) {
        return self === opened_popup;
    }

    function open_box(self, event, button_dom, relative_to_dom) {
        reset_opened_popup(self);
        self.callback_handler.before_show(event);
        show_panel(self, $(button_dom), relative_to_dom);
        Util.really_stop_propagation_and_default_action(event);
    }

    function adjust_coordinate(n, offset) {
        return (n > 0) ? n - offset : -offset;
    }

    function adjust_view_port(left, top, body_content) {
        var body_port = body_content.viewportOffset();
        return [adjust_coordinate(left, body_port[0]), adjust_coordinate(top, body_port[1])];
    }

    function show_panel(self, button_dom, relative_to_dom) {
        var view_port = button_dom.viewportOffset();
        var body_content = (relative_to_dom === undefined) ? $(document.body) : $(relative_to_dom);//TODO: im a mistake, don't assume this element is always present
        view_port = adjust_view_port(view_port[0], view_port[1] + button_dom.getDimensions().height, body_content);
        if (body_content) {
            //check to see if left + width > screen width.  If so subtract that diff from left
            var body_content_right = body_content.positionedOffset().left + body_content.getWidth();
            var self_right = view_port[0] + self.panel.getWidth();
            if (self_right > body_content_right) {
                view_port[0] = view_port[0] - (self_right - body_content_right);
            }

            var body_content_bottom = document.viewport.getScrollOffsets() + document.viewport.getHeight();
            var self_bottom = view_port[1] + self.panel.getHeight();
            if (self_bottom > body_content_bottom) {
                view_port[1] = view_port[1] - (self_bottom - body_content_bottom);
            }
        }

        self.panel.style.left = view_port[0] + 'px';
        self.panel.style.top = view_port[1] + 'px';
        self.panel.removeClassName('hidden');
    }

    function register_close_handler(self) {
      jQuery(self.panel).click(function(event) { self.callback_handler.allow_propagation_of(event) || event.stopPropagation(); });
    }

    function init(panel, callback_handler, options) {
        panels.push(panel);
        handlers.push(callback_handler);
        var self = this;
        this.panel = $(panel);
        this.loadingDiv = find_element(self, ".loading");
        this.callback_handler = callback_handler;
        this.callback_handler.after_attach(this);
        this.options = options || {};
        this.showButtons = [];
        register_close_handler(this);
    }

    init.prototype.cleanup = function(){
        this.panel.stopObserving();
    };

    init.prototype.close = function(){
        this.panel.addClassName('hidden');
        this.callback_handler.after_close();
        opened_popup = null;
    };

    init.lookupHandler = function(child_element_dom) {
        var ancestors = child_element_dom.ancestors();
        for (var i = 0; i < panels.length; i++) {
            if (ancestors.include(panels[i])) {
                return handlers[i];
            }
        }
        return null;
    };

    function noOp() {}

    init.NoOpHandler = function () {
        function handler_init() {}
        handler_init.prototype.after_close = noOp;
        handler_init.prototype.after_attach = noOp;
        handler_init.prototype.before_show = noOp;
        handler_init.prototype.on_popup_population = noOp;
        handler_init.prototype.allow_propagation_of = function(event) {
            var dom = event.target;
            return (dom.tagName.toLowerCase() === "a") || !!$(dom).up("a");
        };
        return handler_init;
    }();

    init.PropagateAllHandler = function() {};
    init.PropagateAllHandler.prototype = new init.NoOpHandler();
    init.PropagateAllHandler.prototype.allow_propagation_of = function() {
        return true;
    };

    init.NeverCloseHandler = function() {};

    init.NeverCloseHandler.prototype = new init.NoOpHandler();

    init.NeverCloseHandler.prototype.allow_propagation_of = function() {
        return false;
    };

    init.Shower = function () {
        function shower_init(popup) {
            this.popup = popup;
        }

        shower_init.prototype.toggle_popup = function(event, button_dom, relative_to_dom) {
            is_open(this.popup) ? this.close() : open_box(this.popup, event, button_dom, relative_to_dom);
        };

        shower_init.prototype.open = function(event, button_dom) {
            open_box(this.popup, event, button_dom);
        };

        shower_init.prototype.close = function() {
            close_opened_popup();
        };

        shower_init.prototype.cleanup = function() {
            this.popup.cleanup();
        };

        return shower_init;
    }();

    init.ClickShower = function() {
        function click_shower_init(popup, options) {
            this.options = options || {};
            this.popup = popup;
            this.showButtons = [];
        }

        click_shower_init.prototype = new init.Shower();

        click_shower_init.prototype.bindShowButton = function(button_dom, relative_to_dom) {
            var self = this;
            if (this.options.cleanup) {
                this.showButtons.push(button_dom);
            }
            Event.observe(button_dom, 'click', function(event) {
                self.toggle_popup(event, button_dom, relative_to_dom);
            });
        };

        click_shower_init.prototype.cleanup = function() {
            init.Shower.prototype.cleanup.call(this);
            this.cleanup_buttons();
        };

        click_shower_init.prototype.cleanup_buttons = function() {
            this.showButtons.each(function(button){button.stopObserving();});
            this.showButtons.clear();
        };

        return click_shower_init;
    }();

    init.LiveShower = function() {
        function live_shower_init(popup) {
            this.last_target = null;
            this.popup = popup;
        }

        live_shower_init.prototype = new init.Shower();

        live_shower_init.prototype.open = function(event, button_dom) {
            init.Shower.prototype.open.call(this, event, button_dom);
            ignoreNextClose = true;
        };

        live_shower_init.prototype.toggle_popup = function(event, button_dom) {
            if (is_open(this.popup) && (event.target == this.last_target)) {
                Util.really_stop_propagation_and_default_action(event);
                this.close();
            } else {
                this.open(event, button_dom);
            }
            this.last_target = event.target;
        };

        return live_shower_init;
    }();

    init.NeverCloseHandler = function() {};

    init.NeverCloseHandler.prototype = new init.NoOpHandler();

    init.NeverCloseHandler.prototype.allow_propagation_of = function() {
        return false;
    };

    return init;
}();
