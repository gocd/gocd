# XPath

XPath is a Ruby DSL around a subset of XPath 1.0. Its primary purpose is to
facilitate writing complex XPath queries from Ruby code.

[![Build Status](https://secure.travis-ci.org/jnicklas/xpath.png?branch=master)](http://travis-ci.org/jnicklas/xpath)

## Generating expressions

To create quick, one-off expressions, `XPath.generate` can be used:

``` ruby
XPath.generate { |x| x.descendant(:ul)[x.attr(:id) == 'foo'] }
```

You can also call expression methods directly on the `XPath` module:

``` ruby
XPath.descendant(:ul)[XPath.attr(:id) == 'foo']
```

However for more complex expressions, it is probably more convenient to include
the `XPath` module into your own class or module:

``` ruby
module MyXPaths
  include XPath

  def foo_ul
    descendant(:ul)[attr(:id) == 'foo']
  end

  def password_field(id)
    descendant(:input)[attr(:type) == 'password'][attr(:id) == id]
  end
end
```

Both ways return an
[`XPath::Expression`](http://rdoc.info/github/jnicklas/xpath/XPath/Expression)
instance, which can be further modified.  To convert the expression to a
string, just call `#to_s` on it. All available expressions are defined in
[`XPath::DSL`](http://rdoc.info/github/jnicklas/xpath/XPath/DSL).

## String, Hashes and Symbols

When you send a string as an argument to any XPath function, XPath assumes this
to be a string literal. On the other hand if you send in Symbol, XPath assumes
this to be an XPath literal. Thus the following two statements are not
equivalent:

``` ruby
XPath.descendant(:p)[XPath.attr(:id) == 'foo']
XPath.descendant(:p)[XPath.attr(:id) == :foo]
```

These are the XPath expressions that these would be translated to:

```
.//p[@id = 'foo']
.//p[@id = foo]
```

The second expression would match any p tag whose id attribute matches a 'foo'
tag it contains. Most likely this is not what you want.

In fact anything other than a String is treated as a literal. Thus the
following works as expected:

``` ruby
XPath.descendant(:p)[1]
```

Keep in mind that XPath is 1-indexed and not 0-indexed like most other
programming languages, including Ruby.

Hashes are automatically converted to equality expressions, so the above
example could be written as:

``` ruby
XPath.descendant(:p)[:@id => 'foo']
```

Which would generate the same expression:

```
.//p[@id = 'foo']
```

Note that the same rules apply here, both the keys and values in the hash are
treated the same way as any other expression in XPath. Thus the following are
not equivalent:

``` ruby
XPath.descendant(:p)[:@id => 'foo'] # => .//p[@id = 'foo']
XPath.descendant(:p)[:id => 'foo']  # => .//p[id = 'foo']
XPath.descendant(:p)['id' => 'foo'] # => .//p['id' = 'foo']
```

## HTML

XPath comes with a set of premade XPaths for use with HTML documents.

You can generate these like this:

``` ruby
XPath::HTML.link('Home')
XPath::HTML.field('Name')
```

See [`XPath::HTML`](http://rdoc.info/github/jnicklas/xpath/XPath/HTML) for all
available matchers.

## License

(The MIT License)

Copyright © 2010 Jonas Nicklas

Permission is hereby granted, free of charge, to any person obtaining a copy of
this software and associated documentation files (the ‘Software’), to deal in
the Software without restriction, including without limitation the rights to
use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies
of the Software, and to permit persons to whom the Software is furnished to do
so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED ‘AS IS’, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
