var jsApiReporter;
(function() {
  var jasmineEnv = jasmine.getEnv();
  jsApiReporter = new jasmine.JsApiReporter();
  jasmineEnv.addReporter(jsApiReporter);

  var htmlReporter = new jasmine.HtmlReporter();
  jasmineEnv.addReporter(htmlReporter);
  jasmineEnv.specFilter = function(spec) {
    return htmlReporter.specFilter(spec);
  };

  function execJasmine() {
    jasmineEnv.execute();
  }

  if (window.addEventListener) { // W3C
    window.addEventListener('load', execJasmine, false);
  } else if (window.attachEvent) { // MSIE
    window.attachEvent('onload', execJasmine);
  }
})();
