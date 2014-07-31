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

/* This js file is to show helpful flash messages */
var FlashMessage = Class.create();

FlashMessage.prototype = {
    initialize: function() {
        this._initializeMessageBox('global-error');
        this._initializeMessageBox('global-warning');
        this._initializeMessageBox('error');
        this._initializeMessageBox('warn');
        this._initializeMessageBox('info');
        this._initializeMessageBox('success');
        this.showTimePeriod = 60;//secounds
    },

    _initializeMessageBox: function(type) {
        var message_box = $(type + '-box');
        var container = $(type + '-container');
        try {
            if (!message_box) {
                var main_container = $$('#yui-main .yui-b')[0];
                container = Builder.node('div', {id: type + '-container', style: 'display: none;'});
                var innerHTMLText = '<div class="ab-bg"><span class="ab-corner lvl1"></span><span class="ab-corner lvl2"></span><span class="ab-corner lvl3"></span><span class="ab-corner lvl4"></span></div><div id="'
                        + type + '-box"></div><div class="ab-bg"><span class="ab-corner lvl4"></span><span class="ab-corner lvl3"></span><span class="ab-corner lvl2"></span><span class="ab-corner lvl1"></span></div>';
                container.innerHTML = innerHTMLText;
                this[type + '-container'] = container; 
                main_container.insertBefore(container, main_container.firstChild);
            }
        } catch(e) {
            //for some version of IE6
            this._initializeMessageBox.bind(this).delay(0.01, type);
        }
    },

    getBox: function(type) {
        return $(type + '-box');
    },

    getContainer: function(type) {
        return $(type + '-container');
    },

    showMessageWithDetail: function(message, detail) {
        var innerHTML = '<div class="collapsable-container collapsed"><div class="collapsable-header" onclick="'
                + this.collapsableOnClickFunctionText() + '">' + message
                + '</div><div class="collapsable-content">'
                + detail + '</div></div>';
        return innerHTML;
    },

    collapsableOnClickFunctionText: function() {
        return "$(this).ancestors().first().toggleClassName('collapsed')";
    },

    _createListItem: function(message) {
        return "<li class='error-item'>" + message + "</li>";
    },

    _error: function(message, detail, keepVisible) {
        try {
            if (!detail) {
                this.getBox('error').innerHTML = this._createListItem(message);
            } else {
                this.getBox('error').innerHTML = this._createListItem(this.showMessageWithDetail(message, detail));
            }
            this.showPane(this.getContainer('error'), keepVisible);
//            this.getContainer('error').scrollTo();
//                              ^^
// after docking the header, page was scrolling to error and so that error was going under header :(
//            commented as temporary solution
        } catch(e) {
            this._error.bind(this).delay(1, message, detail, keepVisible)
            /* for some special version of IE */
        }
    },

    error: function(message, detail, keepVisible) {
        this._error.bind(this).delay(0.1, message, detail, keepVisible);
    },

    _global_errors: function(logs, keepVisible) {
        var itemsToShow = "";
        var flashobj = this;
        try {
            $A(logs).each(function(log) {
                if (!log.detail) {
                    itemsToShow += flashobj._createListItem(log.message);
                } else {
                    itemsToShow += flashobj._createListItem(flashobj.showMessageWithDetail(log.message, log.detail));
                }
            });
            this.getBox('global-error').innerHTML = itemsToShow;
            this.showPane(this.getContainer('global-error'), keepVisible);
        } catch(e) {
            this._global_errors.bind(this).delay(1, logs, keepVisible)
            /* for some special version of IE */
        }
    },
     _global_warnings: function(logs, keepVisible) {
        var itemsToShow = "";
        var flashobj = this;
        try {
            $A(logs).each(function(log) {
                if (!log.detail) {
                    itemsToShow += flashobj._createListItem(log.message);
                } else {
                    itemsToShow += flashobj._createListItem(flashobj.showMessageWithDetail(log.message, log.detail));
                }
            });
            this.getBox('global-warning').innerHTML = itemsToShow;
            this.showPane(this.getContainer('global-warning'), keepVisible);
        } catch(e) {
            this._global_warnings.bind(this).delay(1, logs, keepVisible)
            /* for some special version of IE */
        }
    },

    global_errors: function(logs, keepVisible) {
        this._global_errors.bind(this).delay(0.1, logs, keepVisible);
    },

    global_warnings: function(logs, keepVisible) {
        this._global_warnings.bind(this).delay(0.1, logs, keepVisible);
    },

    _warn: function(message, detail, keepVisible) {
        try {
            if (!detail) {
                this.getBox('warn').innerHTML = message;
            } else {
                this.getBox('warn').innerHTML = this.showMessageWithDetail(message, detail);
            }
            this.showPane(this.getContainer('warn'), keepVisible);
        } catch(e) {
            this._warn.bind(this).delay(1, message, detail, keepVisible)
            /* for some special version of IE */
        }
    },
    warn: function(message, detail, keepVisible) {
        this._warn.bind(this).delay(0.1, message, detail, keepVisible);
    },
    _info: function(message, keepVisible) {
        try {
            this.getBox('info').innerHTML = message;
            this.showPane(this.getContainer('info'), keepVisible);
        } catch(e) {
            this._info.bind(this).delay(1, message, keepVisible)
            /* for some special version of IE */
        }
    },
    info: function(message, keepVisible) {
        this._info.bind(this).delay(0.1, message, keepVisible);
    },
    _success: function(message, keepVisible) {
        try {
            this.getBox('success').innerHTML = message;
            this.showPane(this.getContainer('success'), keepVisible);
        } catch(e) {
            this._success.bind(this).delay(1, message, keepVisible)
            /* for some special version of IE */
        }
    },
    success: function(message, keepVisible) {
        this._success.bind(this).delay(0.1, message, keepVisible);
    },
    clear: function(message) {
        this.getContainer('info').hide();
        this.getContainer('error').hide();
    },

    showPane: function(container, keepVisible) {
        if (!keepVisible) {
            this.observeShowTime(container);
        }
        container.show();
    },

    observeShowTime: function(container) {
        var flash = this;
        flash._lastShowTime = (new Date()).getTime();
        flash.hidePaneWhenTimeout.bind(this, container).delay(flash.showTimePeriod);
    },
    hidePaneWhenTimeout: function(container) {
        var now = (new Date()).getTime();
        if (this._lastShowTime && (now - this._lastShowTime) >= this.showTimePeriod) {
            container.hide();
        } else {
            var flash = this;
            flash.hidePaneWhenTimeout.bind(this, container).delay(flash.showTimePeriod);
        }
    }
};

