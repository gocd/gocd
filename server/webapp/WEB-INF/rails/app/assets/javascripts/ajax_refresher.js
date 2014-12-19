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

function AjaxRefresher(url, redirectUrl, options) {
    options = options || {};
    redirectUrl = redirectUrl || "/go/auth/login";
    var noBeforeCallback = ! options.beforeRefresh;
    var hasAfterCallback = !! options.afterRefresh;
    var hasRefreshBegining = !! options.refreshBegining;
    var hasRefreshCompletedCallback = !! options.refreshCompleted;
    var hasManipulateReplacement = !! options.manipulateReplacement;

    var tempDom = jQuery(document.createElement('div'));
    var in_progress = false;
    var stopped = false;
    var ajaxRequest;
    var eTag;
    var periodicalExecuter;

    var transient_after_refresh_callbacks = {};
    var permanent_after_refresh_callbacks = {};

    function getValue(map, key, attribute, mandatory) {
        var value = map[attribute];
        if (mandatory && value === undefined) {
            throw "no '" + attribute + "' given for dom id '" + key + "'";
        }
        return value;
    }

    function receiver(key) {
        return jQuery(document.getElementById(key));
    }

    function _updateWithTempDom(key) {
        var recv = receiver(key);
        recv.empty();
        recv.append(tempDom.contents());
    }

    function _onSuccess(json) {
        if (stopped) {
            return;
        }

        if (isResponseNotModified()) {
           var isPartialRefresh = true;
           hasRefreshBegining && options.refreshBegining(isPartialRefresh);
           hasRefreshCompletedCallback && options.refreshCompleted(isPartialRefresh);
           return;
        }

        hasRefreshBegining && options.refreshBegining();

        for (var key in json) {
            var value = json[key];
            var html_content = getValue(value, key, 'html', true);
            if (noBeforeCallback || options.beforeRefresh(key, value)) {
                if (hasManipulateReplacement) {
                    tempDom.html(html_content);
                    options.manipulateReplacement(key, tempDom.get(0), value);
                    _updateWithTempDom(key);
                } else {
                    receiver(key).html(html_content);
                }
                hasAfterCallback && options.afterRefresh(key);
                transient_after_refresh_callbacks_for(key).each(function(callback) {
                    callback(key);
                });

                permanent_after_refresh_callbacks_for(key).each(function(callback) {
                    callback(key);
                });
                reset_transient_after_refresh_callbacks_for(key);
            }
        }
        hasRefreshCompletedCallback && options.refreshCompleted(false);
    }

    function _onComplete(xhr) {
        in_progress = false;
        var forcePageReloadUrl = xhr.getResponseHeader("X-GO-FORCE-LOAD-PAGE");
        if (forcePageReloadUrl) {
            Util.loadPage(forcePageReloadUrl);
        }
    }

    function _onError(xhr) {
        if (xhr.status === 401) {
            _redirectToLoginPage();
        }
    }

    //TODO: Can't get this under unit test because of window.location being set. Need to figure out a way to do this.
    function _redirectToLoginPage() {
        redirectToLoginPage(redirectUrl);
    }

    var _request = function() {
        if (in_progress) return;
        in_progress = true;
        ajaxRequest = jQuery.ajax({
            data: (options.dataFetcher? options.dataFetcher() : {}),
            url: url,
            context: document.body,
            dataType: 'json',
            success: _onSuccess,
            complete: _onComplete,
            error: _onError
        });
    };

    var _startExecution = function() {
        _hookupAutoRefresh();
        if ($j.browser.msie) {
            stopped = true;
            periodicalExecuter.stop();
            setTimeout(function() {
                stopped = false;
                if (periodicalExecuter) {
                    periodicalExecuter.registerCallback();
                    periodicalExecuter.execute();
                }
            }, (30*1000));
        }
        if (options.executeImmediately) {
            _request();
        }
    };

    function _hookupAutoRefresh() {
        periodicalExecuter = new PeriodicalExecuter(_request, options.time || 10);
    }

    options.updateOnce ? _request() : _startExecution();

    this.stopRefresh = function() {
        if (periodicalExecuter) {
            periodicalExecuter.stop();
        }
        stopped = true;
    };

    this.restartRefresh = function() {
        stopped = false;
        if (periodicalExecuter) {
            periodicalExecuter.registerCallback();
            periodicalExecuter.execute();
        }
    };

    this._getStoppedForTest = function() {
        return stopped;
    };

    this._setStoppedForTest = function(b){
        stopped = b;
    };

    function isResponseNotModified() {
       if (ajaxRequest) {
           var responseETag = ajaxRequest.getResponseHeader("ETag");
           if (eTag == responseETag) {
                return true;
           } else {
             eTag = responseETag;
           }
       }
       return false;
    }

    function transient_after_refresh_callbacks_for(id) {
        return transient_after_refresh_callbacks[id] || reset_transient_after_refresh_callbacks_for(id);
    }

    function permanent_after_refresh_callbacks_for(id) {
        return permanent_after_refresh_callbacks[id] || initialize_permanent_after_refresh_callbacks_for(id);
    }

    function reset_transient_after_refresh_callbacks_for(id) {
        return (transient_after_refresh_callbacks[id] = []);
    }

    function initialize_permanent_after_refresh_callbacks_for(id) {
        return (permanent_after_refresh_callbacks[id] = []);
    }

    this.afterRefreshOf = function(id, callback, permanent) {
        (permanent ? permanent_after_refresh_callbacks_for(id) : transient_after_refresh_callbacks_for(id)).push(callback);
    };
}