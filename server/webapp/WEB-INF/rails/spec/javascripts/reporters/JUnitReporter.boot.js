(function() {
    var reporter = new jasmine.JUnitReporter({
        outputDir: 'tmp/jasmine/test-reports'
    });
    jasmine.getEnv().addReporter(reporter);
})();