var FlashMessageLauncher = {
    init: function() {
        if (!window.flash) {
            window.flash = new FlashMessage();
        }
    },
    FlashTypes : $A(['global-error', 'global-warning', 'error', 'warn', 'info', 'success']),
    error: function(message, detail, keepVisible) {
        FlashMessageLauncher.init();
        FlashMessageLauncher.hide('success');
        window.flash.error(message, detail, keepVisible);
    },
    errors: function(logs, keepVisible) {
        FlashMessageLauncher.init();
        window.flash.errors(logs, keepVisible);
    },
    global_errors: function(logs, keepVisible) {
        FlashMessageLauncher.init();
        window.flash.global_errors(logs, keepVisible);
    },
    global_warnings: function(logs, keepVisible) {
        FlashMessageLauncher.init();
        window.flash.global_warnings(logs, keepVisible);
    },
    warn: function(message, detail, keepVisible) {
        FlashMessageLauncher.init();
        window.flash.warn(message, detail, keepVisible);
    },
    info: function(message, keepVisible) {
        FlashMessageLauncher.init();
        window.flash.info(message, keepVisible);
    },
    success: function(message, keepVisible) {
        FlashMessageLauncher.init();
        FlashMessageLauncher.hide('error');
        window.flash.success(message, keepVisible);
    },
    hide: function(type) {
        if (window.flash && window.flash[type + '-container']) {
            try {
                window.flash[type + '-container'].hide();
            } catch(e) {
            }
        }
    },
    hideAll: function() {
        FlashMessageLauncher.FlashTypes.each(function(type) {
            FlashMessageLauncher.hide(type);
        });
    }
};