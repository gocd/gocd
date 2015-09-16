## 2.0.3 2014-02-20

* minor fix for time_to_js when scope not set (#27)
* Jar path now avoids using a glob, which doesn't work inside a jar (#26)
* use Rhino.warn explicitly otherwise it's a Kernel.warn

## 2.0.2 2012-12-05

* handle Rhino's 64K code generation (method) limit on the fly (#23)
* correct explicit Ruby equality == and eql? (JRuby 1.7.1 compat)
* allow to set javascript version programatically - globally also allow
  reading it from system properties by default
* allow to set optimization level globally also allow reading it from
  system properties by default
* make sure Ruby function wrapper has (Ruby) #call semantics just like
  JavaScript functions exposed into the Ruby side
* function's return value should be converted to Ruby (Ruby #call style)

## 2.0.1 2012-08-24

* JSError improvement to preserve nested Ruby error message
* jar-1.7.4 regression fix e.g when loading less (#25)
* error.message should be a String value (1.9.3 compat)

## jar-1.7.4 2012-08-02

* updated to new Mozilla Rhino 1.7R4 release, notes:
  https://developer.mozilla.org/en/New_in_Rhino_1.7R4

## 2.0.0 2012-08-02

This release is functionally the same as therubyrhino-1.73.5 as long
as the therubyrhino_jar-1.7.3 gem dependency is used along with it.

* new versioning scheme - old scheme used for the jar gem
* moved out the rhino.jar into a separate therubyrhino_jar gem

## jar-1.7.3 2012-08-02

* therubyrhino_jar gem packaged with Mozilla Rhino 1.7R3


## 1.73.5 2012-07-25

* #to_s functionName typo fix for org.mozilla.javascript.ScriptStackElement
* make sure thrown values are correctly raised from inside JS functions
* a better #inspect for native rhino objects
* correct JavaScript error handling for Function#apply

## 1.73.4 2012-05-21

* allow rhino.jar path overrides with Rhino::JAR_PATH
* 'correct' JSError#inspect - show thrown value
* fix JSError#javascript_backtrace to be an array and add it on top of the
  (ruby) backtrace
* make sure JSError#cause always points to native rhino cause (#19)
* avoid using instance variables with 'native' JS::Context (JRuby 1.7 warnings)

## 1.73.3 2012-04-23

RedJS 0.4 compatible

* allow try-catch-ing ScriptError (besides StandardError) in JS
* support for yield in JS property access via the [], []= methods
* refactor access implementations to classes + introduce a shared base
* missing explicit require 'rhino/version'
* Ruby StandardError wrapping so they can be try-catched as JS "error" values
* Rhino::Context.new is expected to yield when block passed

## 1.73.2 2012-04-11

RedJS 0.2.1 compatible

* improve JSError#message + add JSError#value to reflect throw JS value
* correctly convert hashes nested within arrays to_javascript (#12)
* full jruby --1.9 compatibility
* Context.default_factory - no longer use a new factory per context
* restrictable limits now require Contex.new(:restrictable => true)
* added Context#timeout_limit (to complete instruction_limit)

## 1.73.1 2011-11-28

NOTE: this is a "major" code update from 1.73.0 with some incompatibilities
although keeping the bits backward compatible as much as possible :

* add a JS:Function#apply to be used for calling functions from Ruby
* add a JS:Function#bind usable with JS functions from Ruby
* JS:Function#call should work similar to Method/Proc#call
* customizable scriptable access module for resolving Ruby properties from JS
  with TRR compatible Ruby::DefaultAccess and a custom Ruby::AttributeAccess
* implement JavaScript function style argument filling/slicing (for Ruby)
* delegate to hash like method []/[]= when wrapped subject supports them
* introduce Ruby::Constructor JS wrapper for Ruby classes
* hande int JavaScript property resolution
* make sure Time -> Date conversion happens as well
* deprecate JavascriptError use JSError instead with a javascript_backtrace fix
* avoid using Rhino::To.javascript instead use Rhino.to_javascript etc.
* NativeObject/NativeFunction got removed to avoid wrapping - instead Rhino's
  "native" Java classes are customized using JRuby's Java integration
* support for setting JS version via Context.version
* Rhino::J gets deprecated it's now know as Rhino::JS

## 1.73.0 2011-11-28

* upgrade to rhino-1.7R3
* cache objects passed to context - same object passed twice ends up the same
* properly map ruby Time objects to javascript Date
* jruby --1.9 improvements

## 1.72.8 2011-06-26

* fix passing of options hash to ruby.

## 1.72.6 2009-11-30

* 2 major enhancements:
  * evaluate an IO object such as a file or an socket input stream
  * load javascript sources directly into the context with the file system

## 1.72.5 2009-11-12

* 2 major enhancements:
  * evaluate javascript with a ruby object as it's scope using Context#open(:with => object)
  * add eval_js() method to Object to evaluate in the context of that object

## 1.72.4 2009-11-12

* 3 major enhancements:
  * automatically wrap/unwrap ruby and javascript arrays
  * automatically convert ruby method objects and Proc objects into javascript functions
  * Make functions defined in javascript callable from ruby

## 1.72.3 2009-11-11

* 4 major enhancements:
  * greatly simplified interface to context by unifying context and scope
  * remove Context#open_std()
  * remove Context#standard
  * remove Context#init_standard_objects

## 1.72.2 2009-11-10

* 1 major enhancement:
  * ability to limit the instruction count for a context

## 1.72.1 2009-11-09

* 4 major enhancements:
  * easily manipulate javascript objects from ruby (NativeObject)
  * make NativeObject Enumerable
  * to_h and to_json for NativeObject
  * embed ruby instances and call methods from javascript

## 1.72.0 2009-09-24

* 2 major enhancements:
  * evaluate javascript in jruby
  * embed callable ruby objects in javascript
