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

var TransMessage = Class.create();

TransMessage.TYPE_NOTICE = "notice";
TransMessage.TYPE_ERROR  = "error";

TransMessage.prototype = {
    initialize : function(trans_message, targetElement) {
        this.options = Object.extend({
            type       : TransMessage.TYPE_NOTICE,
            autoHide   : true,
            height     : $(targetElement).getHeight() * 0.9,
            hideDelay  : 2,
            offsetTop  : 0,
            offsetLeft : 0
        }, arguments[2] || { });
        this.trans_message = $(trans_message);
        this.targetElement = $(targetElement);
        this.changeType();
        this.show();
    },
    show : function() {
        if (this.trans_message.visible()) {
            return;
        }
        this.options.offsetLeft = $(this.targetElement).getWidth() * 0.1;
        Element.clonePosition(this.trans_message, this.targetElement,this.options);
        this.trans_message.style.width =  $(this.targetElement).getWidth() * 0.8 + 'px';
        this.trans_message.show();
        if (this.options.autoHide) {
            this.hide.delay(this.options.hideDelay, this);
        }
    },
    hide : function(obj) {
        obj.trans_message.hide();
    },
    changeType : function() {
        var elements = this.trans_message.select(".r1, .r2, .r3, .r4, .transparent_message");
        var _me = this;
        elements.each(function(elem) {
            if (_me.options.type == TransMessage.TYPE_NOTICE) {
                elem.removeClassName("transparent_" + TransMessage.TYPE_ERROR);
                elem.addClassName("transparent_" + TransMessage.TYPE_NOTICE);
            } else {
                elem.removeClassName("transparent_" + TransMessage.TYPE_NOTICE);
                elem.addClassName("transparent_" + TransMessage.TYPE_ERROR);
            }
        })
    }
}
