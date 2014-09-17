ExecJS
======

ExecJS lets you run JavaScript code from Ruby. It automatically picks
the best runtime available to evaluate your JavaScript program, then
returns the result to you as a Ruby object.

ExecJS supports these runtimes:

* [therubyracer](https://github.com/cowboyd/therubyracer) - Google V8
  embedded within Ruby
* [therubyrhino](https://github.com/cowboyd/therubyrhino) - Mozilla
  Rhino embedded within JRuby
* [Node.js](http://nodejs.org/)
* Apple JavaScriptCore - Included with Mac OS X
* [Microsoft Windows Script Host](http://msdn.microsoft.com/en-us/library/9bbdkx3k.aspx) (JScript)

A short example:

``` ruby
require "execjs"
ExecJS.eval "'red yellow blue'.split(' ')"
# => ["red", "yellow", "blue"]
```

A longer example, demonstrating how to invoke the CoffeeScript compiler:

``` ruby
require "execjs"
require "open-uri"
source = open("http://coffeescript.org/extras/coffee-script.js").read

context = ExecJS.compile(source)
context.call("CoffeeScript.compile", "square = (x) -> x * x", bare: true)
# => "var square;\nsquare = function(x) {\n  return x * x;\n};"
```

# Installation

```
$ gem install execjs
```


# FAQ

**Why can't I use CommonJS `require()` inside ExecJS?**

ExecJS provides a lowest common denominator interface to any JavaScript runtime.
Use ExecJS when it doesn't matter which JavaScript interpreter your code runs
in. If you want to access the Node API, you should check another library like
[commonjs.rb](https://github.com/cowboyd/commonjs.rb) designed to provide a
consistent interface.


# License

Copyright (c) 2014 Sam Stephenson and Josh Peek.

Released under the MIT license. See `LICENSE` for details.
