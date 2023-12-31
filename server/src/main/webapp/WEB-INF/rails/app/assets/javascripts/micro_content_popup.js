/*
 * Copyright 2024 Thoughtworks, Inc.
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
MicroContentPopup = function() {

  let opened_popup = null;
  let ignoreNextClose = false;

  window.addEventListener('load', function () {
    init.register();
  });

  init.register = function(){
    $(document).on('click', close_opened_popup.bind(this));
  };

  function reset_opened_popup(new_popup) {
    close_opened_popup();
    opened_popup = new_popup;
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

  function open_box(self, event, button_dom) {
    reset_opened_popup(self);
    show_panel(self, $(button_dom));
    event.preventDefault();
    event.stopPropagation();
    event.stopImmediatePropagation();
  }

  function adjust_coordinate(n, offset) {
    return (n > 0) ? n - offset : -offset;
  }

  function viewportOffset(element) {
    const offset = element.offset();
    return [offset.left - $(window).scrollLeft(), offset.top - $(window).scrollTop()];
  }

  function adjust_view_port(left, top, body_content) {
    const body_port = viewportOffset(body_content);
    return [adjust_coordinate(left, body_port[0]), adjust_coordinate(top, body_port[1])];
  }

  function show_panel(self, button_dom) {
    let view_port = viewportOffset(button_dom);
    const body_content = $(document.body);
    view_port = adjust_view_port(view_port[0], view_port[1] + button_dom.height(), body_content);

    if (body_content.length > 0) {
      //check to see if left + width > screen width.  If so subtract that diff from left
      const body_content_right = body_content.position().left + body_content.width();
      const self_right = view_port[0] + self.panel.width();
      if (self_right > body_content_right) {
        view_port[0] = view_port[0] - (self_right - body_content_right);
      }
    }

    self.panel.css('left', view_port[0] + 'px');
    self.panel.css('top', view_port[1] + 'px');
    self.panel.removeClass('hidden');
  }

  function register_close_handler(self) {
    $(self.panel).click(function(event) {
      if ((event.target.tagName.toLowerCase() !== "a") && !$(event.target).closest("a")) {
        event.stopPropagation();
      }
    });
  }

  function init(panel) {
    this.panel = panel;
    register_close_handler(this);
  }

  init.prototype.close = function(){
    this.panel.addClass('hidden');
    opened_popup = null;
  };

  init.ClickShower = function() {
    function click_shower_init(popup) {
      this.popup = popup;
    }

    click_shower_init.prototype.toggle_popup = function(event, button_dom) {
      is_open(this.popup) ? this.close() : open_box(this.popup, event, button_dom);
    };

    click_shower_init.prototype.open = function(event, button_dom) {
      open_box(this.popup, event, button_dom);
    };

    click_shower_init.prototype.close = function() {
      close_opened_popup();
    };

    click_shower_init.prototype.bindShowButton = function(button_dom) {
      var self = this;
      $(button_dom).on('click', function(event) {
        self.toggle_popup(event, button_dom);
      });
    };

    return click_shower_init;
  }();

  return init;
}();
