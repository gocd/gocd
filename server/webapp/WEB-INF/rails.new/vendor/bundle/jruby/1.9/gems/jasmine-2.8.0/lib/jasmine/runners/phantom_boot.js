function PhantomReporter() {
  this.jasmineDone = function(details) {
    window.callPhantom({ state: 'jasmineDone', details: details });
  };

  this.specDone = function(results) {
    window.callPhantom({ state: 'specDone', results: results });
  };

  this.suiteDone = function(results) {
    window.callPhantom({ state: 'suiteDone', results: results });
  };
}

jasmine.getEnv().addReporter(new PhantomReporter());
