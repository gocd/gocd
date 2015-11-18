# -*- encoding: utf-8 -*-
# stub: test-unit 3.0.3 ruby lib

Gem::Specification.new do |s|
  s.name = "test-unit"
  s.version = "3.0.3"

  s.required_rubygems_version = Gem::Requirement.new(">= 0") if s.respond_to? :required_rubygems_version=
  s.require_paths = ["lib"]
  s.authors = ["Kouhei Sutou", "Haruka Yoshihara"]
  s.date = "2014-10-28"
  s.description = "Test::Unit (test-unit) is unit testing framework for Ruby, based on xUnit\nprinciples. These were originally designed by Kent Beck, creator of extreme\nprogramming software development methodology, for Smalltalk's SUnit. It allows\nwriting tests, checking results and automated testing in Ruby.\n"
  s.email = ["kou@cozmixng.org", "yoshihara@clear-code.com"]
  s.files = ["COPYING", "GPL", "LGPL", "PSFL", "README.md", "Rakefile", "TODO", "doc/text/how-to.md", "doc/text/news.md", "lib/test-unit.rb", "lib/test/unit.rb", "lib/test/unit/assertion-failed-error.rb", "lib/test/unit/assertions.rb", "lib/test/unit/attribute-matcher.rb", "lib/test/unit/attribute.rb", "lib/test/unit/autorunner.rb", "lib/test/unit/code-snippet-fetcher.rb", "lib/test/unit/collector.rb", "lib/test/unit/collector/descendant.rb", "lib/test/unit/collector/dir.rb", "lib/test/unit/collector/load.rb", "lib/test/unit/collector/objectspace.rb", "lib/test/unit/collector/xml.rb", "lib/test/unit/color-scheme.rb", "lib/test/unit/color.rb", "lib/test/unit/data.rb", "lib/test/unit/diff.rb", "lib/test/unit/error.rb", "lib/test/unit/exception-handler.rb", "lib/test/unit/failure.rb", "lib/test/unit/fault-location-detector.rb", "lib/test/unit/fixture.rb", "lib/test/unit/notification.rb", "lib/test/unit/omission.rb", "lib/test/unit/pending.rb", "lib/test/unit/priority.rb", "lib/test/unit/runner/console.rb", "lib/test/unit/runner/emacs.rb", "lib/test/unit/runner/xml.rb", "lib/test/unit/test-suite-creator.rb", "lib/test/unit/testcase.rb", "lib/test/unit/testresult.rb", "lib/test/unit/testsuite.rb", "lib/test/unit/ui/console/outputlevel.rb", "lib/test/unit/ui/console/testrunner.rb", "lib/test/unit/ui/emacs/testrunner.rb", "lib/test/unit/ui/testrunner.rb", "lib/test/unit/ui/testrunnermediator.rb", "lib/test/unit/ui/testrunnerutilities.rb", "lib/test/unit/ui/xml/testrunner.rb", "lib/test/unit/util/backtracefilter.rb", "lib/test/unit/util/method-owner-finder.rb", "lib/test/unit/util/observable.rb", "lib/test/unit/util/output.rb", "lib/test/unit/util/procwrapper.rb", "lib/test/unit/version.rb", "sample/adder.rb", "sample/subtracter.rb", "sample/test_adder.rb", "sample/test_subtracter.rb", "sample/test_user.rb", "test/collector/test-descendant.rb", "test/collector/test-load.rb", "test/collector/test_dir.rb", "test/collector/test_objectspace.rb", "test/fixtures/header-label.csv", "test/fixtures/header-label.tsv", "test/fixtures/header.csv", "test/fixtures/header.tsv", "test/fixtures/no-header.csv", "test/fixtures/no-header.tsv", "test/fixtures/plus.csv", "test/run-test.rb", "test/test-assertions.rb", "test/test-attribute-matcher.rb", "test/test-attribute.rb", "test/test-code-snippet.rb", "test/test-color-scheme.rb", "test/test-color.rb", "test/test-data.rb", "test/test-diff.rb", "test/test-emacs-runner.rb", "test/test-error.rb", "test/test-failure.rb", "test/test-fault-location-detector.rb", "test/test-fixture.rb", "test/test-notification.rb", "test/test-omission.rb", "test/test-pending.rb", "test/test-priority.rb", "test/test-test-case.rb", "test/test-test-result.rb", "test/test-test-suite-creator.rb", "test/test-test-suite.rb", "test/testunit-test-util.rb", "test/ui/test_testrunmediator.rb", "test/util/test-method-owner-finder.rb", "test/util/test-output.rb", "test/util/test_backtracefilter.rb", "test/util/test_observable.rb", "test/util/test_procwrapper.rb"]
  s.homepage = "http://rubygems.org/gems/test-unit"
  s.licenses = ["Ruby", "PSFL"]
  s.rubygems_version = "2.4.6"
  s.summary = "An xUnit family unit testing framework for Ruby."
  s.test_files = ["test/test-assertions.rb", "test/test-color.rb", "test/test-code-snippet.rb", "test/test-test-suite-creator.rb", "test/test-test-result.rb", "test/test-error.rb", "test/test-failure.rb", "test/run-test.rb", "test/test-pending.rb", "test/test-color-scheme.rb", "test/test-attribute-matcher.rb", "test/testunit-test-util.rb", "test/test-data.rb", "test/ui/test_testrunmediator.rb", "test/util/test-method-owner-finder.rb", "test/util/test-output.rb", "test/util/test_observable.rb", "test/util/test_backtracefilter.rb", "test/util/test_procwrapper.rb", "test/test-omission.rb", "test/test-test-case.rb", "test/test-fixture.rb", "test/fixtures/no-header.csv", "test/fixtures/header-label.tsv", "test/fixtures/plus.csv", "test/fixtures/no-header.tsv", "test/fixtures/header-label.csv", "test/fixtures/header.csv", "test/fixtures/header.tsv", "test/collector/test-descendant.rb", "test/collector/test_objectspace.rb", "test/collector/test-load.rb", "test/collector/test_dir.rb", "test/test-priority.rb", "test/test-test-suite.rb", "test/test-diff.rb", "test/test-emacs-runner.rb", "test/test-attribute.rb", "test/test-fault-location-detector.rb", "test/test-notification.rb"]

  if s.respond_to? :specification_version then
    s.specification_version = 4

    if Gem::Version.new(Gem::VERSION) >= Gem::Version.new('1.2.0') then
      s.add_runtime_dependency(%q<power_assert>, [">= 0"])
      s.add_development_dependency(%q<bundler>, [">= 0"])
      s.add_development_dependency(%q<rake>, [">= 0"])
      s.add_development_dependency(%q<yard>, [">= 0"])
      s.add_development_dependency(%q<kramdown>, [">= 0"])
      s.add_development_dependency(%q<packnga>, [">= 0"])
    else
      s.add_dependency(%q<power_assert>, [">= 0"])
      s.add_dependency(%q<bundler>, [">= 0"])
      s.add_dependency(%q<rake>, [">= 0"])
      s.add_dependency(%q<yard>, [">= 0"])
      s.add_dependency(%q<kramdown>, [">= 0"])
      s.add_dependency(%q<packnga>, [">= 0"])
    end
  else
    s.add_dependency(%q<power_assert>, [">= 0"])
    s.add_dependency(%q<bundler>, [">= 0"])
    s.add_dependency(%q<rake>, [">= 0"])
    s.add_dependency(%q<yard>, [">= 0"])
    s.add_dependency(%q<kramdown>, [">= 0"])
    s.add_dependency(%q<packnga>, [">= 0"])
  end
end
