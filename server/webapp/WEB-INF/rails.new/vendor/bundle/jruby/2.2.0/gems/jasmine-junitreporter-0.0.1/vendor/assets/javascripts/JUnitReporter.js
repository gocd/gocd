jasmine.JUnitReporter = function JUnitReporter(options) {
  var options = options || {},
      outputDir = options.outputDir || '/tmp',
      traceEnabled = options.traceEnabled || false,
      currentSuites = [],
      currentSpec;

  this.jasmineStarted = function(things) {
    trace("JASMINE STARTED");
  };

  this.jasmineDone = function(things) {
    trace("JASMINE DONE");
    console.log("JUnitReporter finished (files written to " + outputDir + ")");
  };

  this.suiteStarted = function(result) {
    trace("SUITE STARTED");

    currentSuites.push({
      id: result.id,
      name: result.fullName,
      tests: 0,
      startTime: t(),
      time: 0.0,
      failed: 0,
      errored: 0,
      skipped: 0,
      specs: []
    });
  }

  this.suiteDone = function(result) {
    var suite = currentSuites.pop();

    trace("SUITE DONE");

    suite.endTime = t();
    suite.time = suite.endTime - suite.startTime;

    saveXML(result.id, suite.name, suiteToXML(suite));
  };

  this.specStarted = function(result) {
    trace("SPEC STARTED");

    currentSpec = {
      name: result.fullName,
      time: 0.0,
      startTime: t(),
      failures: []
    }
  };

  this.specDone = function(result) {
    var suite = currentSuite();
    var spec = currentSpec;
    currentSpec = undefined;

    trace("SPEC DONE");

    spec.endTime = t();
    spec.time = spec.endTime - spec.startTime;

    suite.tests++;
    if (result.status === 'pending') {
      suite.skipped++;
    } else if (result.status === 'failed') {
      suite.failed++;
    }

    result.failedExpectations.forEach(function(expectation) {
      spec.failures.push({
        message: expectation.message,
        stack: expectation.stack
      });
    });

    suite.specs.push(spec);
  };

  return this;

  function currentSuite() {
    return currentSuites[currentSuites.length - 1];
  }

  function specToXML(spec) {
    var xml = document.createElement('testcase');

    xml.setAttribute("name", spec.name);
    xml.setAttribute("time", spec.time / 1000);

    spec.failures.forEach(function(failure) {
      var failXml = document.createElement('failure');

      failXml.setAttribute("message", failure.message);
      failXml.textContent = failure.stack;

      xml.appendChild(failXml);
    });

    return xml;
  }

  function suiteToXML(suite) {
    var xml = document.createElement('testsuite');

    xml.setAttribute("name", suite.name);
    xml.setAttribute("tests", suite.tests);
    xml.setAttribute("time", suite.time / 1000);
    xml.setAttribute("failures", suite.failed);
    xml.setAttribute("errors", suite.errored);
    xml.setAttribute("skipped", suite.skipped);

    suite.specs.forEach(function(spec) {
      xml.appendChild(specToXML(spec));
    });

    return xml;
  }

  function filename(suiteId, suiteName) {
    return "SPEC-" + suiteId + "-" + suiteName.replace(/\W/g, '_') + ".xml";
  }

  function saveXML(suiteId, suiteName, node) {
    var outputFilename = outputDir + "/" + filename(suiteId, suiteName);

    trace("Saving XML:", node.outerHTML);

    window.callPhantom({event: 'writeFile',
                        filename: outputFilename,
                        text: node.outerHTML});
  }

  function trace() {
    if (traceEnabled) {
      console.log.apply(console, arguments);
    }
  }

  // Current milliseconds since the epoch
  function t() {
    return new Date().getTime();
  }
}
