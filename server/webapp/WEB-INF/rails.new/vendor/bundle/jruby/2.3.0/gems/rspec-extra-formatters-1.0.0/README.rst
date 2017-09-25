================
RSpec Formatters
================

::

    $ rake format=tap | sed 1d
    TAP version 13
    1..15
    ok 1 - JUnitFormatter should initialize the tests with failures and success
    ok 2 - JUnitFormatter example_passed should push the example obj into success list
    ok 3 - JUnitFormatter example_failed should push the example obj into failures list
    ok 4 - JUnitFormatter example_pending should push the example obj into the skipped list
    ok 5 - JUnitFormatter read_failure should ignore if there is no exception
    ok 6 - JUnitFormatter read_failure should attempt to read exception if exception encountered is nil
    ok 7 - JUnitFormatter read_failure should read message and backtrace from the example
    ok 8 - JUnitFormatter dump_summary should print the junit xml
    ok 9 - JUnitFormatter dump_summary should escape characteres <,>,&," before building xml
    ok 10 - TapFormatter should initialize the counter to 0
    ok 11 - TapFormatter example_passed should increment the counter and use the full_description attribute
    ok 12 - TapFormatter example_failed should increment the counter and use the full_description attribute
    ok 13 - TapFormatter example_pending should do the same as example_failed with SKIP comment
    ok 14 - TapFormatter dump_summary should print the number of tests if there were tests
    ok 15 - TapFormatter dump_summary should print nothing if there were not tests

::

    $ rake format=junit | sed 1d
    <?xml version="1.0" encoding="utf-8"?>
    <testsuite errors="0" failures="0" skipped="0" tests="15" time="0.008132403" timestamp="2012-12-03T17:52:59-02:00">
      <properties/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter should initialize the tests with failures and success" time="0.00258258"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter example_passed should push the example obj into success list" time="0.000210973"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter example_failed should push the example obj into failures list" time="0.00019527"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter example_pending should push the example obj into the skipped list" time="0.000194194"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter read_failure should ignore if there is no exception" time="0.000330483"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter read_failure should attempt to read exception if exception encountered is nil" time="0.000629093"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter read_failure should read message and backtrace from the example" time="0.00054028"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter dump_summary should print the junit xml" time="0.000990184"/>
      <testcase classname="./spec/rspec-extra-formatters/junit_formatter_spec.rb" name="JUnitFormatter dump_summary should escape characteres &lt;,&gt;,&amp;,&quot; before building xml" time="0.00040997"/>
      <testcase classname="./spec/rspec-extra-formatters/tap_formatter_spec.rb" name="TapFormatter should initialize the counter to 0" time="7.3649e-05"/>
      <testcase classname="./spec/rspec-extra-formatters/tap_formatter_spec.rb" name="TapFormatter example_passed should increment the counter and use the full_description attribute" time="0.000205454"/>
      <testcase classname="./spec/rspec-extra-formatters/tap_formatter_spec.rb" name="TapFormatter example_failed should increment the counter and use the full_description attribute" time="0.00029728"/>
      <testcase classname="./spec/rspec-extra-formatters/tap_formatter_spec.rb" name="TapFormatter example_pending should do the same as example_failed with SKIP comment" time="0.000325112"/>
      <testcase classname="./spec/rspec-extra-formatters/tap_formatter_spec.rb" name="TapFormatter dump_summary should print the number of tests if there were tests" time="0.000372799"/>
      <testcase classname="./spec/rspec-extra-formatters/tap_formatter_spec.rb" name="TapFormatter dump_summary should print nothing if there were not tests" time="6.3588e-05"/>
    </testsuite>

Using it
========

Make sure you add `-r "rspec-extra-formatters"` to rspec options and both `-f JUnitFormatter` and `-f TapFormatter` should work. :-)

