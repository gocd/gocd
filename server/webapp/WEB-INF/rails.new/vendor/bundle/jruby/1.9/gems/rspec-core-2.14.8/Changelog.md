### 2.14.8 / 2014-02-27
[Full Changelog](http://github.com/rspec/rspec-core/compare/v2.14.7...v2.14.8)

Bug fixes:

* Fix regression with the `TextMateFormatter` that prevented backtrace links
  from being clickable. (Stefan Daschek)

### 2.14.7 / 2013-10-29
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.6...v2.14.7)

Bug fixes:

* Fix regression in 2.14.6 that broke the Fivemat formatter.
  It depended upon either
  `example.execution_result[:exception].pending_fixed?` (which
  was removed in 2.14.6 to fix an issue with frozen error objects)
  or `RSpec::Core::PendingExampleFixedError` (which was renamed
  to `RSpec::Core::Pending::PendingExampleFixedError` in 2.8.
  This fix makes a constant alias for the old error name.
  (Myron Marston)

### 2.14.6 / 2013-10-15
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.5...v2.14.6)

Bug fixes:

* Format stringified numbers correctly when mathn library is loaded.
  (Jay Hayes)
* Fix an issue that prevented the use of frozen error objects. (Lars Gierth)

### 2.14.5 / 2013-08-13
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.4...v2.14.5)

Bug fixes:

* Fix a `NoMethodError` that was being raised when there were no shared
  examples or contexts declared and `RSpec.world.reset` is invoked.
  (thepoho, Jon Rowe, Myron Marston)
* Fix a deprecation warning that was being incorrectly displayed when
  `shared_examples` are declared at top level in a `module` scope.
  (Jon Rowe)
* Fix after(:all) hooks so consecutive (same context) scopes will run even if
  one raises an error. (Jon Rowe, Trejkaz)
* JsonFormatter no longer dies if `dump_profile` isn't defined (Alex / @MasterLambaster, Jon Rowe)

### 2.14.4 / 2013-07-21
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.3...v2.14.4)

Bug fixes

* Fix regression in 2.14: ensure configured requires (via `-r` option)
  are loaded before spec files are loaded. This allows the spec files
  to programatically change the file pattern (Jon Rowe).
* Autoload `RSpec::Mocks` and `RSpec::Expectations` when referenced if
  they are not already loaded (`RSpec::Matches` has been autoloaded
  for a while). In the `rspec` gem, we changed it recently to stop
  loading `rspec/mocks` and `rspec/expectations` by default, as some
  users reported problems where they were intending to use mocha,
  not rspec-mocks, but rspec-mocks was loaded and causing a conflict.
  rspec-core loads mocks and expectations at the appropriate time, so
  it seemed like a safe change -- but caused a problem for some authors
  of libraries that integrate with RSpec. This fixes that problem.
  (Myron Marston)
* Gracefully handle a command like `rspec --profile path/to/spec.rb`:
  the `path/to/spec.rb` arg was being wrongly treated as the `profile`
  integer arg, which got cast `0` using `to_i`, causing no profiled
  examples to be printed. (Jon Rowe)

### 2.14.3 / 2013-07-13
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.2...v2.14.3)

Bug fixes

* Fix deprecation notices issued from `RSpec::Core::RakeTask` so
  that they work properly when all of rspec-core is not loaded.
  (This was a regression in 2.14) (Jon Rowe)

### 2.14.2 / 2013-07-09
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.1...v2.14.2)

Bug fixes

* Fix regression caused by 2.14.1 release: formatters that
  report that they `respond_to?` a notification, but had
  no corresponding method would raise an error when registered.
  The new fix is to just implement `start` on the deprecation
  formatter to fix the original JRuby/ruby-debug issue.
  (Jon Rowe)

### 2.14.1 / 2013-07-08
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.0...v2.14.1)

Bug fixes

* Address deprecation formatter failure when using `ruby-debug` on
  JRuby: fix `RSpec::Core::Reporter` to not send a notification
  when the formatter's implementation of the notification method
  comes from `Kernel` (Alex Portnov, Jon Rowe).

