# therubyrhino

Embed the Mozilla Rhino JavaScript interpreter into Ruby

## REQUIREMENTS:

* JRuby >= 1.6.8

## INSTALL:

`jruby -S gem install therubyrhino`

## FEATURES/PROBLEMS:

* Evaluate JavaScript from with in Ruby
* Embed your Ruby objects into the JavaScript world

## SYNOPSIS:

1. JavaScript goes into Ruby
2. Ruby Objects goes into JavaScript
3. Our shark's in the JavaScript!

```ruby
require 'rhino'
```

* evaluate some simple JavaScript using `eval_js`:
```ruby
eval_js "7 * 6" #=> 42
```

* that's quick and dirty, but if you want more control over your environment,
  use a `Context`:
```ruby
Rhino::Context.open do |context|
  context['foo'] = "bar"
  context.eval('foo') # => "bar"
end
```

* evaluate a Ruby function from JavaScript:
```ruby
Rhino::Context.open do |context|
  context["say"] = lambda {|word, times| word * times}
  context.eval("say("Hello", 3)") #=> HelloHelloHello
end
```

* embed a Ruby object into your JavaScript environment
```ruby
class MyMath
  def plus(a, b)
    a + b
  end
end

Rhino::Context.open do |context|
  context["math"] = MyMath.new
  context.eval("math.plus(20, 22)") #=> 42
end
```

* make a Ruby object be your JavaScript environment
```ruby
math = MyMath.new
Rhino::Context.open(:with => math) do |context|
  context.eval("plus(20, 22)") #=> 42
end

# or the equivalent

math.eval_js("plus(20, 22)")
```

### Context Configuration

* make your standard objects (Object, String, etc...) immutable:
```ruby
Rhino::Context.open(:sealed => true) do |context|
  context.eval("Object.prototype.toString = function() {}") # this is an error!
end
```

* turn on Java integration from JavaScript (probably a bad idea):
```ruby
Rhino::Context.open(:java => true) do |context|
  context.eval("java.lang.System.exit()") # it's dangerous!
end
```

* limit the number of instructions that can be executed to prevent rogue code:
```ruby
Rhino::Context.open(:restrictable => true) do |context|
  context.instruction_limit = 100000
  context.eval("while (true);") # => Rhino::RunawayScriptError
end
```

* limit the time a script executes (rogue scripts):
```ruby
Rhino::Context.open(:restrictable => true, :java => true) do |context|
  context.timeout_limit = 1.5 # seconds
  context.eval %Q{
    for (var i = 0; i < 100; i++) {
      java.lang.Thread.sleep(100);
    }
  } # => Rhino::ScriptTimeoutError
end
```

### Loading JavaScript Source

In addition to just evaluating strings, you can also use streams such as files:

* evaluate bytes read from any File/IO object:
```ruby
  File.open("mysource.js") do |file|
    eval_js file, "mysource.js"
  end
```

* or load it by filename:
```ruby
  Rhino::Context.open do |context|
    context.load("mysource.js")
  end
```

### Configurable Ruby access

By default accessing Ruby objects from JavaScript is compatible with *therubyracer*:
https://github.com/cowboyd/therubyracer/wiki/Accessing-Ruby-Objects-From-JavaScript

Thus you end-up calling arbitrary no-arg methods as if they were JavaScript properties,
since instance accessors (properties) and methods (functions) are indistinguishable:
```ruby
Rhino::Context.open do |context|
  context['Time'] = Time
  context.eval('Time.now')
end
```

However, you can customize this behavior and there's another access implementation
that attempts to mirror only attributes as properties as close as possible:
```ruby
class Foo
  attr_accessor :bar

  def initialize
    @bar = "bar"
  end

  def check_bar
    bar == "bar"
  end
end

Rhino::Ruby::Scriptable.access = :attribute
Rhino::Context.open do |context|
  context['Foo'] = Foo
  context.eval('var foo = new Foo()')
  context.eval('foo.bar') # get property using reader
  context.eval('foo.bar = null') # set property using writer
  context.eval('foo.check_bar()') # called like a function
end
```

If you happen to come up with your own access strategy, just set it directly :
```ruby
Rhino::Ruby::Scriptable.access = FooApp::BarAccess.instance
```

## Safe by default

The Ruby Rhino is designed to let you evaluate JavaScript as safely as possible
unless you tell it to do something more dangerous. The default context is a
hermetically sealed JavaScript environment with only the standard objects and
functions. Nothing from the Ruby world is accessible.

For Ruby objects that you explicitly embed into JavaScript, only the +public+
methods "defined in their classes" are exposed by default e.g.
```ruby
class A
  def a; 'a'; end
end

class B < A
  def b; 'b'; end
end

Rhino::Context.open do |context|
  context['a'] = A.new
  context['b'] = B.new
  context.eval("a.a()") # => 'a'
  context.eval("b.b()") # => 'b'
  context.eval("b.a()") # => 'TypeError: undefined property 'a' is not a function'
end
```

## Context Customizations

Just like the JVM packaged Rhino scripting engine, therubyrhino gem supports
specifying JavaScript context properies (optimization level and language version)
using system properties e.g. to force interpreted mode :

  `jruby -J-Drhino.opt.level=-1 -rtherubyrhino -S ...`

You might also set these programatically as a default for all created contexts :
```ruby
Rhino::Context.default_optimization_level = 1
Rhino::Context.default_javascript_version = 1.6
```

Or using plain old JAVA_OPTS e.g. when setting JavaScript version :

  `-Drhino.js.version=1.7`

## Rhino

Rhino is currently maintained at https://github.com/mozilla/rhino
Release downloads are available at http://www.mozilla.org/rhino/download.html
Rhino is licensed under the MPL 1.1/GPL 2.0 license.

### Using a custom Rhino version

Officially supported versions of Rhino's _js.jar_ are packaged separately as
**therubyrhino_jar** gem. Make sure you're using the latest gem version if you
feel like missing something available with Rhino. For experimenters the jar can
be overriden by defining a `Rhino::JAR_PATH` before `require 'rhino'` e.g. :
```ruby
module Rhino
  JAR_PATH = File.expand_path('lib/rhino/build/rhino1_7R5pre/js.jar')
end
# ...
require 'rhino'
```

## LICENSE:

(The MIT License)

Copyright (c) 2009-2013 Charles Lowell

Permission is hereby granted, free of charge, to any person obtaining
a copy of this software and associated documentation files (the
'Software'), to deal in the Software without restriction, including
without limitation the rights to use, copy, modify, merge, publish,
distribute, sublicense, and/or sell copies of the Software, and to
permit persons to whom the Software is furnished to do so, subject to
the following conditions:

The above copyright notice and this permission notice shall be
included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED 'AS IS', WITHOUT WARRANTY OF ANY KIND,
EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY
CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT,
TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
