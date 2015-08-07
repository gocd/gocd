(function() {
  // handler for any page javascript errors
  var errorHandler = function(msg, trace) {
    var msgStack = ['ERROR: ' + msg];
    if (trace) {
      msgStack.push('TRACE:');
      trace.forEach(function(t) {
        msgStack.push(' -> ' + t.file + ': ' + t.line + (t.function ? ' (in function "' + t.function + '")' : ''));
      });
    }
    console.error(msgStack.join('\n'));
    phantom.exit(1);
  };
  phantom.onError = errorHandler;

  var system = require('system');
  var args = system.args;

  if (args.length !== 2) {
    console.log("Need a url as the argument");
    phantom.exit(1);
  }

  var page = new WebPage();

  // log messages to stdout
  page.onConsoleMessage = function(msg) {
    console.log(msg);
  };

  // listen for event from parent page
  page.onCallback = function(data) {
    if (data.event === 'exit') {
      phantom.exit(data.exitCode);
    } else if (data.event === 'writeFile') {
      var fs = require("fs");
      fs.write(data.filename, data.text, 'w');
    } else {
      console.log('unknown event callback: ' + data);
    }
  };

  // log javascript errors
  page.onError = errorHandler;

  // setup listeners for jasmine events
  page.onInitialized = function() {
    return page.evaluate(function() {
      return window.onload = function() {
        var exitCode = 0,
            originalReportSpecResults = jsApiReporter.reportSpecResults,
            originalJasmineDone = jsApiReporter.reportRunnerResults || jsApiReporter.jasmineDone;

        //jasmine 1.x - determine failure by overwriting #reportSpecResults
        jsApiReporter.reportSpecResults = function(spec) {
          originalReportSpecResults.call(jsApiReporter, spec);
          if (spec.results().failedCount > 0) {
            exitCode = 1;
          }
        };

        jsApiReporter.reportRunnerResults = jsApiReporter.jasmineDone = function(runner) {
          originalJasmineDone.call(jsApiReporter, runner);

          //jasmine 2.x - loop over the specs stored on the reporter to find failures
          if(jsApiReporter.specs) {
            jsApiReporter.specs().forEach(function(spec) {
              if(spec.status === "failed") {
                exitCode = 1;
              }
            })
          }

          setTimeout(function() {
            window.callPhantom({
              event: 'exit',
              exitCode: exitCode
            });
          }, 1);
        };
      };
    });
  };

  var address = args[1];
  console.log('Running: ' + address);
  page.open(address, function(status) {
    if (status !== "success") {
      console.log("can't load the address!");
      return phantom.exit(1);
    }
  });
})();
