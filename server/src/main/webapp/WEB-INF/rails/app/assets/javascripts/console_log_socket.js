/*
 * Copyright 2021 ThoughtWorks, Inc.
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
// polyfills needed for IE
//= require "lib/encoding-indexes.js"
//= require "lib/encoding.js"

(function ($) {
  "use strict";

  function ConsoleLogSocket(fallbackObserver, transformer, options) {
    var CONSOLE_LOG_DOES_NOT_EXISTS = 4410;
    var CONSOLE_LOG_NOT_AVAILABLE   = 4004;
    var startLine                   = 0, socket;
    var encoder;

    var details              = $(".job_details_content");
    var fallingBackToPolling = false;

    if (!details.length) return;

    function endpointUrl(startLine) {
      var l        = document.location;
      var protocol = l.protocol.replace("http", "ws"), host = l.host, path = [
        "console-websocket",
        details.data("pipeline"),
        details.data("pipeline-counter"),
        details.data("stage"),
        details.data("stage-counter"),
        details.data("build")
      ].join("/");

      return protocol + "//" + host + context_path(path) + "?startLine=" + startLine;
    }

    function start() {
      socket = new WebSocketWrapper({
        url:                          endpointUrl(startLine),
        indefiniteRetry:              true,
        failIfInitialConnectionFails: true
      });

      socket.on("message", grabEncoding);
      socket.on("message", renderLines);
      socket.on("initialConnectFailed", retryConnectionOrFallbackToPollingOnError);
      socket.on("close", maybeResumeOnClose);
      socket.on("beforeInitialize", function (options) {
        options.url = endpointUrl(startLine);
      });
    }

    function grabEncoding(e) {
      if (_.isString(e.data)) {
        var charset;

        try {
          charset = JSON.parse(e.data)['charset'];
        } catch (e) {
          // ignore, maybe it's not json
        }

        if (!_.isEmpty(charset)) {
          encoder = new TextDecoder(charset);
          socket.off('message', grabEncoding);
        }
      }
    }

    function retryConnectionOrFallbackToPollingOnError(e) {
      fallingBackToPolling = true; // prevent close handler from trying to reconnect
      fallbackObserver.enable();
      fallbackObserver.notify();
    }

    function maybeResumeOnClose(e) {
      if (fallingBackToPolling) {
        return;
      }

      if (e.code === CONSOLE_LOG_DOES_NOT_EXISTS) {
        transformer.transform([e.reason]);
        if (options && "function" === typeof options.onComplete) {
          transformer.invoke(options.onComplete);
        }
      }

      if (e.code === WebSocketWrapper.CLOSE_NORMAL) {
        if (options && "function" === typeof options.onComplete) {
          transformer.invoke(options.onComplete);
        }
      }

      if (e.code === CONSOLE_LOG_NOT_AVAILABLE) {
        start();
      }
    }

    function maybeGunzip(gzippedBuf) {
      encoder.toString();
      var inflator = new pako.Inflate();
      inflator.push(gzippedBuf, true);

      if (inflator.err) {
        return encoder.decode(gzippedBuf);
      } else {
        return encoder.decode(inflator.result);
      }
    }

    function renderLines(e) {
      var buildOutput = e.data, lines, slice = [];

      if (!buildOutput || !(buildOutput instanceof Blob)) {
        return;
      }

      var reader = new FileReader();

      reader.addEventListener("loadend", function () {
        var arrayBuffer   = reader.result;
        var gzippedBuf    = new Uint8Array(arrayBuffer);
        var consoleOutput = maybeGunzip(gzippedBuf);

        lines = consoleOutput.split(/\r?\n/);

        startLine += lines.length;

        while (lines.length) {
          slice = lines.splice(0, 1000);
          transformer.transform(slice);
        }

        if (options && "function" === typeof options.onUpdate) {
          transformer.invoke(options.onUpdate);
        }
      });
      reader.readAsArrayBuffer(buildOutput);
    }

    start();

  }

  window.ConsoleLogSocket = ConsoleLogSocket;
})(jQuery);
