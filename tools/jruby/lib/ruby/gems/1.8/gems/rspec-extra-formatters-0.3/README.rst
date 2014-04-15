================
RSpec Formatters
================

::

    $ rake format=tap
    ok 1 - TapFormatter should initialize the counter to 0
    ok 2 - TapFormatter example_passed should increment the counter and use the full_description attribute
    ok 3 - TapFormatter example_failed should increment the counter and use the full_description attribute
    ok 4 - TapFormatter example_pending should do the same as example_failed
    ok 5 - TapFormatter dump_summary should print the number of tests if there were tests
    ok 6 - TapFormatter dump_summary should print nothing if there were not tests
    ok 7 - JUnitFormatter should initialize the tests with failures and success
    ok 8 - JUnitFormatter example_passed should push the example obj into success list
    ok 9 - JUnitFormatter example_failed should push the example obj into failures list
    ok 10 - JUnitFormatter example_pending should do the same as example_failed
    ok 11 - JUnitFormatter read_failure should ignore if there is no exception
    ok 12 - JUnitFormatter read_failure should read message and backtrace from the example
    ok 13 - JUnitFormatter dump_summary should print the junit xml
    1..13

::

    $ rake format=junit
    <?xml version="1.0" encoding="utf-8" ?>
    <testsuite errors="0" failures="0" tests="13" time="0.019992" timestamp="2011-01-21T23:07:41-02:00">
      <properties />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/tap_formatter_spec.rb" name="TapFormatter should initialize the counter to 0" time="0.001298" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/tap_formatter_spec.rb" name="TapFormatter example_passed should increment the counter and use the full_description attribute" time="0.001546" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/tap_formatter_spec.rb" name="TapFormatter example_failed should increment the counter and use the full_description attribute" time="0.001427" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/tap_formatter_spec.rb" name="TapFormatter example_pending should do the same as example_failed" time="0.001456" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/tap_formatter_spec.rb" name="TapFormatter dump_summary should print the number of tests if there were tests" time="0.00177" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/tap_formatter_spec.rb" name="TapFormatter dump_summary should print nothing if there were not tests" time="0.000398" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter should initialize the tests with failures and success" time="0.000859" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter example_passed should push the example obj into success list" time="0.000829" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter example_failed should push the example obj into failures list" time="0.000778" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter example_pending should do the same as example_failed" time="0.000758" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter read_failure should ignore if there is no exception" time="0.00119" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter read_failure should read message and backtrace from the example" time="0.001823" />
      <testcase classname="/home/dsouza/dev/github/rspec_formatters/spec/junit_formatter_spec.rb" name="JUnitFormatter dump_summary should print the junit xml" time="0.003813" />
    </testsuite>

Using it
========

Make sure you add `-r "rspec-extra-formatters"` to rspec options and both `-f JUnitFormatter` and `-f TapFormatter` should work. :-)

