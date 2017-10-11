# Addressable

<dl>
  <dt>Homepage</dt><dd><a href="https://github.com/sporkmonger/addressable">github.com/sporkmonger/addressable</a></dd>
  <dt>Author</dt><dd><a href="mailto:bob@sporkmonger.com">Bob Aman</a></dd>
  <dt>Copyright</dt><dd>Copyright © 2006-2015 Bob Aman</dd>
  <dt>License</dt><dd>Apache 2.0</dd>
</dl>

[![Gem Version](http://img.shields.io/gem/dt/addressable.svg)][gem]
[![Build Status](https://secure.travis-ci.org/sporkmonger/addressable.png?branch=master)][travis]
[![Dependency Status](https://gemnasium.com/sporkmonger/addressable.png?travis)][gemnasium]
[![Test Coverage Status](https://img.shields.io/coveralls/sporkmonger/addressable.svg)][coveralls]
[![Documentation Coverage Status](http://inch-ci.org/github/sporkmonger/addressable.svg?branch=master)][inch]
[![Gittip Donate](http://img.shields.io/gittip/sporkmonger.png)](https://www.gittip.com/sporkmonger/ "Support Open Source Development w/ Gittip")

[gem]: https://rubygems.org/gems/addressable
[travis]: http://travis-ci.org/sporkmonger/addressable
[gemnasium]: https://gemnasium.com/sporkmonger/addressable
[coveralls]: https://coveralls.io/r/sporkmonger/addressable
[inch]: http://inch-ci.org/github/sporkmonger/addressable

# Description

Addressable is a replacement for the URI implementation that is part of
Ruby's standard library. It more closely conforms to RFC 3986, RFC 3987, and
RFC 6570 (level 4), providing support for IRIs and URI templates.

# Reference

- {Addressable::URI}
- {Addressable::Template}

# Example usage

```ruby
require "addressable/uri"

uri = Addressable::URI.parse("http://example.com/path/to/resource/")
uri.scheme
#=> "http"
uri.host
#=> "example.com"
uri.path
#=> "/path/to/resource/"

uri = Addressable::URI.parse("http://www.詹姆斯.com/")
uri.normalize
#=> #<Addressable::URI:0xc9a4c8 URI:http://www.xn--8ws00zhy3a.com/>
```


# URI Templates

For more details, see [RFC 6570](https://www.rfc-editor.org/rfc/rfc6570.txt).


```ruby

require "addressable/template"

template = Addressable::Template.new("http://example.com/{?query*}/")
template.expand({
  "query" => {
    'foo' => 'bar',
    'color' => 'red'
  }
})
#=> #<Addressable::URI:0xc9d95c URI:http://example.com/?foo=bar&color=red>

template = Addressable::Template.new("http://example.com/{?one,two,three}")
template.partial_expand({"one" => "1", "three" => 3}).pattern
#=> "http://example.com/?one=1{&two}&three=3"

template = Addressable::Template.new(
  "http://{host}{/segments*}/{?one,two,bogus}{#fragment}"
)
uri = Addressable::URI.parse(
  "http://example.com/a/b/c/?one=1&two=2#foo"
)
template.extract(uri)
#=>
# {
#   "host" => "example.com",
#   "segments" => ["a", "b", "c"],
#   "one" => "1",
#   "two" => "2",
#   "fragment" => "foo"
# }
```

# Install

```console
$ sudo gem install addressable
```

You may optionally turn on native IDN support by installing libidn and the
idn gem:

```console
$ sudo apt-get install idn # Debian/Ubuntu
$ brew install libidn # OS X
$ gem install idn-ruby
```
