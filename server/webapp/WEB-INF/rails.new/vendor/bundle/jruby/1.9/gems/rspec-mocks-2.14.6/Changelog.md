### 2.14.6 development
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.5...v2.14.6)

Bug Fixes:

* Ensure `any_instance` method stubs and expectations are torn down regardless of
  expectation failures. (Sam Phippen)

### 2.14.5 / 2014-02-01
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.4...v2.14.5)

Bug Fixes:

* Fix regression that caused block implementations to not receive all
  args on 1.8.7 if the block also receives a block, due to Proc#arity
  reporting `1` no matter how many args the block receives if it
  receives a block, too. (Myron Marston)

### 2.14.4 / 2013-10-15
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.3...v2.14.4)

Bug Fixes:

* Fix issue where unstubing methods on "any instances" would not
  remove stubs on existing instances (Jon Rowe)
* Fix issue with receive(:message) do ... end precedence preventing
  the usage of modifications (`and_return` etc) (Jon Rowe)

### 2.14.3 / 2013-08-08
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.2...v2.14.3)

Bug Fixes:

* Fix stubbing some instance methods for classes whose hierarchy includes
  a prepended Module (Bradley Schaefer)

### 2.14.2 / 2013-07-30
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.1...v2.14.2)

Bug Fixes:

* Fix `as_null_object` doubles so that they return `nil` from `to_ary`
  (Jon Rowe).
* Fix regression in 2.14 that made `stub!` (with an implicit receiver)
  return a test double rather than stub a method (Myron Marston).

### 2.14.1 / 2013-07-07
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.0...v2.14.1)

Bug Fixes:

* Restore `double.as_null_object` behavior from 2.13 and earlier: a
  double's nullness persisted between examples in earlier examples.
  While this is not an intended use case (test doubles are meant to live
  for only one example), we don't want to break behavior users rely
  on in a minor relase.  This will be deprecated in 2.99 and removed
  in 3.0. (Myron Marston)

### 2.14.0 / 2013-07-06
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.14.0.rc1...v2.14.0)

Enhancements:

* Document test spies in the readme. (Adarsh Pandit)
* Add an `array_including` matcher. (Sam Phippen)
* Add a syntax-agnostic API for mocking or stubbing a method. This is
  intended for use by libraries such as rspec-rails that need to mock
  or stub a method, and work regardless of the syntax the user has
  configured (Paul Annesley, Myron Marston and Sam Phippen).

Bug Fixes:

* Fix `double` so that it sets up passed stubs correctly regardless of
  the configured syntax (Paul Annesley).
* Allow a block implementation to be used in combination with
  `and_yield`, `and_raise`, `and_return` or `and_throw`. This got fixed
  in 2.13.1 but failed to get merged into master for the 2.14.0.rc1
  release (Myron Marston).
* `Marshal.dump` does not unnecessarily duplicate objects when rspec-mocks has
  not been fully initialized. This could cause errors when using `spork` or
  similar preloading gems (Andy Lindeman).

### 2.14.0.rc1 / 2013-05-27
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.13.0...v2.14.0.rc1)

Enhancements:

* Refactor internals so that the mock proxy methods and state are held
  outside of the mocked object rather than inside it. This paves the way
  for future syntax enhancements and removes the need for some hacky
  work arounds for `any_instance` dup'ing and `YAML` serialization,
  among other things. Note that the code now relies upon `__id__`
  returning a unique, consistent value for any object you want to
  mock or stub (Myron Marston).
* Add support for test spies. This allows you to verify a message
  was received afterwards using the `have_received` matcher.
  Note that you must first stub the method or use a null double.
  (Joe Ferris and Joël Quenneville)
* Make `at_least` and `at_most` style receive expectations print that they were
  expecting at least or at most some number of calls, rather than just the
  number of calls given in the expectation (Sam Phippen)
* Make `with` style receive expectations print the args they were expecting, and
  the args that they got (Sam Phippen)
* Fix some warnings seen under ruby 2.0.0p0 (Sam Phippen).
* Add a new `:expect` syntax for message expectations
  (Myron Marston and Sam Phippen).

Bug fixes

* Fix `any_instance` so that a frozen object can be `dup`'d when methods
  have been stubbed on that type using `any_instance` (Jon Rowe).
* Fix `and_call_original` so that it properly raises an `ArgumentError`
  when the wrong number of args are passed (Jon Rowe).
* Fix `double` on 1.9.2 so you can wrap them in an Array
  using `Array(my_double)` (Jon Rowe).
* Fix `stub_const` and `hide_const` to handle constants that redefine `send`
  (Sam Phippen).
* Fix `Marshal.dump` extension so that it correctly handles nil.
  (Luke Imhoff, Jon Rowe)
