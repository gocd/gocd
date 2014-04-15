### 2.11.3 / 2012-09-04
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.11.2...v.2.11.3)

Bug fixes

* Fix (and deprecate) `expect { }.should` syntax so that it works even
  though it was never a documented or intended syntax. It worked as a
  consequence of the implementation of `expect` in RSpec 2.10 and
  earlier. (Myron Marston)
* Ensure #== is defined on build in matchers so that they can be composed.
  For example:

    expect {
      user.emailed!
    }.to change { user.last_emailed_at }.to be_within(1.second).of(Time.zone.now)

### 2.11.2 / 2012-07-25
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.11.1...v2.11.2)

Bug fixes

* Define `should` and `should_not` on `Object` rather than `BasicObject`
  on MacRuby. On MacRuby, `BasicObject` is defined but is not the root
  of the object hierarchy. (Gabriel Gilder)

### 2.11.1 / 2012-07-08
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.11.0...v2.11.1)

Bug fixes

* Constrain `actual` in `be_within` matcher to values that respond to `-` instead
  of requiring a specific type.
    * `Time`, for example, is a legit alternative.

### 2.11.0 / 2012-07-07
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.10.0...v2.11.0)

Enhancements

* Expand `expect` syntax so that it supports expections on bare values
  in addition to blocks (Myron Marston).
* Add configuration options to control available expectation syntaxes
  (Myron Marston):
  * `RSpec.configuration.expect_with(:rspec) { |c| c.syntax = :expect }`
  * `RSpec.configuration.expect_with(:rspec) { |c| c.syntax = :should }`
  * `RSpec.configuration.expect_with(:rspec) { |c| c.syntax = [:should, :expect] }`
  * `RSpec.configuration.add_should_and_should_not_to Delegator`

Bug fixes

* Allow only `Numeric` values to be the "actual" in the `be_within` matcher.
  This prevents confusing error messages. (Su Zhang @zhangsu)
* Define `should` and `should_not` on `BasicObject` rather than `Kernel`
  on 1.9. This makes `should` and `should_not` work properly with
  `BasicObject`-subclassed proxy objects like `Delegator`. (Myron
  Marston)

### 2.10.0 / 2012-05-03
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.9.1...v2.10.0)

Enhancements

* Add new `start_with` and `end_with` matchers (Jeremy Wadsack)
* Add new matchers for specifying yields (Myron Marston):
    * `expect {...}.to yield_control`
    * `expect {...}.to yield_with_args(1, 2, 3)`
    * `expect {...}.to yield_with_no_args`
    * `expect {...}.to yield_successive_args(1, 2, 3)`
* `match_unless_raises` takes multiple exception args

Bug fixes

* Fix `be_within` matcher to be inclusive of delta.
* Fix message-specific specs to pass on Rubinius (John Firebaugh)

### 2.9.1 / 2012-04-03
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.9.0...v2.9.1)

Bug fixes

* Provide a helpful message if the diff between two objects is empty.
* Fix bug diffing single strings with multiline strings.
* Fix for error with using custom matchers inside other custom matchers
  (mirasrael)
* Fix using execution context methods in nested DSL matchers (mirasrael)

### 2.9.0 / 2012-03-17
[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.8.0...v2.9.0)

Enhancements

* Move built-in matcher classes to RSpec::Matchers::BuiltIn to reduce pollution
  of RSpec::Matchers (which is included in every example).
* Autoload files with matcher classes to improve load time.

Bug fixes

* Align `respond_to?` and `method_missing` in DSL-defined matchers.
* Clear out user-defined instance variables between invocations of DSL-defined
  matchers.
* Dup the instance of a DSL generated matcher so its state is not changed by
  subsequent invocations.
* Treat expected args consistently across positive and negative expectations
  (thanks to Ralf Kistner for the heads up)

### 2.8.0 / 2012-01-04

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.8.0.rc2...v2.8.0)

Enhancements

* Better diff output for Hash (Philippe Creux)
* Eliminate Ruby warnings (Olek Janiszewski)

### 2.8.0.rc2 / 2011-12-19

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.8.0.rc1...v2.8.0.rc2)

