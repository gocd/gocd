# Uglifier [![Build Status](https://travis-ci.org/lautis/uglifier.svg?branch=master)](https://travis-ci.org/lautis/uglifier) [![Dependency Status](https://gemnasium.com/lautis/uglifier.svg)](https://gemnasium.com/lautis/uglifier)

Ruby wrapper for [UglifyJS](https://github.com/mishoo/UglifyJS2) JavaScript compressor.

## Installation

Uglifier is available as a ruby gem.

    $ gem install uglifier

Ensure that your environment has a JavaScript interpreter supported by
[ExecJS](https://github.com/sstephenson/execjs). Using `therubyracer` gem
is a safe choice if a runtime isn't already present. Note that while JScript built-in Windows 7 and older works, it is extremely slow.

## Usage

```ruby
require 'uglifier'

Uglifier.new.compile(File.read("source.js"))
# => js file minified

# Or alternatively
Uglifier.compile(File.read("source.js"))
```

Uglifier also supports generating source maps:

```ruby
uglified, source_map = Uglifier.new.compile_with_map(source)
```

When initializing UglifyJS, you can tune the behavior of UglifyJS by passing options. For example, if you want disable variable name mangling:

```ruby
Uglifier.new(:mangle => false).compile(source)

# Or
Uglifier.compile(source, :mangle => false)
```

Available options and their defaults are

```ruby
{
  :output => {
    :ascii_only => true,        # Escape non-ASCII characters
    :comments => :copyright,    # Preserve comments (:all, :jsdoc, :copyright, :none)
    :inline_script => false,    # Escape occurrences of </script in strings
    :quote_keys => false,       # Quote keys in object literals
    :max_line_len => 32 * 1024, # Maximum line length in minified code
    :bracketize => false,       # Bracketize if, for, do, while or with statements, even if their body is a single statement
    :semicolons => true,        # Separate statements with semicolons
    :preserve_line => false,    # Preserve line numbers in outputs
    :beautify => false,         # Beautify output
    :indent_level => 4,         # Indent level in spaces
    :indent_start => 0,         # Starting indent level
    :space_colon => false,      # Insert space before colons (only with beautifier)
    :width => 80,               # Specify line width when beautifier is used (only with beautifier)
    :preamble => nil            # Preamble for the generated JS file. Can be used to insert any code or comment.
  },
  :mangle => {
    :eval => false,             # Mangle names when eval of when is used in scope
    :except => ["$super"],      # Argument names to be excluded from mangling
    :sort => false,             # Assign shorter names to most frequently used variables. Often results in bigger output after gzip.
    :toplevel => false          # Mangle names declared in the toplevel scope
  },                            # Mangle variable and function names, set to false to skip mangling
  :compress => {
    :sequences => true,         # Allow statements to be joined by commas
    :properties => true,        # Rewrite property access using the dot notation
    :dead_code => true,         # Remove unreachable code
    :drop_debugger => true,     # Remove debugger; statements
    :unsafe => false,           # Apply "unsafe" transformations
    :conditionals => true,      # Optimize for if-s and conditional expressions
    :comparisons => true,       # Apply binary node optimizations for comparisons
    :evaluate => true,          # Attempt to evaluate constant expressions
    :booleans => true,          # Various optimizations to boolean contexts
    :loops => true,             # Optimize loops when condition can be statically determined
    :unused => true,            # Drop unreferenced functions and variables
    :hoist_funs => true,        # Hoist function declarations
    :hoist_vars => false,       # Hoist var declarations
    :if_return => true,         # Optimizations for if/return and if/continue
    :join_vars => true,         # Join consecutive var statements
    :cascade => true,           # Cascade sequences
    :negate_iife => true,       # Negate immediately invoked function expressions to avoid extra parens
    :pure_getters => false,     # Assume that object property access does not have any side-effects
    :pure_funcs => nil,         # List of functions without side-effects. Can safely discard function calls when the result value is not used
    :drop_console => false,     # Drop calls to console.* functions
    :angular => false           # Process @ngInject annotations
    :keep_fargs => false        # Preserve unused function arguments
  },                            # Apply transformations to code, set to false to skip
  :define => {},                # Define values for symbol replacement
  :enclose => false,            # Enclose in output function wrapper, define replacements as key-value pairs
  :source_filename => nil,      # The filename of the input file
  :source_root => nil,          # The URL of the directory which contains :source_filename
  :output_filename => nil,      # The filename or URL where the minified output can be found
  :input_source_map => nil,     # The contents of the source map describing the input
  :screw_ie8 => false           # Don't bother to generate safe code for IE8
}
```

## Development

Tests are run using

    bundle exec rake

See [CONTRIBUTING](https://github.com/lautis/uglifier/blob/master/CONTRIBUTING.md) for details about working on and contributing to Uglifier.

## Copyright

© Ville Lautanala. Released under MIT license, see [LICENSE](https://github.com/lautis/uglifier/blob/master/LICENSE.txt) for details.
