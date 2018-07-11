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

ContextualAjaxPopupHandler = function() {
    function onSuccess(popupHandler) {
        return function(html) {
            popupHandler.responseDom.html(html);
        };
    }

    function onError(popupHandler) {
        return function() {
            popupHandler.responseDom.html("No data available");
        };
    }

    function init(responseDom, event_url_getter) {
        this.responseDom = jQuery(responseDom);
        this.event_url_getter = event_url_getter;
    }

    init.prototype = new MicroContentPopup.NeverCloseHandler();

    init.prototype.ajax_load = function(url) {
        this.responseDom.empty();
        jQuery.ajax({
            url: url,
            dataType: 'html',
            success: onSuccess(this),
            error: onError(this)
        });
    };

    init.prototype.before_show = function(event) {
        this.ajax_load(this.event_url_getter(event));
    };

    return init;
}();

ContextualPropagatingAjaxPopupHandler =  function() {
    function onSuccess(popupHandler) {
        return function(html) {
            popupHandler.responseDom.html(html);
        };
    }

    function onError(popupHandler) {
        return function() {
            popupHandler.responseDom.html("No data available");
        };
    }

    function init(responseDom, event_url_getter) {
        this.responseDom = jQuery(responseDom);
        this.event_url_getter = event_url_getter;
    }

    init.prototype = new MicroContentPopup.PropagateAllHandler();

    init.prototype.ajax_load = function(url) {
        this.responseDom.empty();
        jQuery.ajax({
            url: url,
            dataType: 'html',
            success: onSuccess(this),
            error: onError(this)
        });
    };

    init.prototype.before_show = function(event) {
        this.ajax_load(this.event_url_getter(event));
    };

    return init;
}();
AjaxPopupHandler = function() {
    function self_url(self) {
        return function(_) {
            return self.url;
        }
    }

    function init(url, responseDom) {
        ContextualAjaxPopupHandler.call(this, responseDom, self_url(this));
        this.url = url;
    }

    init.prototype = new ContextualAjaxPopupHandler();

    return init;
}();

