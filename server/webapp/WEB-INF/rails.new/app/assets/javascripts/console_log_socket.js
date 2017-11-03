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

(function ($) {
  "use strict";

  function ConsoleLogSocket(fallbackObserver, transformer, options) {
    var CONSOLE_LOG_DOES_NOT_EXISTS = 4410;
    var startLine = 0, socket, genricSocket ;

    var details              = $(".job_details_content");
    var fallingBackToPolling = false;

    if (!details.length) return;

    function genricEndpointUrl() {
      var l = document.location;
      var protocol = l.protocol.replace("http", "ws"), host = l.host, path = "browser-websocket";

      return protocol + "//" + host + context_path(path);
    }

    genricSocket = new WebSocketWrapper({
      url:                          genricEndpointUrl(),
      indefiniteRetry:              true,
      failIfInitialConnectionFails: true
    });


    genricSocket.on("open", function () {
      //on connection open, subscribe for console-log event
      var msg = {
        action: "subscribe",
        events: [
          {
            'type': 'JobStatusChange',
            'job_identifier': {
              'pipeline_name': details.data('pipeline'),
              'pipeline_label': details.data('pipeline-label'),
              'stage_name': details.data('stage'),
              'stage_counter': details.data('stage-counter'),
              'build_name': details.data('build')
            },
            'start_line': startLine
          }
        ]
      };

      debugger;
      // we call `toJSON` offered by prototype.js.
      // Calling `JSON.stringify()` fails because of monkey patched `toJSON` on all JS prototypes applied by prototype.js.
      genricSocket.send(Object.toJSON(msg));
    });


    function endpointUrl(startLine) {
      var l        = document.location;
      var protocol = l.protocol.replace("http", "ws"), host = l.host, path = [
        "console-websocket",
        details.data("pipeline"),
        details.data("pipeline-label"),
        details.data("stage"),
        details.data("stage-counter"),
        details.data("build")
      ].join("/");

      return protocol + "//" + host + context_path(path) + "?startLine=" + startLine;
    }

    socket = new WebSocketWrapper({
      url:                          endpointUrl(startLine),
      indefiniteRetry:              true,
      failIfInitialConnectionFails: true
    });

    socket.on("message", renderLines);
    socket.on("initialConnectFailed", retryConnectionOrFallbackToPollingOnError);
    socket.on("close", maybeResumeOnClose);
    socket.on("beforeInitialize", function (options) {
      options.url = endpointUrl(startLine);
    });

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
        if (options && "function" === typeof options.onComplete) {
          transformer.invoke(options.onComplete);
        }
      }

      if (e.code === WebSocketWrapper.CLOSE_NORMAL) {
        if (options && "function" === typeof options.onComplete) {
          transformer.invoke(options.onComplete);
        }
      }
    }

    function maybeGunzip(gzippedBuf) {
      var inflator = new pako.Inflate({to: 'string'});
      inflator.push(gzippedBuf, true);

      if (inflator.err) {
        return String.fromCharCode.apply(null, gzippedBuf);
      } else {
        return inflator.result;
      }
    }

    function renderLines(e) {
      debugger;
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

  }

  window.ConsoleLogSocket = ConsoleLogSocket;
})(jQuery);