### 2.14.0 / 2013-07-06
[full changelog](http://github.com/rspec/rspec-core/compare/v2.14.0.rc1...v2.14.0)

Enhancements

* Apply focus to examples defined with `fit` (equivalent of
  `it "description", focus: true`) (Michael de Silva)

Bug fixes

* Ensure methods defined by `let` take precedence over others
  when there is a name collision (e.g. from an included module).
  (Jon Rowe, Andy Lindeman and Myron Marston)

### 2.14.0.rc1 / 2013-05-27
[full changelog](http://github.com/rspec/rspec-core/compare/v2.13.1...v2.14.0.rc1)

Enhancements

* Improved Windows detection inside Git Bash, for better `--color` handling.
* Add profiling of the slowest example groups to `--profile` option.
  The output is sorted by the slowest average example groups.
* Don't show slow examples if there's a failure and both `--fail-fast`
  and `--profile` options are used (Paweł Gościcki).
* Rather than always adding `spec` to the load path, add the configured
  `--default-path` to the load path (which defaults to `spec`). This
  better supports folks who choose to put their specs in a different
  directory (John Feminella).
* Add some logic to test time duration precision. Make it a
  function of time, dropping precision as the time increases. (Aaron Kromer)
* Add new `backtrace_inclusion_patterns` config option. Backtrace lines
  that match one of these patterns will _always_ be included in the
  backtrace, even if they match an exclusion pattern, too (Sam Phippen).
* Support ERB trim mode using the `-` when parsing `.rspec` as ERB
  (Gabor Garami).
* Give a better error message when let and subject are called without a block.
  (Sam Phippen).
* List the precedence of `.rspec-local` in the configuration documentation
  (Sam Phippen)
* Support `{a,b}` shell expansion syntax in `--pattern` option
  (Konstantin Haase).
* Add cucumber documentation for --require command line option
  (Bradley Schaefer)
* Expose configruation options via config:
  * `config.libs` returns the libs configured to be added onto the load path
  * `full_backtrace?` returns the state of the backtrace cleaner
  * `debug?` returns true when the debugger is loaded
  * `line_numbers` returns the line numbers we are filtering by (if any)
  * `full_description` returns the RegExp used to filter descriptions
  (Jon Rowe)
* Add setters for RSpec.world and RSpec.configuration (Alex Soulim)
* Configure ruby's warning behaviour with `--warnings` (Jon Rowe)
* Fix an obscure issue on old versions of `1.8.7` where `Time.dup` wouldn't
  allow access to `Time.now` (Jon Rowe)
* Make `shared_examples_for` context aware, so that keys may be safely reused
  in multiple contexts without colliding. (Jon Rowe)
* Add a configurable `deprecation_stream` (Jon Rowe)
* Publish deprecations through a formatter (David Chelimsky)

Bug fixes

* Make JSON formatter behave the same when it comes to `--profile` as
  the text formatter (Paweł Gościcki).
* Fix named subjects so that if an inner group defines a method that
  overrides the named method, `subject` still retains the originally
  declared value (Myron Marston).
* Fix random ordering so that it does not cause `rand` in examples in
  nested sibling contexts to return the same value (Max Shytikov).
* Use the new `backtrace_inclusion_patterns` config option to ensure
  that folks who develop code in a directory matching one of the default
  exclusion patterns (e.g. `gems`) still get the normal backtrace
  filtering (Sam Phippen).
* Fix ordering of `before` hooks so that `before` hooks declared in
  `RSpec.configure` run before `before` hooks declared in a shared
  context (Michi Huber and Tejas Dinkar).
* Fix `Example#full_description` so that it gets filled in by the last
  matcher description (as `Example#description` already did) when no
  doc string has been provided (David Chelimsky).
* Fix the memoized methods (`let` and `subject`) leaking `define_method`
  as a `public` method. (Thomas Holmes and Jon Rowe) (#873)
* Fix warnings coming from the test suite. (Pete Higgins)

Deprecations

* Deprecate `Configuration#backtrace_clean_patterns` in favor of
  `Configuration#backtrace_exclusion_patterns` for greater consistency
  and symmetry with new `backtrace_inclusion_patterns` config option
  (Sam Phippen).
* Deprecate `Configuration#requires=` in favor of using ruby's
  `require`. Requires specified by the command line can still be
  accessed by the `Configuration#require` reader. (Bradley Schaefer)
* Deprecate calling `SharedExampleGroups` defined across sibling contexts
  (Jon Rowe)

### 2.13.1 / 2013-03-12
[full changelog](http://github.com/rspec/rspec-core/compare/v2.13.0...v2.13.1)

Bug fixes

* Use hook classes as proxies rather than extending hook blocks to support
  lambdas for before/after/around hooks. (David Chelimsky)
* Fix regression in 2.13.0 that caused confusing behavior when overriding
  a named subject with an unnamed subject in an inner group and then
  referencing the outer group subject's name. The fix for this required
  us to disallow using `super` in a named subject (which is confusing,
  anyway -- named subjects create 2 methods, so which method on the
  parent example group are you `super`ing to?) but `super` in an unnamed
  subject continues to work (Myron Marston).
* Do not allow a referenced `let` or `subject` in `before(:all)` to cause
  other `let` declarations to leak across examples (Myron Marston).
* Work around odd ruby 1.9 bug with `String#match` that was triggered
  by passing it a regex from a `let` declaration. For more info, see
  http://bugs.ruby-lang.org/issues/8059 (Aaron Kromer).
* Add missing `require 'set'` to `base_text_formatter.rb` (Tom
  Anderson).

Deprecations

* Deprecate accessing `let` or `subject` declarations in `before(:all)`.
  These were not intended to be called in a `before(:all)` hook, as
  they exist to define state that is reset between each example, while
  `before(:all)` exists to define state that is shared across examples
  in an example group (Myron Marston).

### 2.13.0 / 2013-02-23
[full changelog](http://github.com/rspec/rspec-core/compare/v2.12.2...v2.13.0)

Enhancements

* Allow `--profile` option to take a count argument that
  determines the number of slow examples to dump
  (Greggory Rothmeier).
* Add `subject!` that is the analog to `let!`. It defines an
  explicit subject and sets a `before` hook that will invoke
  the subject (Zubin Henner).
* Fix `let` and `subject` declaration so that `super`
  and `return` can be used in them, just like in a normal
  method. (Myron Marston)
* Allow output colors to be configured individually.
  (Charlie Maffitt)
* Always dump slow examples when `--profile` option is given,
  even when an example failed (Myron Marston).

Bug fixes

* Don't blow up when dumping error output for instances
  of anonymous error classes (Myron Marston).
* Fix default backtrace filters so lines from projects
  containing "gems" in the name are not filtered, but
  lines from installed gems still are (Myron Marston).
* Fix autotest command so that is uses double quotes
  rather than single quotes for windows compatibility
  (Jonas Tingeborn).
* Fix `its` so that uses of `subject` in a `before` or `let`
  declaration in the parent group continue to reference the
  parent group's subject. (Olek Janiszewski)

### 2.12.2 / 2012-12-13
[full changelog](http://github.com/rspec/rspec-core/compare/v2.12.1...v2.12.2)

Bug fixes

* Fix `RSpec::Core::RakeTask` so that it is compatible with rake 0.8.7
  on ruby 1.8.7. We had accidentally broke it in the 2.12 release
  (Myron Marston).
* Fix `RSpec::Core::RakeTask` so it is tolerant of the `Rspec` constant
  for backwards compatibility (Patrick Van Stee)

### 2.12.1 / 2012-12-01
[full changelog](http://github.com/rspec/rspec-core/compare/v2.12.0...v2.12.1)

Bug fixes

* Specs are run even if another at\_exit hook calls `exit`. This allows
  Test::Unit and RSpec to run together. (Suraj N. Kurapati)
* Fix full doc string concatenation so that it handles the case of a
  method string (e.g. "#foo") being nested under a context string
  (e.g. "when it is tuesday"), so that we get "when it is tuesday #foo"
  rather than "when it is tuesday#foo". (Myron Marston)
* Restore public API I unintentionally broke in 2.12.0:
  `RSpec::Core::Formatters::BaseFormatter#format_backtrce(backtrace, example)`
  (Myron Marston).

### 2.12.0 / 2012-11-12
[full changelog](http://github.com/rspec/rspec-core/compare/v2.11.1...v2.12.0)

Enhancements

* Add support for custom ordering strategies for groups and examples.
  (Myron Marston)
* JSON Formatter (Alex Chaffee)
* Refactor rake task internals (Sam Phippen)
* Refactor HtmlFormatter (Pete Hodgson)
* Autotest supports a path to Ruby that contains spaces (dsisnero)
* Provide a helpful warning when a shared example group is redefined.
  (Mark Burns).
* `--default_path` can be specified as `--default-line`. `--line_number` can be
  specified as `--line-number`. Hyphens are more idiomatic command line argument
  separators (Sam Phippen).
* A more useful error message is shown when an invalid command line option is
  used (Jordi Polo).
* Add `format_docstrings { |str| }` config option. It can be used to
  apply formatting rules to example group and example docstrings.
  (Alex Tan)
* Add support for an `.rspec-local` options file. This is intended to
  allow individual developers to set options in a git-ignored file that
  override the common project options in `.rspec`. (Sam Phippen)
* Support for mocha 0.13.0. (Andy Lindeman)

Bug fixes

* Remove override of `ExampleGroup#ancestors`. This is a core ruby method that
  RSpec shouldn't override. Instead, define `ExampleGroup#parent_groups`. (Myron
  Marston)
* Limit monkey patching of shared example/context declaration methods
  (`shared_examples_for`, etc.) to just the objects that need it rather than
  every object in the system (Myron Marston).
* Fix Metadata#fetch to support computed values (Sam Goldman).
* Named subject can now be referred to from within subject block in a nested
  group (tomykaira).
* Fix `fail_fast` so that it properly exits when an error occurs in a
  `before(:all) hook` (Bradley Schaefer).
* Make the order spec files are loaded consistent, regardless of the
  order of the files returned by the OS or the order passed at
  the command line (Jo Liss and Sam Phippen).
* Ensure instance variables from `before(:all)` are always exposed
  from `after(:all)`, even if an error occurs in `before(:all)`
  (Sam Phippen).
* `rspec --init` no longer generates an incorrect warning about `--configure`
  being deprecated (Sam Phippen).
* Fix pluralization of `1 seconds` (Odin Dutton)
* Fix ANSICON url (Jarmo Pertman)
* Use dup of Time so reporting isn't clobbered by examples that modify Time
  without properly restoring it. (David Chelimsky)

Deprecations

* `share_as` is no longer needed. `shared_context` and/or
  `RSpec::SharedContext` provide better mechanisms (Sam Phippen).
* Deprecate `RSpec.configuration` with a block (use `RSpec.configure`).


### 2.11.1 / 2012-07-18
[full changelog](http://github.com/rspec/rspec-core/compare/v2.11.0...v2.11.1)

Bug fixes

* Fix the way we autoload RSpec::Matchers so that custom matchers can be
  defined before rspec-core has been configured to definitely use
  rspec-expectations. (Myron Marston)
* Fix typo in --help message printed for -e option. (Jo Liss)
* Fix ruby warnings. (Myron Marston)
* Ignore mock expectation failures when the example has already failed.
  Mock expectation failures have always been ignored in this situation,
  but due to my changes in 27059bf1 it was printing a confusing message.
  (Myron Marston).

### 2.11.0 / 2012-07-07
[full changelog](http://github.com/rspec/rspec-core/compare/v2.10.1...v2.11.0)

Enhancements

* Support multiple `--example` options. (Daniel Doubrovkine @dblock)
* Named subject e.g. `subject(:article) { Article.new }`
    * see [http://blog.davidchelimsky.net/2012/05/13/spec-smell-explicit-use-of-subject/](http://blog.davidchelimsky.net/2012/05/13/spec-smell-explicit-use-of-subject/)
      for background.
    * thanks to Bradley Schaefer for suggesting it and Avdi Grimm for almost
      suggesting it.
* `config.mock_with` and `config.expect_with` yield custom config object to a
  block if given
    * aids decoupling from rspec-core's configuation
* `include_context` and `include_examples` support a block, which gets eval'd
  in the current context (vs the nested context generated by `it_behaves_like`).
* Add `config.order = 'random'` to the `spec_helper.rb` generated by `rspec
  --init`.
* Delay the loading of DRb (Myron Marston).
* Limit monkey patching of `describe` onto just the objects that need it rather
  than every object in the system (Myron Marston).

Bug fixes

* Support alternative path separators. For example, on Windows, you can now do
  this: `rspec spec\subdir`. (Jarmo Pertman @jarmo)
* When an example raises an error and an after or around hook does as
  well, print out the hook error. Previously, the error was silenced and
  the user got no feedback about what happened. (Myron Marston)
* `--require` and `-I` are merged among different configuration sources (Andy
  Lindeman)
* Delegate to mocha methods instead of aliasing them in mocha adapter.

### 2.10.1 / 2012-05-19
[full changelog](http://github.com/rspec/rspec-core/compare/v2.10.0...v2.10.1)

Bug fixes

* `RSpec.reset` properly reinits configuration and world
* Call `to_s` before `split` on exception messages that might not always be
  Strings (slyphon)

### 2.10.0 / 2012-05-03
[full changelog](http://github.com/rspec/rspec-core/compare/v2.9.0...v2.10.0)

Enhancements

* Add `prepend_before` and `append_after` hooks (preethiramdev)
    * intended for extension libs
    * restores rspec-1 behavior
* Reporting of profiled examples (moro)
    * Report the total amount of time taken for the top slowest examples.
    * Report what percentage the slowest examples took from the total runtime.

Bug fixes

* Properly parse `SPEC_OPTS` options.
* `example.description` returns the location of the example if there is no
  explicit description or matcher-generated description.
* RDoc fixes (Grzegorz Świrski)
* Do not modify example ancestry when dumping errors (Michael Grosser)

### 2.9.0 / 2012-03-17
[full changelog](http://github.com/rspec/rspec-core/compare/v2.8.0...v2.9.0)

Enhancements

* Support for "X minutes X seconds" spec run duration in formatter. (uzzz)
* Strip whitespace from group and example names in doc formatter.
* Removed spork-0.9 shim. If you're using spork-0.8.x, you'll need to upgrade
  to 0.9.0.

Bug fixes

* Restore `--full_backtrace` option
* Ensure that values passed to `config.filter_run` are respected when running
  over DRb (using spork).
* Ensure shared example groups are reset after a run (as example groups are).
* Remove `rescue false` from calls to filters represented as Procs
* Ensure `described_class` gets the closest constant (pyromaniac)
* In "autorun", don't run the specs in the `at_exit` hook if there was an
  exception (most likely due to a SyntaxError). (sunaku)
* Don't extend groups with modules already used to extend ancestor groups.
* `its` correctly memoizes nil or false values (Yamada Masaki)

### 2.8.0 / 2012-01-04

[full changelog](http://github.com/rspec/rspec-core/compare/v2.8.0.rc2...v2.8.0)

Bug fixes

* For metadata filtering, restore passing the entire array to the proc, rather
  than each item in the array (weidenfreak)
* Ensure each spec file is loaded only once
    * Fixes a bug that caused all the examples in a file to be run when
      referenced twice with line numbers in a command, e.g.
        * `rspec path/to/file:37 path/to/file:42`

### 2.8.0.rc2 / 2011-12-19

[full changelog](http://github.com/rspec/rspec-core/compare/v2.8.0.rc1...v2.8.0.rc2)

Enhancments

* new `--init` command (Peter Schröder)
    * generates `spec/spec_helper.rb`
    * deletes obsolete files (on confirmation)
    * merged with and deprecates `--configure` command, which generated
      `.rspec`
* use `require_relative` when available (Ian Leitch)
* `include_context` and `include_examples` accept params (Calvin Bascom)
* print the time for every example in the html formatter (Richie Vos)
* several tasty refactoring niblets (Sasha)
* `it "does something", :x => [:foo,'bar',/baz/] (Ivan Neverov)
    * supports matching n command line tag values with an example or group

### 2.8.0.rc1 / 2011-11-06

[full changelog](http://github.com/rspec/rspec-core/compare/v2.7.1...v2.8.0.rc1)

Enhancements

* `--order` (Justin Ko)
    * run examples in random order: `--order rand`
    * specify the seed: `--order rand:123`
* `--seed SEED`
    * equivalent of `--order rand:SEED`
* SharedContext supports `let` (David Chelimsky)
* Filter improvements (David Chelimsky)
    * override opposing tags from the command line
    * override RSpec.configure tags from the command line
    * `--line_number 37` overrides all other filters
    * `path/to/file.rb:37` overrides all other filters
    * refactor: consolidate filter management in a FilterManger object
* Eliminate Ruby warnings (Matijs van Zuijlen)
* Make reporter.report an API (David Chelimsky)
    * supports extension tools like interative_rspec

Changes

* change `config.color_enabled` (getter/setter/predicate) to `color` to align
  with `--[no]-color` CLI option.
    * `color_enabled` is still supported for now, but will likley be deprecated
      in a 2.x release so we can remove it in 3.0.

Bug fixes

* Make sure the `bar` in `--tag foo:bar` makes it to DRb (Aaron Gibralter)
* Fix bug where full descriptions of groups nested 3 deep  were repeated.
* Restore report of time to run to start after files are loaded.
    * fixes bug where run times were cumalitive in spork
    * fixes compatibility with time-series metrics
* Don't error out when `config.mock_with` or `expect_with` is re-specifying the
  current config (Myron Marston)

* Deprecations
    * :alias option on `configuration.add_setting`. Use `:alias_with` on the
      original setting declaration instead.

### 2.7.1 / 2011-10-20

[full changelog](http://github.com/rspec/rspec-core/compare/v2.7.0...v2.7.1)

Bug fixes

* tell autotest the correct place to find the rspec executable

### 2.7.0 / 2011-10-16

[full changelog](http://github.com/rspec/rspec-core/compare/v2.6.4...v2.7.0)

NOTE: RSpec's release policy dictates that there should not be any backward
incompatible changes in minor releases, but we're making an exception to
release a change to how RSpec interacts with other command line tools.

As of 2.7.0, you must explicity `require "rspec/autorun"` unless you use the
`rspec` command (which already does this for you).

Enhancements

* Add `example.exception` (David Chelimsky)
* `--default_path` command line option (Justin Ko)
* support multiple `--line_number` options (David J. Hamilton)
    * also supports `path/to/file.rb:5:9` (runs examples on lines 5 and 9)
* Allow classes/modules to be used as shared example group identifiers (Arthur
  Gunn)
* Friendly error message when shared context cannot be found (Sławosz
  Sławiński)
* Clear formatters when resetting config (John Bintz)
* Add `xspecify` and xexample as temp-pending methods (David Chelimsky)
* Add `--no-drb` option (Iain Hecker)
* Provide more accurate run time by registering start time before code is
  loaded (David Chelimsky)
    * reverted in 2.8.0
* Rake task default pattern finds specs in symlinked dirs (Kelly Felkins)
* Rake task no longer does anything to invoke bundler since Bundler already
  handles it for us. Thanks to Andre Arko for the tip.
* Add `--failure-exit-code` option (Chris Griego)

Bug fixes

* Include `Rake::DSL` to remove deprecation warnings in Rake > 0.8.7 (Pivotal
  Casebook)
* Only eval `let` block once even if it returns `nil` (Adam Meehan)
* Fix `--pattern` option (wasn't being recognized) (David Chelimsky)
* Only implicitly `require "rspec/autorun"` with the `rspec` command (David
  Chelimsky)
* Ensure that rspec's `at_exit` defines the exit code (Daniel Doubrovkine)
* Show the correct snippet in the HTML and TextMate formatters (Brian Faherty)

### 2.6.4 / 2011-06-06

[full changelog](http://github.com/rspec/rspec-core/compare/v2.6.3...v2.6.4)

NOTE: RSpec's release policy dictates that there should not be new
functionality in patch releases, but this minor enhancement slipped in by
accident.  As it doesn't add a new API, we decided to leave it in rather than
roll back this release.

Enhancements

* Add summary of commands to run individual failed examples.

Bug fixes

* Support exclusion filters in DRb. (Yann Lugrin)
* Fix --example escaping when run over DRb. (Elliot Winkler)
* Use standard ANSI codes for color formatting so colors work in a wider set of
  color schemes.

### 2.6.3 / 2011-05-24

[full changelog](http://github.com/rspec/rspec-core/compare/v2.6.2...v2.6.3)

Bug fixes

* Explicitly convert exit code to integer, avoiding TypeError when return
  value of run is IO object proxied by `DRb::DRbObject` (Julian Scheid)
* Clarify behavior of `--example` command line option
* Build using a rubygems-1.6.2 to avoid downstream yaml parsing error

### 2.6.2 / 2011-05-21

[full changelog](http://github.com/rspec/rspec-core/compare/v2.6.1...v2.6.2)

Bug fixes

* Warn rather than raise when HOME env var is not defined
* Properly merge command-line exclusions with default :if and :unless (joshcooper)

### 2.6.1 / 2011-05-19

[full changelog](http://github.com/rspec/rspec-core/compare/v2.6.0...v2.6.1)

Bug fixes

* Don't extend nil when filters are nil
* `require 'rspec/autorun'` when running rcov.

### 2.6.0 / 2011-05-12

[full changelog](http://github.com/rspec/rspec-core/compare/v2.5.1...v2.6.0)

Enhancements

* `shared_context` (Damian Nurzynski)
    * extend groups matching specific metadata with:
        * method definitions
        * subject declarations
        * let/let! declarations
        * etc (anything you can do in a group)
* `its([:key])` works for any subject with #[]. (Peter Jaros)
* `treat_symbols_as_metadata_keys_with_true_values` (Myron Marston)
* Print a deprecation warning when you configure RSpec after defining an
  example.  All configuration should happen before any examples are defined.
  (Myron Marston)
* Pass the exit status of a DRb run to the invoking process. This causes specs
  run via DRb to not just return true or false. (Ilkka Laukkanen)
* Refactoring of `ConfigurationOptions#parse_options` (Rodrigo Rosenfeld Rosas)
* Report excluded filters in runner output (tip from andyl)
* Clean up messages for filters/tags.
* Restore --pattern/-P command line option from rspec-1
* Support false as well as true in config.full_backtrace= (Andreas Tolf
  Tolfsen)

Bug fixes

* Don't stumble over an exception without a message (Hans Hasselberg)
* Remove non-ascii characters from comments that were choking rcov (Geoffrey
  Byers)
* Fixed backtrace so it doesn't include lines from before the autorun at_exit
  hook (Myron Marston)
* Include RSpec::Matchers when first example group is defined, rather than just
  before running the examples.  This works around an obscure bug in ruby 1.9
  that can cause infinite recursion. (Myron Marston)
* Don't send `example_group_[started|finished]` to formatters for empty groups.
* Get specs passing on jruby (Sidu Ponnappa)
* Fix bug where mixing nested groups and outer-level examples gave
  unpredictable :line_number behavior (Artur Małecki)
* Regexp.escape the argument to --example (tip from Elliot Winkler)
* Correctly pass/fail pending block with message expectations
* CommandLine returns exit status (0/1) instead of true/false
* Create path to formatter output file if it doesn't exist (marekj).


### 2.5.1 / 2011-02-06

[full changelog](http://github.com/rspec/rspec-core/compare/v2.5.0...v2.5.1)

NOTE: this release breaks compatibility with rspec/autotest/bundler
integration, but does so in order to greatly simplify it.

With this release, if you want the generated autotest command to include
'bundle exec', require Autotest's bundler plugin in a .autotest file in the
project's root directory or in your home directory:

    require "autotest/bundler"

Now you can just type 'autotest' on the commmand line and it will work as you expect.

If you don't want 'bundle exec', there is nothing you have to do.

### 2.5.0 / 2011-02-05

[full changelog](http://github.com/rspec/rspec-core/compare/v2.4.0...v2.5.0)

Enhancements

* Autotest::Rspec2 parses command line args passed to autotest after '--'
* --skip-bundler option for autotest command
* Autotest regexp fixes (Jon Rowe)
* Add filters to html and textmate formatters (Daniel Quimper)
* Explicit passing of block (need for JRuby 1.6) (John Firebaugh)

Bug fixes

* fix dom IDs in HTML formatter (Brian Faherty)
* fix bug with --drb + formatters when not running in drb
* include --tag options in drb args (monocle)
* fix regression so now SPEC_OPTS take precedence over CLI options again (Roman
  Chernyatchik)
* only call its(:attribute) once (failing example from Brian Dunn)
* fix bizarre bug where rspec would hang after String.alias :to_int :to_i
  (Damian Nurzynski)

Deprecations

* implicit inclusion of 'bundle exec' when Gemfile present (use autotest's
  bundler plugin instead)

### 2.4.0 / 2011-01-02

[full changelog](http://github.com/rspec/rspec-core/compare/v2.3.1...v2.4.0)

Enhancements

* start the debugger on -d so the stack trace is visible when it stops
  (Clifford Heath)
* apply hook filtering to examples as well as groups (Myron Marston)
* support multiple formatters, each with their own output
* show exception classes in failure messages unless they come from RSpec
  matchers or message expectations
* before(:all) { pending } sets all examples to pending

Bug fixes

* fix bug due to change in behavior of reject in Ruby 1.9.3-dev (Shota
  Fukumori)
* fix bug when running in jruby: be explicit about passing block to super (John
  Firebaugh)
* rake task doesn't choke on paths with quotes (Janmejay Singh)
* restore --options option from rspec-1
* require 'ostruct' to fix bug with its([key]) (Kim Burgestrand)
* --configure option generates .rspec file instead of autotest/discover.rb

### 2.3.1 / 2010-12-16

[full changelog](http://github.com/rspec/rspec-core/compare/v2.3.0...v2.3.1)

Bug fixes

* send debugger warning message to $stdout if RSpec.configuration.error_stream
  has not been defined yet.
* HTML Formatter _finally_ properly displays nested groups (Jarmo Pertman)
* eliminate some warnings when running RSpec's own suite (Jarmo Pertman)

### 2.3.0 / 2010-12-12

[full changelog](http://github.com/rspec/rspec-core/compare/v2.2.1...v2.3.0)

Enhancements

* tell autotest to use "rspec2" if it sees a .rspec file in the project's root
  directory
    * replaces the need for ./autotest/discover.rb, which will not work with
      all versions of ZenTest and/or autotest
* config.expect_with
    * :rspec          # => rspec/expectations
    * :stdlib         # => test/unit/assertions
    * :rspec, :stdlib # => both

Bug fixes

* fix dev Gemfile to work on non-mac-os machines (Lake Denman)
* ensure explicit subject is only eval'd once (Laszlo Bacsi)

### 2.2.1 / 2010-11-28

[full changelog](http://github.com/rspec/rspec-core/compare/v2.2.0...v2.2.1)

Bug fixes
* alias_method instead of override Kernel#method_missing (John Wilger)
* changed --autotest to --tty in generated command (MIKAMI Yoshiyuki)
* revert change to debugger (had introduced conflict with Rails)
    * also restored --debugger/-debug option

### 2.2.0 / 2010-11-28

[full changelog](http://github.com/rspec/rspec-core/compare/v2.1.0...v2.2.0)

Deprecations/changes

* --debug/-d on command line is deprecated and now has no effect
* win32console is now ignored; Windows users must use ANSICON for color support
  (Bosko Ivanisevic)

Enhancements

* When developing locally rspec-core now works with the rspec-dev setup or your
  local gems
* Raise exception with helpful message when rspec-1 is loaded alongside rspec-2
  (Justin Ko)
* debugger statements _just work_ as long as ruby-debug is installed
  * otherwise you get warned, but not fired
* Expose example.metadata in around hooks
* Performance improvments (much faster now)

Bug fixes

* Make sure --fail-fast makes it across drb
* Pass -Ilib:spec to rcov

### 2.1.0 / 2010-11-07

[full changelog](http://github.com/rspec/rspec-core/compare/v2.0.1...v2.1.0)

Enhancments

* Add skip_bundler option to rake task to tell rake task to ignore the presence
  of a Gemfile (jfelchner)
* Add gemfile option to rake task to tell rake task what Gemfile to look for
  (defaults to 'Gemfile')
* Allow passing caller trace into Metadata to support extensions (Glenn
  Vanderburg)
* Add deprecation warning for Spec::Runner.configure to aid upgrade from
  RSpec-1
* Add deprecated Spec::Rake::SpecTask to aid upgrade from RSpec-1
* Add 'autospec' command with helpful message to aid upgrade from RSpec-1
* Add support for filtering with tags on CLI (Lailson Bandeira)
* Add a helpful message about RUBYOPT when require fails in bin/rspec (slyphon)
* Add "-Ilib" to the default rcov options (Tianyi Cui)
* Make the expectation framework configurable (default rspec, of course)
  (Justin Ko)
* Add 'pending' to be conditional (Myron Marston)
* Add explicit support for :if and :unless as metadata keys for conditional run
  of examples (Myron Marston)
* Add --fail-fast command line option (Jeff Kreeftmeijer)

Bug fixes

* Eliminate stack overflow with "subject { self }"
* Require 'rspec/core' in the Raketask (ensures it required when running rcov)

### 2.0.1 / 2010-10-18

[full changelog](http://github.com/rspec/rspec-core/compare/v2.0.0...v2.0.1)

Bug fixes

* Restore color when using spork + autotest
* Pending examples without docstrings render the correct message (Josep M.
  Bach)
* Fixed bug where a failure in a spec file ending in anything but _spec.rb
  would fail in a confusing way.
* Support backtrace lines from erb templates in html formatter (Alex Crichton)

### 2.0.0 / 2010-10-10

[full changelog](http://github.com/rspec/rspec-core/compare/v2.0.0.rc...v2.0.0)

RSpec-1 compatibility

* Rake task uses ENV["SPEC"] as file list if present

Bug fixes

* Bug Fix: optparse --out foo.txt (Leonardo Bessa)
* Suppress color codes for non-tty output (except autotest)

### 2.0.0.rc / 2010-10-05

[full changelog](http://github.com/rspec/rspec-core/compare/v2.0.0.beta.22...v2.0.0.rc)

Enhancements

* implicitly require unknown formatters so you don't have to require the file
  explicitly on the commmand line (Michael Grosser)
* add --out/-o option to assign output target
* added fail_fast configuration option to abort on first failure
* support a Hash subject (its([:key]) { should == value }) (Josep M. Bach)

Bug fixes

* Explicitly require rspec version to fix broken rdoc task (Hans de Graaff)
* Ignore backtrace lines that come from other languages, like Java or
  Javascript (Charles Lowell)
* Rake task now does what is expected when setting (or not setting)
  fail_on_error and verbose
* Fix bug in which before/after(:all) hooks were running on excluded nested
  groups (Myron Marston)
* Fix before(:all) error handling so that it fails examples in nested groups,
  too (Myron Marston)

### 2.0.0.beta.22 / 2010-09-12

[full changelog](http://github.com/rspec/rspec-core/compare/v2.0.0.beta.20...v2.0.0.beta.22)

Enhancements

* removed at_exit hook
* CTRL-C stops the run (almost) immediately
    * first it cleans things up by running the appropriate after(:all) and
      after(:suite) hooks
    * then it reports on any examples that have already run
* cleaned up rake task
    * generate correct task under variety of conditions
    * options are more consistent
    * deprecated redundant options
* run 'bundle exec autotest' when Gemfile is present
* support ERB in .rspec options files (Justin Ko)
* depend on bundler for development tasks (Myron Marston)
* add example_group_finished to formatters and reporter (Roman Chernyatchik)

Bug fixes

* support paths with spaces when using autotest (Andreas Neuhaus)
* fix module_exec with ruby 1.8.6 (Myron Marston)
* remove context method from top-level
    * was conflicting with irb, for example
* errors in before(:all) are now reported correctly (Chad Humphries)

Removals

* removed -o --options-file command line option
    * use ./.rspec and ~/.rspec