* Fix isolation of `allow_message_expectations_on_nil` (Jon Rowe)
* Use inspect to format actual arguments on expectations in failure messages (#280, Ben Langfeld)
* Protect against improperly initialised test doubles (#293) (Joseph Shraibman and Jon Rowe)

Deprecations

* Deprecate `stub` and `mock` as aliases for `double`. `double` is the
  best term for creating a test double, and it reduces confusion to
  have only one term (Michi Huber).
* Deprecate `stub!` and `unstub!` in favor of `stub` and `unstub`
  (Jon Rowe).
* Deprecate `at_least(0).times` and `any_number_of_times` (Michi Huber).

### 2.13.1 / 2013-04-06
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.13.0...v2.13.1)

Bug fixes

* Allow a block implementation to be used in combination with
  `and_yield`, `and_raise`, `and_return` or `and_throw` (Myron Marston).

### 2.13.0 / 2013-02-23
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.12.2...v2.13.0)

Bug fixes

* Fix bug that caused weird behavior when a method that had
  previously been stubbed with multiple return values (e.g.
  `obj.stub(:foo).and_return(1, 2)`) was later mocked with a
  single return value (e.g. `obj.should_receive(:foo).once.and_return(1)`).
  (Myron Marston)
* Fix bug related to a mock expectation for a method that already had
  multiple stubs with different `with` constraints. Previously, the
  first stub was used, even though it may not have matched the passed
  args. The fix defers this decision until the message is received so
  that the proper stub response can be chosen based on the passed
  arguments (Myron Marston).
* Do not call `nil?` extra times on a mocked object, in case `nil?`
  itself is expected a set number of times (Myron Marston).
* Fix `missing_default_stub_error` message so array args are handled
  properly (Myron Marston).
* Explicitly disallow `any_instance.unstub!` (Ryan Jones).
* Fix `any_instance` stubbing so that it works with `Delegator`
  subclasses (Myron Marston).
* Fix `and_call_original` so that it works with `Delegator` subclasses
  (Myron Marston).
* Fix `any_instance.should_not_receive` when `any_instance.should_receive`
  is used on the same class in the same example. Previously it would
  wrongly report a failure even when the message was not received
  (Myron Marston).

### 2.12.2 / 2013-01-27
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.12.1...v.2.12.2)

Bug fixes

* Fix `and_call_original` to work properly for methods defined
  on a module extended onto an object instance (Myron Marston).
* Fix `stub_const` with an undefined constnat name to work properly
  with constant strings that are prefixed with `::` -- and edge case
  I missed in the bug fix in the 2.12.1 release (Myron Marston).
* Ensure method visibility on a partial mock is restored after reseting
  method stubs, even on a singleton module (created via `extend self`)
  when the method visibility differs between the instance and singleton
  versions (Andy Lindeman).

### 2.12.1 / 2012-12-21
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.12.0...v2.12.1)

Bug fixes

* Fix `any_instance` to support `and_call_original`.
  (Myron Marston)
* Properly restore stubbed aliased methods on rubies
  that report the incorrect owner (Myron Marston and Andy Lindeman).
* Fix `hide_const` and `stub_const` with a defined constnat name to
  work properly with constant strings that are prefixed with `::` (Myron Marston).

### 2.12.0 / 2012-11-12
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.11.3...v2.12.0)

Enhancements

* `and_raise` can accept an exception class and message, more closely
  matching `Kernel#raise` (e.g., `foo.stub(:bar).and_raise(RuntimeError, "message")`)
  (Bas Vodde)
* Add `and_call_original`, which will delegate the message to the
  original method (Myron Marston).

Deprecations:

* Add deprecation warning when using `and_return` with `should_not_receive`
  (Neha Kumari)

### 2.11.3 / 2012-09-19
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.11.2...v2.11.3)

Bug fixes

* Fix `:transfer_nested_constants` option of `stub_const` so that it
  doesn't blow up when there are inherited constants. (Myron Marston)
* `any_instance` stubs can be used on classes that override `Object#method`.
  (Andy Lindeman)
* Methods stubbed with `any_instance` are unstubbed after the test finishes.
  (Andy Lindeman)
* Fix confusing error message when calling a mocked class method an
  extra time with the wrong arguments (Myron Marston).

### 2.11.2 / 2012-08-11
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.11.1...v2.11.2)

Bug fixes

* Don't modify `dup` on classes that don't support `dup` (David Chelimsky)
* Fix `any_instance` so that it works properly with methods defined on
  a superclass. (Daniel Eguzkiza)
* Fix `stub_const` so that it works properly for nested constants that
  share a name with a top-level constant (e.g. "MyGem::Hash"). (Myron
  Marston)

### 2.11.1 / 2012-07-09
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.11.0...v2.11.1)

Bug fixes

* Fix `should_receive` so that when it is called on an `as_null_object`
  double with no implementation, and there is a previous explicit stub
  for the same method, the explicit stub remains (rather than being
  overriden with the null object implementation--`return self`). (Myron Marston)

### 2.11.0 / 2012-07-07
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.10.1...v2.11.0)

Enhancements

* Expose ArgumentListMatcher as a formal API
    * supports use by 3rd party mock frameworks like Surrogate
* Add `stub_const` API to stub constants for the duration of an
  example (Myron Marston).

Bug fixes

* Fix regression of edge case behavior. `double.should_receive(:foo) { a }`
  was causing a NoMethodError when `double.stub(:foo).and_return(a, b)`
  had been setup before (Myron Marston).
