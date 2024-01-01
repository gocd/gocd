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
function AjaxRefresher(url, options) {
  options = options || {};
  const hasAfterCallback = !!options.afterRefresh;
  const hasManipulateReplacement = !!options.manipulateReplacement;

  const tempDom = $(document.createElement('div'));
  let in_progress = false;
  let stopped = false;
  let ajaxRequest;
  let eTag;
  let periodicExecutor;

  const transient_after_refresh_callbacks = {};
  const permanent_after_refresh_callbacks = {};

  function getValue(map, key, attribute, mandatory) {
    const value = map[attribute];
    if (mandatory && value === undefined) {
      throw "no '" + attribute + "' given for dom id '" + key + "'";
    }
    return value;
  }

  function receiver(key) {
    return $(document.getElementById(key));
  }

  function _updateWithTempDom(key) {
    const recv = receiver(key);
    recv.empty();
    recv.append(tempDom.contents());
  }

  function _onSuccess(json) {
    if (stopped) {
      return;
    }

    if (isResponseNotModified()) {
      return;
    }

    for (const key in json) {
      const value = json[key];
      const html_content = getValue(value, key, 'html', true);
      if (hasManipulateReplacement) {
        tempDom.html(html_content);
        options.manipulateReplacement(key, tempDom.get(0));
        _updateWithTempDom(key);
      } else {
        receiver(key).html(html_content);
      }
      hasAfterCallback && options.afterRefresh(key);
      transient_after_refresh_callbacks_for(key).forEach(function(callback) {
        callback(key);
      });

      permanent_after_refresh_callbacks_for(key).forEach(function(callback) {
        callback(key);
      });
      reset_transient_after_refresh_callbacks_for(key);
    }
  }

  function _onComplete() {
    in_progress = false;
  }

  function _onError(xhr) {
    if (xhr.status === 401) {
      _redirectToLoginPage();
    }
  }

  function _redirectToLoginPage() {
    window.location = window.location.protocol + '//' + window.location.host + "/go/auth/login";
  }

  const _request = function () {
    if (in_progress) return;
    in_progress = true;
    ajaxRequest = $.ajax({
      data: (options.dataFetcher ? options.dataFetcher() : {}),
      url: url,
      context: document.body,
      dataType: 'json',
      success: _onSuccess.bind(this),
      complete: _onComplete.bind(this),
      error: _onError.bind(this)
    });
  };

  const _startExecution = function () {
    _hookupAutoRefresh();
  };

  function _hookupAutoRefresh() {
    periodicExecutor = new PeriodicExecutor(_request, options.time || 10);
  }

  options.updateOnce ? _request() : _startExecution();

  this.stopRefresh = function() {
    if (periodicExecutor) {
      periodicExecutor.stop();
    }
    stopped = true;
  };

  this.restartRefresh = function() {
    stopped = false;
    if (periodicExecutor) {
      periodicExecutor.registerCallback();
      periodicExecutor.execute();
    }
  };

  function isResponseNotModified() {
    if (ajaxRequest) {
      const responseETag = ajaxRequest.getResponseHeader("ETag");
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


class PeriodicExecutor {
  constructor(callback, frequencySeconds) {
    this.callback = callback;
    this.frequencySeconds = frequencySeconds;
    this.currentlyExecuting = false;

    this.registerCallback();
  }

  registerCallback() {
    this.timer = setInterval(this.onTimerEvent.bind(this), this.frequencySeconds * 1000);
  }

  execute() {
    this.callback(this);
  }

  stop() {
    if (!this.timer) return;
    clearInterval(this.timer);
    this.timer = null;
  }

  onTimerEvent() {
    if (!this.currentlyExecuting) {
      try {
        this.currentlyExecuting = true;
        this.execute();
      } finally {
        this.currentlyExecuting = false;
      }
    }
  }
}
