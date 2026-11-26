/*
 * Copyright Thoughtworks, Inc.
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
  constructor(trans_message, targetElement, options) {
    this.targetElement = $(targetElement);
    this.options = {
      height: this.targetElement.height() * 0.9,
      offsetTop: 0,
      offsetLeft: this.targetElement.width() * 0.1,
      ...(options || {})
    };
    this.trans_message = $(Util.idToSelector(trans_message));
    this.trans_message.css({
      'position': 'absolute',
      'top': `${this.targetElement.position().top + this.options.offsetTop}px`,
      'left': `${this.targetElement.position().left + this.options.offsetLeft}px`,
      'height': this.options.height,
      'width': `${this.targetElement.width() * 0.8}px`
    });
    this.trans_message.show();
  }
}
