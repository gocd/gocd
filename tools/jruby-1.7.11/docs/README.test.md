# Writing Tests

1. There are actually *4* core test suites:

   a) `org.jruby.test.MainTestSuite`, a suite of pure-java tests;

   b) `org.jruby.test.TestUnitTestSuite`, which runs a suite of files named
   one-per-line in `test/jruby_index` (and other `test/*_index` files).
   Note: some are commented out.

   c) `Rubyspecs`, a recent point-in-time copy of [RubySpec](http://rubyspec.org)
   project. The specs
   are written in `rspec `format and aim to test conformance of JRuby
   against the Ruby specification.

   d) MRI's own tests, synced periodically. The tests are run with
   `rake test:mri19` and known failing tests are marked as such in 
   excludes files under `test/externals/ruby1.9/excludes`.

2. If you'd like to contribute new tests, we'd prefer you write them via the
`rubyspecs` project.  If the tests are very specific to JRuby (like Java
Integration) we prefer writing `rspec`s under our `specs` directory.

3. We're also interested in running tests for other ruby software suites; if
you want to grab your favorite ruby software and run its own tests under JRuby
and report to the mailing list at `dev@jruby.codehaus.org`, that would be great.

4. Please file any and all patches in [Github issues](https://github.com/jruby/jruby/issues)

# Running Tests

Using ant, type

    ant test

The three test suites should be run in order.

To run rubyspecs, type

    ant spec

A slightly shorter run we run frequently is:

    ant spec-short

The rubyspecs will be fetched over the net, and then executed.

To run an individual test:

1. run `ant install-dev-gems` which will install `bin/rspec` for use.
2. run `ant compile-test` to compile the java fixtures (you don't need to wait for the tests to finish) then test as normal with `bin/rspec spec/path-to-spec`
