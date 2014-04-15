// ==UserScript==
// @name           Better FBH
// @namespace      Go
// @description    FBH failed tests aggregator
// @include        */go/pipelines/*/tests
// ==/UserScript==

FailedBuildHistory = function() {
  function add_data_pane(self, holder) {
    self.body = document.createElement('div');
    var line = document.createElement('h3');
    line.innerHTML = "Total failing tests: " + self.total_tests;
    holder.insertBefore(self.body, holder.childNodes[0]);
    holder.insertBefore(line, self.body);
  }

  function load_test_names(self) {
    self.tests = {};
    self.total_tests = 0;
    var tests = document.getElementsByClassName('test_name');
    for(var i = 0; i < tests.length; i++) {
      var test = tests[i];
      var name = test.getElementsByClassName('name')[0].innerHTML;
      var job_link = test.getElementsByTagName('a')[0];
      if (! self.tests[name]) {
        self.tests[name] = job_link.href;
        self.total_tests++;
      }
    }
  }

  function render(self) {
    var html = "<ul class='list_aggregation' style='width: 400px; margin: 0 0 30px 0;'>";
    for(var test in self.tests) {
      html += "<li style='float:none;'><a href='" + self.tests[test] + "'>" + test + "</a></li>";
    }
    html += "</ul>";
    self.body.innerHTML = html;
  }

  function init(holder) {
    load_test_names(this);
    add_data_pane(this, holder);
    render(this);
  };

  return init;
}();

try {
  var holder = document.getElementsByClassName('rail')[0];
  new FailedBuildHistory(holder);
} catch (e) {
  alert("FBH interpreter failed: " + e);
}
