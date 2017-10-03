/**
 Jasmine Reporter that outputs test results to the browser console.
 Useful for running in a headless environment such as PhantomJs, ZombieJs etc.

 Usage:
 // From your html file that loads jasmine:
 jasmine.getEnv().execute();
*/

(function() {
  var ConsoleReporter,
      root = this;

  if (!jasmine) {
    throw "jasmine library isn't loaded!";
  }

  var ANSI = {};
  ANSI.color_map = {
      "green" : 32,
      "red"   : 31
  };

  ANSI.colorize_text = function(text, color) {
    var color_code = this.color_map[color];
    return "\033[" + color_code + "m" + text + "\033[0m";
  };

  ConsoleReporter = function() {
    if (!console || !console.log) { throw "console isn't present!"; }
    this.status = this.statuses.stopped;
  };

  var proto = ConsoleReporter.prototype;
  proto.statuses = {
    stopped : "stopped",
    running : "running",
    fail    : "fail",
    success : "success",
    skipped_or_pending : "skipped or pending"
  };

  proto.reportRunnerStarting = proto.jasmineStarted = function(runner) {
    this.status = this.statuses.running;
    this.start_time = (new Date()).getTime();
    this.executed_specs = 0;
    this.passed_specs = 0;
    this.skipped_or_pending_specs = 0;
    this.log("Starting...");
  };

  proto.reportRunnerResults = proto.jasmineDone = function(runner) {
    var failed = this.executed_specs - this.passed_specs - this.skipped_or_pending_specs;
    var spec_str = this.executed_specs + (this.executed_specs === 1 ? " spec, " : " specs, ");
    var fail_str = failed + (failed === 1 ? " failure in " : " failures in ");
    var skipped_or_pending_str = this.skipped_or_pending_specs ? this.skipped_or_pending_specs + ' skipped or pending, ' : '';

    var color = (failed > 0)? "red" : "green";
    var dur = (new Date()).getTime() - this.start_time;

    this.log("");
    this.log("Finished");
    this.log("-----------------");
    this.log(spec_str + skipped_or_pending_str + fail_str + (dur/1000) + "s.", color);

    this.status = (failed > 0)? this.statuses.fail : this.statuses.success;

    /* Print something that signals that testing is over so that headless browsers
       like PhantomJs know when to terminate. */
    this.log("");
    this.log("ConsoleReporter finished");
  };

  proto.reportSpecStarting = proto.specStarted = function(spec) {
    this.executed_specs++;
  };

  proto.reportSpecResults = proto.specDone = function(spec) {
    if(spec.results) { //jasmine 1.x
      var specResult = spec.results()
      if(specResult.skipped) {
        this.skipped_or_pending_specs++;
        return;
      } else if(specResult.passed()) {
        this.passed_specs++;
        return;
      }
    } else { //jasmine 2.x
      if(spec.status === "passed") {
        this.passed_specs++;
        return;
      } else if(spec.status !== "failed") {
        this.skipped_or_pending_specs++;
        //Skipped or Pending
        return;
      }
    }

    var fullName, failedExpectations;
    if(spec.suite) { //jasmine 1.x
      fullName = spec.suite.description + " " + spec.description;
      failedExpectations = spec.results().getItems().map(function(expectation){
        console.log(JSON.stringify(expectation))
        return "  " + expectation.message +
               (expectation.trace && expectation.trace.stack ? "\n    " + expectation.trace.stack : "" );
      });
    } else { //jasmine 2.x
      fullName = spec.fullName;
      failedExpectations = spec.failedExpectations.map(function(expectation){
        if(expectation.message === "undefined: undefined") {
          return "  An unstructured exception was thrown (use `new Error(message)` for better output).";
        } else {
          return "  " + expectation.message + "\n" +
                 "    " + expectation.stack;
        }
      });
    }

    this.log(fullName, "red");
    this.log(failedExpectations.join("\n\n"), "red");
  };

  proto.log = function(str, color) {
    var text = (color != undefined)? ANSI.colorize_text(str, color) : str;
    console.log(text)
  };

  jasmine.ConsoleReporter = ConsoleReporter;
  jasmine.getEnv().addReporter(new jasmine.ConsoleReporter());
})();
