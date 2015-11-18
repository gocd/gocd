Tilt [![Build Status](https://secure.travis-ci.org/rtomayko/tilt.png)](http://travis-ci.org/rtomayko/tilt) [![Dependency Status](https://gemnasium.com/rtomayko/tilt.png)](https://gemnasium.com/rtomayko/tilt)
====

**NOTE** The following file documents the current release of Tilt (2.0). See
https://github.com/rtomayko/tilt/tree/tilt-1 for documentation for Tilt 1.4.

Tilt is a thin interface over a bunch of different Ruby template engines in
an attempt to make their usage as generic as possible. This is useful for web
frameworks, static site generators, and other systems that support multiple
template engines but don't want to code for each of them individually.

The following features are supported for all template engines (assuming the
feature is relevant to the engine):

 * Custom template evaluation scopes / bindings
 * Ability to pass locals to template evaluation
 * Support for passing a block to template evaluation for "yield"
 * Backtraces with correct filenames and line numbers
 * Template file caching and reloading
 * Fast, method-based template source compilation

The primary goal is to get all of the things listed above right for all
template engines included in the distribution.

Support for these template engines is included with the package:

    ENGINE                     FILE EXTENSIONS         REQUIRED LIBRARIES
    -------------------------- ----------------------- ----------------------------
    Asciidoctor                .ad, .adoc, .asciidoc   asciidoctor (>= 0.1.0)
    ERB                        .erb, .rhtml            none (included ruby stdlib)
    Interpolated String        .str                    none (included ruby core)
    Erubis                     .erb, .rhtml, .erubis   erubis
    Haml                       .haml                   haml
    Sass                       .sass                   haml (< 3.1) or sass (>= 3.1)
    Scss                       .scss                   haml (< 3.1) or sass (>= 3.1)
    Less CSS                   .less                   less
    Builder                    .builder                builder
    Liquid                     .liquid                 liquid
    RDiscount                  .markdown, .mkd, .md    rdiscount
    Redcarpet                  .markdown, .mkd, .md    redcarpet
    BlueCloth                  .markdown, .mkd, .md    bluecloth
    Kramdown                   .markdown, .mkd, .md    kramdown
    Maruku                     .markdown, .mkd, .md    maruku
    RedCloth                   .textile                redcloth
    RDoc                       .rdoc                   rdoc
    Radius                     .radius                 radius
    Markaby                    .mab                    markaby
    Nokogiri                   .nokogiri               nokogiri
    CoffeeScript               .coffee                 coffee-script (+ javascript)
    Creole (Wiki markup)       .wiki, .creole          creole
    WikiCloth (Wiki markup)    .wiki, .mediawiki, .mw  wikicloth
    Yajl                       .yajl                   yajl-ruby
    CSV                        .rcsv                   none (Ruby >= 1.9), fastercsv (Ruby < 1.9)

These template engines ship with their own Tilt integration:

    ENGINE                     FILE EXTENSIONS   REQUIRED LIBRARIES
    -------------------------- ----------------- ----------------------------
    Slim                       .slim             slim (>= 0.7)
    Embedded JavaScript                          sprockets
    Embedded CoffeeScript                        sprockets
    JST                                          sprockets
    Org-mode                   .org              org-ruby (>= 0.6.2)
    Handlebars                 .hbs, .handlebars tilt-handlebars

See [TEMPLATES.md][t] for detailed information on template engine
options and supported features.

[t]: http://github.com/rtomayko/tilt/blob/master/docs/TEMPLATES.md
   "Tilt Template Engine Documentation"

Basic Usage
-----------

Instant gratification:

    require 'erb'
    require 'tilt'
    template = Tilt.new('templates/foo.erb')
    => #<Tilt::ERBTemplate @file="templates/foo.rb" ...>
    output = template.render
    => "Hello world!"

It's recommended that calling programs explicitly require template engine
libraries (like 'erb' above) at load time. Tilt attempts to lazy require the
template engine library the first time a template is created but this is
prone to error in threaded environments.

The {Tilt} module contains generic implementation classes for all supported
template engines. Each template class adheres to the same interface for
creation and rendering. In the instant gratification example, we let Tilt
determine the template implementation class based on the filename, but
{Tilt::Template} implementations can also be used directly:

    require 'tilt/haml'
    template = Tilt::HamlTemplate.new('templates/foo.haml')
    output = template.render

The `render` method takes an optional evaluation scope and locals hash
arguments. Here, the template is evaluated within the context of the
`Person` object with locals `x` and `y`:

    require 'tilt/erb'
    template = Tilt::ERBTemplate.new('templates/foo.erb')
    joe = Person.find('joe')
    output = template.render(joe, :x => 35, :y => 42)

If no scope is provided, the template is evaluated within the context of an
object created with `Object.new`.

A single `Template` instance's `render` method may be called multiple times
with different scope and locals arguments. Continuing the previous example,
we render the same compiled template but this time in jane's scope:

    jane = Person.find('jane')
    output = template.render(jane, :x => 22, :y => nil)

Blocks can be passed to `render` for templates that support running
arbitrary ruby code (usually with some form of `yield`). For instance,
assuming the following in `foo.erb`:

    Hey <%= yield %>!

The block passed to `render` is called on `yield`:

    template = Tilt::ERBTemplate.new('foo.erb')
    template.render { 'Joe' }
    # => "Hey Joe!"

Template Mappings
-----------------

The {Tilt::Mapping} class includes methods for associating template
implementation classes with filename patterns and for locating/instantiating
template classes based on those associations.

The {Tilt} module has a global instance of `Mapping` that is populated with the
table of template engines above.

The {Tilt.register} method associates a filename pattern with a specific
template implementation. To use ERB for files ending in a `.bar` extension:

     >> Tilt.register Tilt::ERBTemplate, 'bar'
     >> Tilt.new('views/foo.bar')
     => #<Tilt::ERBTemplate @file="views/foo.bar" ...>

Retrieving the template class for a file or file extension:

     >> Tilt['foo.bar']
     => Tilt::ERBTemplate
     >> Tilt['haml']
     => Tilt::HamlTemplate

Retrieving a list of template classes for a file:

    >> Tilt.templates_for('foo.bar')
    => [Tilt::ERBTemplate]
    >> Tilt.templates_for('foo.haml.bar')
    => [Tilt::ERBTemplate, Tilt::HamlTemplate]

The template class is determined by searching for a series of decreasingly
specific name patterns. When creating a new template with
`Tilt.new('views/foo.html.erb')`, we check for the following template
mappings:

  1. `views/foo.html.erb`
  2. `foo.html.erb`
  3. `html.erb`
  4. `erb`

Encodings
---------

Tilt needs to know the encoding of the template in order to work properly:

Tilt will use `Encoding.default_external` as the encoding when reading external
files. If you're mostly working with one encoding (e.g. UTF-8) we *highly*
recommend setting this option. When providing a custom reader block (`Tilt.new
{ custom_string }`) you'll have ensure the string is properly encoded yourself.

Most of the template engines in Tilt also allows you to override the encoding
using the `:default_encoding`-option:

```ruby
tmpl = Tilt.new('hello.erb', :default_encoding => 'Big5')
```

Ultimately it's up to the template engine how to handle the encoding: It might
respect `:default_encoding`, it might always assume it's UTF-8 (like
CoffeeScript), or it can do its own encoding detection.

Template Compilation
--------------------

Tilt compiles generated Ruby source code produced by template engines and reuses
it on subsequent template invocations. Benchmarks show this yields a 5x-10x
performance increase over evaluating the Ruby source on each invocation.

Template compilation is currently supported for these template engines:
StringTemplate, ERB, Erubis, Haml, Nokogiri, Builder and Yajl.

LICENSE
-------

Tilt is Copyright (c) 2010 [Ryan Tomayko](http://tomayko.com/about) and
distributed under the MIT license. See the `COPYING` file for more info.
