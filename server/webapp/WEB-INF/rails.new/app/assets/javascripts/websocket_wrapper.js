/*
 * Copyright 2017 ThoughtWorks, Inc.
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

(function () {
  "use strict";

  var WebSocketWrapper = function (options) {
    var self = this;
    Emitter(this);

    var indefiniteRetry              = options.indefiniteRetry || false;
    var failIfInitialConnectionFails = options.failIfInitialConnectionFails || true;
    var timeoutInterval              = options.timeoutInterval || WebSocketWrapper.TIMEOUT_DEFAULT;
    var timeoutStart                 = options.timeoutStart || WebSocketWrapper.TIMEOUT_START;

    var everConnectedUsingWebsocket = false;
    var ws;
    var lastPingTime;
    var initTimer;
    var stopped;
    var pingDetectionTimeoutTimer;

    if (indefiniteRetry) {
      pingDetectionTimeoutTimer = often(function () {
        var diff = new Date() - lastPingTime;
        if (diff > timeoutInterval) {
          self.close(WebSocketWrapper.CLOSE_WILL_RETRY, "Will retry in some time!");
          retryIfNeeded.call(self);
        }
      }).start(timeoutStart).wait(timeoutInterval);
    }

    var init = function () {
      self.emit('beforeInitialize', options);

      ws           = new WebSocket(options.url, options.protocols);
      lastPingTime = new Date();
      stopped      = false;

      ws.addEventListener('open', function (e) {
        everConnectedUsingWebsocket = true;
        self.emit('open', e);
      });

      ws.addEventListener('message', function (e) {
        if (isPingFrame(e.data)) {
          lastPingTime = new Date();
          // don't bubble the ping event, since it's only meant for the websocket
          return;
        }
        self.emit('message', e);
      });

      ws.addEventListener('error', function (e) {
        if (indefiniteRetry) {
          pingDetectionTimeoutTimer.done();
        }
        if (!everConnectedUsingWebsocket) {
          self.emit('initialConnectFailed', e)
        } else {
          self.emit('error', e);
        }
        retryIfNeeded();
      });

      ws.addEventListener('close', function (e) {
        if (indefiniteRetry) {
          pingDetectionTimeoutTimer.done();
        }

        if (e.code === WebSocketWrapper.CLOSE_ABNORMAL) {
          return;
        }
        self.emit('close', e);
      });
    };

    var retryIfNeeded = function () {
      if (!indefiniteRetry) {
        return;
      }

      if (failIfInitialConnectionFails && !everConnectedUsingWebsocket) {
        return;
      }

      if (stopped) {
        return;
      }

      if (initTimer) {
        window.clearTimeout(initTimer);
      }

      initTimer = setTimeout(init, 5000);
    };

    var isPingFrame = function (data) {
      if (_.isString(data)) {
        try {
          return JSON.parse(e.data)['type'] === 'ping'
        } catch (e) {
          // ignore, maybe it's not json
        }
      }
      return false;
    };

    this.close = function (code, reason) {
      ws.close(code, reason)
    };

    this.stop = function (code, reason) {
      stopped = true;
      pingDetectionTimeoutTimer.done();
      this.close(code, reason);
    };

    init();
  };

  // the timeout on the server is 10s,
  // we add another 5 seconds as a buffer, to account for network latency
  WebSocketWrapper.TIMEOUT_DEFAULT  = 15000;
  WebSocketWrapper.TIMEOUT_START    = 5000;
  WebSocketWrapper.CLOSE_NORMAL     = 1000;
  WebSocketWrapper.CLOSE_ABNORMAL   = 1006;
  WebSocketWrapper.CLOSE_WILL_RETRY = 4100;

  window.WebSocketWrapper = WebSocketWrapper;
})();