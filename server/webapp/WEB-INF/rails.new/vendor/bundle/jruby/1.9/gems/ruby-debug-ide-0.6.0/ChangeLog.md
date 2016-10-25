## [master](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.5.0...master)

* "file-filter on|off" command added
* "include file|dir" command added
* "exclude file|dir" command added

## [0.5.0](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.33...v0.5.0)

* catchpointDeleted event added (under --catchpoint-deleted-event flag)
* --value-as-nested-element to enable just this the extension

## [0.4.33](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.32...v0.4.33)

* Fixed problem with inspecting Jbuilder
  [RUBY-16838](https://youtrack.jetbrains.com/issue/RUBY-16838)

## [0.4.32](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.31...v0.4.32)

* Fixed problem with preloading psych
  [RUBY-16721](https://youtrack.jetbrains.com/issue/RUBY-16721)

## [0.4.31](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.30...v0.4.31)

* need to handle mock objects somehow
  [RUBY-16665](https://youtrack.jetbrains.com/issue/RUBY-16665)

## [0.4.30](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.29...v0.4.30)

* reverting fix for
  [RUBY-16192](https://youtrack.jetbrains.com/issue/RUBY-16192) to resolve
  [RUBY-16435](https://youtrack.jetbrains.com/issue/RUBY-16435)

* unescaping of empty line fixed
  [RUBY-16600](https://youtrack.jetbrains.com/issue/RUBY-16600)

## [0.4.29](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.28...v0.4.29)

* Fixed problem with evaluating "%"
  [RUBY-16244](https://youtrack.jetbrains.com/issue/RUBY-16244)

## [0.4.28](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.27...v0.4.28)

* [better handling for escaped chars and slashes](https://github.com/ruby-debug/ruby-debug-ide/pull/68)

## [0.4.27](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.26...v0.4.27)

* Redundant quotes dropped from string variable representation
  [RUBY-16275](https://youtrack.jetbrains.com/issue/RUBY-16275)

## [0.4.26](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.25...v0.4.26)

* Compact value for inline debugger should be really compact
  [RUBY-15932](https://youtrack.jetbrains.com/issue/RUBY-15932)

## [0.4.25](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.24...v0.4.25)

* Let's use String#inspect in print variable for String variables
  [RUBY-16192](https://youtrack.jetbrains.com/issue/RUBY-16192)

## [0.4.24](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.24.beta5...v0.4.24)

* time to release

## [0.4.24.beta5](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.24.beta4...v0.4.24.beta5)

* do not print empty value attr in case RubyMine-specific protocol extensions are enabled

## [0.4.24.beta4](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.23...v0.4.24.beta4)

* Performance optimisation for variable representation [RUBY-16055](https://youtrack.jetbrains.com/issue/RUBY-16055)
* Added command line argument to enable RubyMine-specific protocol extensions
* Several fixes to make debugger more robust [RUBY-16070](https://youtrack.jetbrains.com/issue/RUBY-16070)

## [0.4.23](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.23.beta11...v0.4.23)

* fixed problem with compact name for binary params (strings with invalid encoding)
  [RUBY-15960](https://youtrack.jetbrains.com/issue/RUBY-15960)

## [0.4.23.beta11](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.23.beta10...v0.4.23.beta11)

* adding breakpoint in non-existing file should not break debugger
  [RUBY-15873](https://youtrack.jetbrains.com/issue/RUBY-15873)

## [0.4.23.beta10](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.23.beta9...v0.4.23.beta10)

* fixed problem with printing hashes [RUBY-15804](https://youtrack.jetbrains.com/issue/RUBY-15804)

## [0.4.23.beta9](https://github.com/ruby-debug/ruby-debug-ide/compare/v0.4.23.beta8...v0.4.23.beta9)

* problem with calculating local variables for 1.8 fixed

## "per-historical" changes 
Dennis Ushakov <dennis.ushakov@gmail.com>
  * run context commands on stopped thread to prevent segfaults (3c1b52d5091fccec447d5695d5b43e73f335cc54)
  * enable building without deps (9b597f8ce2b97ed40bb57e55ad178cf8ce270fa9)
  * improved error handling (220d78b8e2be52f8a3b21f34c6c88283b22440d4)
  * show full exception info when failed to evaluate expression (8f4ec5f16dd659e0cada7ff2a218e8768ff87b35)
  * fix config (8440dc7bb51c934f99887ff43cdf805a16664159)
  * edited dependency for 1.9, allow prerelease (14ffae2dd364ad445377b358870c35eabdf9567c)
  * fix incompatibilities with command-line debug (474d3607222a0c9fc78917e7b38051d37b11b75a)
  * fix installation issues with prerelease base gem versions (12fc83ce1f389c1feaaa3899dd716b670909687a)
  * added synchronization between command processors threads (175f2ef890d23d490e04265949853b4202db1860)
  * fixed debug output (0e9dd25a8cdde9195ef2060678fc6272270762f8)
  * fix 1.9 support (725f14095c67c02be8b4f9b8ef2706de70cfa220)
  * just a version bump, because i forgot to push fix for 1.9 (be7c60c446c293d522958fd9c6136dc21ece6dba)
  * fix incompatibilities with command-line debug (37c98589bf1b8d43521bfa71fa75a587130c8494)
  * fix incompatibilities with command-line debug (749ba472d5bd44c1488a7a310fa51712426ab972)
  * fix incompatibilities with command-line debug, one more round (fa3b98788250d028ecac9db3828b14b6df307042)

2009-12-22 14:28  Martin Krauskopf

	* Rakefile, doc/protocol-spec.texi, lib/ruby-debug-ide.rb,
	  test/rd_test_base.rb: Increasing version; updating changes

2009-12-22 14:23  Martin Krauskopf

	* lib/ruby-debug/commands/variables.rb: Fix for possible
	  NoSuchMethodException (by Oleg Shpynov)

2009-09-09 12:24  Martin Krauskopf

	* ChangeLog, Rakefile, ext/mkrf_conf.rb, lib/ruby-debug-ide.rb,
	  test/rd_test_base.rb: Do not try to install native extension for
	  JRuby.

2009-08-31 12:00  Martin Krauskopf

	* ChangeLog: ChangeLog update

2009-08-31 11:59  Martin Krauskopf

	* Rakefile, bin/rdebug-ide, ext/extconf.rb, ext/mkrf_conf.rb,
	  lib/ruby-debug-ide.rb, lib/ruby-debug.rb, test/rd_test_base.rb:
	  1)fixed bug #27009, 2)rename to ruby-debug-ide.rb (both by Mark
	  Moseley)
	  
	  - to not collide with lib/ruby-debug.rb from ruby-debug-cli

2009-08-26 19:06  Martin Krauskopf

	* ChangeLog, Rakefile, bin/rdebug-ide, ext, ext/extconf.rb,
	  lib/ruby-debug.rb, lib/ruby-debug/command.rb,
	  lib/ruby-debug/event_processor.rb, lib/ruby-debug/processor.rb,
	  lib/ruby-debug/xml_printer.rb: Patch by Mark Moseley supporting
	  ruby-debug-base19.
	  
	  Dynamically installs right ruby-debug-base dependency depending
	  on the version
	  of a Ruby platform being used. ruby-debug-base19 is the only
	  solution these
	  days for 1.9 debugging, so might be temporary solution until
	  ruby-debug
	  projects brings official version.

2009-08-26 17:36  Martin Krauskopf

	* lib/ruby-debug.rb: One more try for the right value for default
	  'host' value.
	  
	  127.0.0.1 seemingly works with all systems and with IPv6 as well.
	  "localhost" and nil on have problems on some systems.

2009-08-21 08:43  Martin Krauskopf

	* lib/ruby-debug.rb: Ruby 1.9.x vs Java vs 'localhost' seems to be
	  problematic as well

2009-06-04 16:02  Martin Krauskopf

	* Rakefile, lib/ruby-debug.rb, test/rd_test_base.rb: Increasing
	  trung version

2009-06-04 15:59  Martin Krauskopf

	* ChangeLog, bin/rdebug-ide: Mentioning RubyMine among
	  ruby-debug-ide clients.

2009-05-21 18:16  Martin Krauskopf

	* ChangeLog: Changelog update

2009-05-21 18:01  Martin Krauskopf

	* Rakefile, doc/protocol-spec.texi, lib/ruby-debug.rb,
	  test/rd_test_base.rb: Added Debugger::start_server (ticket
	  #25972), patch by Tim Hanson

2009-03-12 11:38  Martin Krauskopf

	* ChangeLog, Rakefile, doc/protocol-spec.texi, lib/ruby-debug.rb,
	  test/rd_test_base.rb: Oops. 0.4.5 was not released yet, so it is
	  the rigth version, not 0.4.6, reverting.

2009-03-12 11:31  Martin Krauskopf

	* Rakefile, doc/protocol-spec.texi, lib/ruby-debug.rb,
	  lib/ruby-debug/commands/catchpoint.rb, test/rd_test_base.rb:
	  Possibility to remove catchpoints (patch by Chris Williams)

2009-02-03 09:34  Martin Krauskopf

	* Rakefile, bin/rdebug-ide, doc/protocol-spec.texi,
	  lib/ruby-debug.rb, test/rd_test_base.rb: 1) bugfix: syntax error
	  with Ruby 1.9 (patch by Mikael Rudberg) 2) 0.4.5 territory

2009-01-14 07:19  Martin Krauskopf

	* doc/protocol-spec.texi: completeng missing changes

2009-01-13 10:00  Martin Krauskopf

	* Rakefile, doc/protocol-spec.texi, lib/ruby-debug.rb,
	  test/rd_test_base.rb: 1) bugfix: print out backtrace when
	  debuggee fails. 2) version to 0.4.4

2009-01-13 09:54  Martin Krauskopf

	* Rakefile: Release appropriate files like license, tests, changes,
	  ... withing the gem.

2008-12-19 08:23  Martin Krauskopf

	* Rakefile, lib/ruby-debug.rb, test/rd_test_base.rb: Depends on the
	  "~> 0.10.3.x", rather then on 0.10.3 exactly. Increasing version
	  to 0.4.3.

2008-12-19 08:20  Martin Krauskopf

	* doc/protocol-spec.texi: typo

2008-11-24 12:59  Martin Krauskopf

	* doc/protocol-spec.texi: Changes between 0.3.1 and 0.3.2

2008-11-21 10:56  Martin Krauskopf

	* ChangeLog, Rakefile, doc/protocol-spec.texi,
	  etc/build_and_install.sh, lib/ruby-debug.rb,
	  test/rd_test_base.rb: Dependency changed to
	  ruby-debug-base-0.10.3 which fixes various bugs and contains
	  bunch of RFEs

2008-11-10 09:22  Martin Krauskopf

	* ChangeLog, doc/protocol-spec.texi: Changes for 0.4.1

2008-11-10 08:15  Martin Krauskopf

	* bin/rdebug-ide, lib/ruby-debug.rb, lib/ruby-debug/xml_printer.rb:
	  Making '-x' switch actually work. Commenting out sending <trace>
	  elements to the debugger. To be decided. There are large amount
	  of such events. For now servers rather for developers.

2008-11-09 23:39  Martin Krauskopf

	* bin/rdebug-ide, lib/ruby-debug.rb: --stop switch: stop at the
	  first line when the script is loaded. Utilized by remote
	  debugging.
	  s/Debugger#main/Debugger#debug_program (similarly to ruby-debug)

2008-11-09 00:28  Martin Krauskopf

	* lib/ruby-debug/commands/eval.rb,
	  lib/ruby-debug/commands/frame.rb, lib/ruby-debug/xml_printer.rb:
	  Fixing CLI verbose when -d is used. Some usused code

2008-11-08 18:43  Martin Krauskopf

	* doc/protocol-spec.texi, lib/ruby-debug/xml_printer.rb,
	  test/ruby-debug/xml_printer_test.rb: Ensure suspension's file
	  attribute contains absolute path.

2008-11-08 18:33  Martin Krauskopf

	* Rakefile, doc/protocol-spec.texi, etc/build_and_install.sh,
	  lib/ruby-debug.rb, lib/ruby-debug/xml_printer.rb,
	  nbproject/project.properties, test/rd_test_base.rb,
	  test/ruby-debug/xml_printer_test.rb: Ensure frame file attribute
	  contains absolute path. Increasing trunk version to 0.4.1

2008-11-02 11:19  Martin Krauskopf

	* doc/protocol-spec.texi: noting <message> element/event

2008-10-30 02:14  Martin Krauskopf

	* Rakefile, bin/rdebug-ide, doc/protocol-spec.texi,
	  etc/build_and_install.sh, lib/ruby-debug.rb,
	  lib/ruby-debug/xml_printer.rb, test/rd_test_base.rb,
	  test/ruby-debug, test/ruby-debug/xml_printer_test.rb: New
	  --xml-debug switch. To be used by frontend to not mess up
	  debuggee output with debugger's debug messages.
	  
	  - increasing version to 0.4.0 (new <message debug='true'...>
	  attribute)
	  - more robust failures handling in DebugThread
	  - some XmlPrinterTest sanity tests

2008-10-13 13:48  Martin Krauskopf

	* doc/protocol-spec.texi: Mentioning 0.3.1 changes

2008-10-09 06:48  Martin Krauskopf

	* Rakefile, bin/rdebug-ide, etc/build_and_install.sh,
	  lib/ruby-debug.rb, test/rd_test_base.rb: Support for
	  '--load-mode' option. Hotfix, likely workaround, for GlassFish
	  debugging. Experimental, might be removed in the future.
	  
	  If option is used, it calls Debugger#debug_load with
	  increment_start=true
	  Increasing version to 0.3.1

2008-09-08 15:17  Martin Krauskopf

	* ChangeLog: ChangeLog update before 0.3.0 release

2008-09-05 11:52  Martin Krauskopf

	* Rakefile, doc/protocol-spec.texi, etc/build_and_install.sh,
	  lib/ruby-debug.rb, lib/ruby-debug/commands/catchpoint.rb,
	  lib/ruby-debug/xml_printer.rb, test-base/readers.rb,
	  test-base/test_base.rb, test/rd_catchpoint_test.rb,
	  test/rd_test_base.rb: Setting catchpoint now answers with
	  <conditionSet> instead of just <message> providing better control
	  at frontend side.
	  Increasing version to 0.3.0 (incompatible protocol change)

2008-09-03 14:14  Martin Krauskopf

	* lib/ruby-debug/processor.rb: Do not swallow exceptions

2008-09-02 18:24  Martin Krauskopf

	* test-base/test_base.rb: Be sure socket is flushed during tests

2008-09-02 13:55  Martin Krauskopf

	* test-base/stepping_breakpoints_test.rb: ruby-debug-base fixes
	  stepping behaviour. Re-enabling common tests again

2008-09-02 13:54  Martin Krauskopf

	* test-base/test_base.rb, test/rd_test_base.rb: Do not hardcode
	  port for test server. Find always free one to prevent batch test
	  fails.

2008-09-02 13:49  Martin Krauskopf

	* nbproject/project.properties, nbproject/project.xml: Tweaking
	  NetBeans project metadata to include bin directory as a source
	  root

2008-09-02 13:48  Martin Krauskopf

	* Rakefile: Switching trunk dependency on recently released
	  ruby-debug-base 0.10.2

2008-08-12 16:10  Martin Krauskopf

	* ChangeLog, Rakefile, doc/protocol-spec.texi,
	  etc/build_and_install.sh, lib/ruby-debug.rb: Switching trunk to
	  0.2.2

2008-08-12 16:00  Martin Krauskopf

	* lib/ruby-debug/xml_printer.rb, test-base/variables_test.rb:
	  debugging is terminated when object's to_s method raises an
	  exception

2008-07-31 15:10  Martin Krauskopf

	* nbproject: ignore private folder

2008-07-31 15:09  Martin Krauskopf

	* nbproject, nbproject/project.properties, nbproject/project.xml:
	  Adding NetBeans project metadata.

2008-07-31 13:39  Martin Krauskopf

	* doc/protocol-spec.texi: - pushing changes to website
	  - fixing typo s/workardouning/workarounding

2008-07-31 13:23  Martin Krauskopf

	* ChangeLog: ChangeLog update before tagging 0.2.1

2008-07-31 13:20  Martin Krauskopf

	* doc/protocol-spec.texi: Updating Changes between 0.2.0 and 0.2.1

2008-07-31 13:13  Martin Krauskopf

	* test-base/stepping_breakpoints_test.rb: tweaking tests for bugs
	  in MRI to catch it once it is fixed

2008-07-31 13:12  Martin Krauskopf

	* Rakefile, lib/ruby-debug.rb: Hotfixing problem with running Ruby
	  on Mac. See 'Debugger timing out' thread:
	  http://ruby.netbeans.org/servlets/BrowseList?list=users&by=thread&from=861334
	  Still continuing on 0.2.x row (changing dependeny on
	  ruby-debug-base back to 0.10.1)

2008-07-14 22:40  Martin Krauskopf

	* Rakefile: Make trunk dependent on ruby-debug-base 0.10.2 which
	  fixed important "finish" bug

2008-06-22 07:10  Martin Krauskopf

	* CHANGES, Rakefile, etc/build_and_install.sh, lib/ruby-debug.rb:
	  increasing trunk version to 0.2.1
	  - CHANGES no longer maintained

2008-06-22 06:58  Martin Krauskopf

	* ChangeLog: ChangeLog update before tagging 0.2.0

2008-06-17 12:40  Martin Krauskopf

	* test/rd_test_base.rb: Fixing test loadpath

2008-06-17 11:52  Martin Krauskopf

	* test-base/variables_test.rb: New version of Rubies (Ruby 1.8.7
	  and JRuby 1.1.2) handle stack differently

2008-06-17 09:20  Martin Krauskopf

	* lib/ruby-debug.rb: s/Debugger::VERSION/0.2.0; former one points
	  to ruby-debug-base version

2008-06-12 06:41  Martin Krauskopf

	* CHANGES, Rakefile, doc/protocol-spec.texi,
	  etc/build_and_install.sh: New version will be 0.2.0, since there
	  is slight incompatible change (see spec)

2008-05-23 12:48  Martin Krauskopf

	* doc/protocol-spec.texi: - documenting catchpoitns
	  - mentioning "catch off" in uncompatible changes

2008-05-21 14:01  Martin Krauskopf

	* ChangeLog, test/rd_catchpoint_test.rb: Adding test for catchpoint

2008-05-21 13:32  Martin Krauskopf

	* doc/protocol-spec.texi: s/0.1.1/trunk (not sure whether it will
	  be 0.1.11 or 0.2.0)
	  - since tag for enable/disable command

2008-05-21 13:22  Martin Krauskopf

	* doc/protocol-spec.texi: - enabled/disable breakpoint feature
	  - typo in delete command

2008-05-21 13:21  Martin Krauskopf

	* lib/ruby-debug/command.rb, lib/ruby-debug/commands/enable.rb,
	  lib/ruby-debug/helper.rb, lib/ruby-debug/xml_printer.rb,
	  test-base/readers.rb, test-base/test_base.rb,
	  test/rd_enable_disable_test.rb: Enabled/Disable breakpoint
	  feature

2008-05-21 12:39  Martin Krauskopf

	* doc/protocol-spec.texi: Deleting Breakpoint

2008-05-17 21:06  Martin Krauskopf

	* doc/protocol-spec.texi: - changes section
	  - continuing with protocol description

2008-05-17 21:04  Martin Krauskopf

	* lib/ruby-debug/commands/catchpoint.rb, test-base/inspect_test.rb,
	  test-base/readers.rb, test-base/test_base.rb: Tweaking
	  catchpoint.rb and addng basic test

2008-05-15 20:59  Martin Krauskopf

	* lib/ruby-debug/event_processor.rb: I've got the deadlock
	  suggesting that Debugger::DebuggerThread instance was being
	  traced from tests. Putting 'assertion' forbidding it

2008-05-15 20:56  Martin Krauskopf

	* lib/ruby-debug.rb: Sanity check whether control thread starts up
	  (#20121)

2008-05-15 14:13  Martin Krauskopf

	* doc/protocol-spec.texi: - fixing version
	  - RDEBUG_IDE variable

2008-05-15 13:52  Martin Krauskopf

	* doc/protocol-spec.texi: Fixing 'cond' section

2008-05-15 10:58  Martin Krauskopf

	* lib/ruby-debug/event_processor.rb: Preventing possible race
	  condition between a program thread and control thread

2008-05-14 21:16  Martin Krauskopf

	* lib/ruby-debug/processor.rb: State and ControlState does not need
	  to keep commands' classes
	  (they are used in CLI debugger from 'help' command)

2008-05-13 18:58  Martin Krauskopf

	* doc, doc/protocol-spec.texi: Some works on ruby-debug-ide
	  protocol specification, far from complete

2008-05-13 18:04  Martin Krauskopf

	* lib/ruby-debug/command.rb, lib/ruby-debug/commands/condition.rb,
	  lib/ruby-debug/xml_printer.rb, test-base/condition_test.rb,
	  test-base/readers.rb, test-base/test_base.rb,
	  test/rd_condition_test.rb: Support for setting condition on
	  breakpoints
	  - condition.rb copy-pasted (then tweaked) from ruby-debug until
	  we find better way

2008-05-09 14:56  Martin Krauskopf

	* lib/ruby-debug.rb, lib/ruby-debug/event_processor.rb,
	  test-base/stepping_breakpoints_test.rb,
	  test-base/threads_and_frames_test.rb: - implementing
	  Context#at_return to make 'finish' work with MRI (not used by
	  JRuby yet)
	  - tweaking test for different behaviours of interpreters

2008-05-08 21:40  Martin Krauskopf

	* test-base/threads_and_frames_test.rb: Adding test for 'finish'.
	  Currently does not work with MRI:
	  ruby-debug issue: [#20039] Finish does not work correctly

2008-05-08 16:49  Martin Krauskopf

	* Rakefile: - switching trunk to ruby-debug-base(s) 0.10.1
	  - remove README to pass the 'gem' task. So far we have no any
	  README

2008-05-08 12:55  Martin Krauskopf

	* ., ChangeLog, ChangeLog.archive, svn2cl_usermap: - archiving
	  before-division ChangeLogs
	  - ignore pkg and private configuration

2008-05-08 12:17  Martin Krauskopf

	* ., CHANGES, ChangeLog, MIT-LICENSE, Rakefile, bin,
	  bin/rdebug-ide, config.yaml, etc, etc/build_and_install.sh,
	  etc/kill_debugger, lib, lib/ruby-debug, lib/ruby-debug.rb,
	  lib/ruby-debug/command.rb, lib/ruby-debug/commands,
	  lib/ruby-debug/commands/breakpoints.rb,
	  lib/ruby-debug/commands/catchpoint.rb,
	  lib/ruby-debug/commands/control.rb,
	  lib/ruby-debug/commands/eval.rb,
	  lib/ruby-debug/commands/frame.rb,
	  lib/ruby-debug/commands/inspect.rb,
	  lib/ruby-debug/commands/load.rb,
	  lib/ruby-debug/commands/stepping.rb,
	  lib/ruby-debug/commands/threads.rb,
	  lib/ruby-debug/commands/variables.rb,
	  lib/ruby-debug/event_processor.rb, lib/ruby-debug/helper.rb,
	  lib/ruby-debug/interface.rb, lib/ruby-debug/printers.rb,
	  lib/ruby-debug/processor.rb, lib/ruby-debug/xml_printer.rb, test,
	  test-base, test-base/basic_test.rb, test-base/inspect_test.rb,
	  test-base/readers.rb, test-base/stepping_breakpoints_test.rb,
	  test-base/test_base.rb, test-base/threads_and_frames_test.rb,
	  test-base/variables_test.rb, test/rd_basic_test.rb,
	  test/rd_inspect_test.rb, test/rd_stepping_breakpoints_test.rb,
	  test/rd_test_base.rb, test/rd_threads_and_frames_test.rb,
	  test/rd_variables_test.rb: Dividing 'debug-commons' into two
	  projects:
	  - ruby-debug-ide: wrapper for ruby-debug-base
	  - classic-debug-ide: wrapper for classic debug.rb
	  to make further development more smooth. classic-debug-ide is
	  getting
	  deprecated by availability of ruby-debug-base for MRI, JRuby and
	  Rubinius