No changes for this release. Just releasing with the other rspec gems.

### 2.8.0.rc1 / 2011-11-06

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.7.0...v2.8.0.rc1)

Enhancements

* Use classes for the built-in matchers (they're faster).
* Eliminate Ruby warnings (Matijs van Zuijlen)

### 2.7.0 / 2011-10-16

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.6.0...v2.7.0)

Enhancements

* `HaveMatcher` converts argument using `to_i` (Alex Bepple & Pat Maddox)
* Improved failure message for the `have_xxx` matcher (Myron Marston)
* `HaveMatcher` supports `count` (Matthew Bellantoni)
* Change matcher dups `Enumerable` before the action, supporting custom
  `Enumerable` types like `CollectionProxy` in Rails (David Chelimsky)

Bug fixes

* Fix typo in `have(n).xyz` documentation (Jean Boussier)
* fix `safe_sort` for ruby 1.9.2 (`Kernel` now defines `<=>` for Object) (Peter
  van Hardenberg)

### 2.6.0 / 2011-05-12

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.5.0...v2.6.0)

Enhancements

* `change` matcher accepts regexps (Robert Davis)
* better descriptions for `have_xxx` matchers (Magnus Bergmark)
* `range.should cover(*values)` (Anders Furseth)

Bug fixes

* Removed non-ascii characters that were choking rcov (Geoffrey Byers)
* change matcher dups arrays and hashes so their before/after states can be
  compared correctly.
* Fix the order of inclusion of RSpec::Matchers in Test::Unit::TestCase and
  MiniTest::Unit::TestCase to prevent a SystemStackError (Myron Marston)

### 2.5.0 / 2011-02-05

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.4.0...v2.5.0)

Enhancements

* `should exist` works with `exist?` or `exists?` (Myron Marston)
* `expect { ... }.not_to do_something` (in addition to `to_not`)

Documentation

* improved docs for raise_error matcher (James Almond)

### 2.4.0 / 2011-01-02

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.3.0...v2.4.0)

No functional changes in this release, which was made to align with the
rspec-core-2.4.0 release.

Enhancements

* improved RDoc for change matcher (Jo Liss)

### 2.3.0 / 2010-12-12

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.2.1...v2.3.0)

Enhancements

* diff strings when include matcher fails (Mike Sassak)

### 2.2.0 / 2010-11-28

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.1.0...v2.2.0)

### 2.1.0 / 2010-11-07

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.0.1...v2.1.0)

Enhancements

* `be_within(delta).of(expected)` matcher (Myron Marston)
* Lots of new Cucumber features (Myron Marston)
* Raise error if you try `should != expected` on Ruby-1.9 (Myron Marston)
* Improved failure messages from `throw_symbol` (Myron Marston)

Bug fixes

* Eliminate hard dependency on `RSpec::Core` (Myron Marston)
* `have_matcher` - use pluralize only when ActiveSupport inflections are indeed
  defined (Josep M Bach)
* throw_symbol matcher no longer swallows exceptions (Myron Marston)
* fix matcher chaining to avoid name collisions (Myron Marston)

### 2.0.0 / 2010-10-10

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.0.0.rc...v2.0.0)

Enhancements

* Add match_for_should_not method to matcher DSL (Myron Marston)

Bug fixes

* `respond_to` matcher works correctly with `should_not` with multiple methods
  (Myron Marston)
* `include` matcher works correctly with `should_not` with multiple values
  (Myron Marston)

### 2.0.0.rc / 2010-10-05

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.0.0.beta.22...v2.0.0.rc)

Enhancements

* `require 'rspec/expectations'` in a T::U or MiniUnit suite (Josep M. Bach)

Bug fixes

* change by 0 passes/fails correctly (Len Smith)
* Add description to satisfy matcher

### 2.0.0.beta.22 / 2010-09-12

[full changelog](http://github.com/rspec/rspec-expectations/compare/v2.0.0.beta.20...v2.0.0.beta.22)

Enhancements

* diffing improvements
    * diff multiline strings
    * don't diff single line strings
    * don't diff numbers (silly)
    * diff regexp + multiline string

Bug fixes
    * `should[_not]` change now handles boolean values correctly
