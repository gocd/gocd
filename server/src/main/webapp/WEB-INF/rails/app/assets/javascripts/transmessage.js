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

class TransMessage {
  static TYPE_NOTICE = "notice";
  static TYPE_ERROR = "error";

  constructor(trans_message, targetElement, options) {
    this.options = {
      type: TransMessage.TYPE_NOTICE,
      autoHide: true,
      height: $(targetElement).height() * 0.9,
      hideDelay: 2,
      offsetTop: 0,
      offsetLeft: 0,
      ...(options || {})
    };
    this.trans_message = $(Util.idToSelector(trans_message));
    this.targetElement = $(targetElement);
    this.changeType();
    this.show();
  }

  show() {
    if (this.trans_message.is(':visible')) {
      return;
    }

    this.options.offsetLeft = this.targetElement.width() * 0.1;
    this.trans_message.css({
      'position': 'absolute',
      'top': (this.targetElement.position().top + this.options.offsetTop) + 'px',
      'left': (this.targetElement.position().left + this.options.offsetLeft) + 'px',
      'height': this.options.height,
      'width': this.targetElement.width() * 0.8 + 'px'
    });
    this.trans_message.show();

    if (this.options.autoHide) {
      setTimeout(this.hide.bind(this), this.options.hideDelay * 1000);
    }
  }

  hide() {
    this.trans_message.hide();
  }

  changeType() {
    const _me = this;
    this.trans_message.find(".r1, .r2, .r3, .r4, .transparent_message").each(function (i, elem) {
      if (_me.options.type === TransMessage.TYPE_NOTICE) {
        $(elem).removeClass("transparent_" + TransMessage.TYPE_ERROR);
        $(elem).addClass("transparent_" + TransMessage.TYPE_NOTICE);
      } else {
        $(elem).removeClass("transparent_" + TransMessage.TYPE_NOTICE);
        $(elem).addClass("transparent_" + TransMessage.TYPE_ERROR);
      }
    });
  }
}