* Infinite loop generated by using `any_instance` and `dup`. (Sidu Ponnappa @kaiwren)
* `double.should_receive(:foo).at_least(:once).and_return(a)` always returns a
  even if `:foo` is already stubbed.
* Prevent infinite loop when interpolating a null double into a string
  as an integer (`"%i" % double.as_null_object`). (Myron Marston)
* Fix `should_receive` so that null object behavior (e.g. returning
  self) is preserved if no implementation is given (Myron Marston).
* Fix `and_raise` so that it raises `RuntimeError` rather than
  `Exception` by default, just like ruby does. (Andrew Marshall)

### 2.10.1 / 2012-05-05
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.10.0...v2.10.1)

Bug fixes

* fix regression of edge case behavior
  (https://github.com/rspec/rspec-mocks/issues/132)
    * fixed failure of `object.should_receive(:message).at_least(0).times.and_return value`
    * fixed failure of `object.should_not_receive(:message).and_return value`

### 2.10.0 / 2012-05-03
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.9.0...v2.10.0)

Bug fixes

* fail fast when an `exactly` or `at_most` expectation is exceeded

### 2.9.0 / 2012-03-17
[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.8.0...v2.9.0)

Enhancements

* Support order constraints across objects (preethiramdev)

Bug fixes

* Allow a `as_null_object` to be passed to `with`
* Pass proc to block passed to stub (Aubrey Rhodes)
* Initialize child message expectation args to match any args (#109 -
  preethiramdev)

### 2.8.0 / 2012-01-04

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.8.0.rc2...v2.8.0)

No changes for this release. Just releasing with the other rspec gems.

### 2.8.0.rc2 / 2011-12-19

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.8.0.rc1...v2.8.0.rc2)

No changes for this release. Just releasing with the other rspec gems.

### 2.8.0.rc1 / 2011-11-06

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.7.0...v2.8.0.rc1)

Enhancements

* Eliminate Ruby warnings (Matijs van Zuijlen)

### 2.7.0 / 2011-10-16

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.6.0...v2.7.0)

Enhancements

* Use `__send__` rather than `send` (alextk)
* Add support for `any_instance.stub_chain` (Sidu Ponnappa)
* Add support for `any_instance` argument matching based on `with` (Sidu
  Ponnappa and Andy Lindeman)

Changes

* Check for `failure_message_for_should` or `failure_message` instead of
  `description` to detect a matcher (Tibor Claassen)

Bug fixes

* pass a hash to `any_instance.stub`. (Justin Ko)
* allow `to_ary` to be called without raising `NoMethodError` (Mikhail
  Dieterle)
* `any_instance` properly restores private methods (Sidu Ponnappa)

### 2.6.0 / 2011-05-12

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.5.0...v2.6.0)

Enhancements

* Add support for `any_instance.stub` and `any_instance.should_receive` (Sidu
  Ponnappa and Andy Lindeman)

Bug fixes

* fix bug in which multiple chains with shared messages ending in hashes failed
  to return the correct value

### 2.5.0 / 2011-02-05

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.4.0...v2.5.0)

Bug fixes

* message expectation counts now work in combination with a stub (Damian
  Nurzynski)
* fix failure message when message received with incorrect args (Josep M.
  Bach)

### 2.4.0 / 2011-01-02

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.3.0...v2.4.0)

No functional changes in this release, which was made to align with the
rspec-core-2.4.0 release.

### 2.3.0 / 2010-12-12

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.2.0...v2.3.0)

Bug fixes

* Fix our Marshal extension so that it does not interfere with objects that
  have their own `@mock_proxy` instance variable. (Myron Marston)

### 2.2.0 / 2010-11-28

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.1.0...v2.2.0)

Enhancements

* Added "rspec/mocks/standalone" for exploring the rspec-mocks in irb.

Bug fix

* Eliminate warning on splat args without parens (Gioele Barabucci)
* Fix bug where `obj.should_receive(:foo).with(stub.as_null_object)` would pass
  with a false positive.

### 2.1.0 / 2010-11-07

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.0.1...v2.1.0)

Bug fixes

* Fix serialization of stubbed object (Josep M Bach)

### 2.0.0 / 2010-10-10

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.0.0.beta.22...v2.0.0)

### 2.0.0.rc / 2010-10-05

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.0.0.beta.22...v2.0.0.rc)

Enhancements

* support passing a block to an expectation block (Nicolas Braem)
    * `obj.should_receive(:msg) {|&block| ... }`

Bug fixes

* Fix YAML serialization of stub (Myron Marston)
* Fix rdoc rake task (Hans de Graaff)

### 2.0.0.beta.22 / 2010-09-12

[full changelog](http://github.com/rspec/rspec-mocks/compare/v2.0.0.beta.20...v2.0.0.beta.22)

Bug fixes

* fixed regression that broke `obj.stub_chain(:a, :b => :c)`
* fixed regression that broke `obj.stub_chain(:a, :b) { :c }`
* `respond_to?` always returns true when using `as_null_object`
