# RSpec JUnit Formatter

[![Build results](http://img.shields.io/travis/sj26/rspec_junit_formatter.svg)](https://travis-ci.org/sj26/rspec_junit_formatter) 
[![Gem version](http://img.shields.io/gem/v/rspec_junit_formatter.svg)](https://rubygems.org/gem/rspec_junit_formatter)

[RSpec][rspec] 2 & 3 results that [Jenkins][jenkins] can read. Probably a few other CI servers, too.

Inspired by the work of [Diego Souza][dgvncsz0f] on [RSpec Formatters][dgvncsz0f/rspec_formatters] after frustration with [CI Reporter][ci_reporter].

## Usage

Install the gem:

    gem install rspec_junit_formatter

Use it:

    rspec --format RspecJunitFormatter  --out rspec.xml

You'll get an XML file with your results in it.

## More Permanent Usage

Add it to your Gemfile if you're using [Bundler][bundler]. Put it in the same groups as rspec.

In your .rspec, usually alongside another formatter, add:

    --format RspecJunitFormatter
    --out rspec.xml

I use it with the excellent [Fuubar formatter][fuubar].

## Roadmap

 * It would be nice to split things up into individual test suites, although would this correspond to example groups? The subject? The spec file? Not sure yet.
 * This would sit nicely in rspec-core, and has been designed to do so.

## License

The MIT License, see [LICENSE][license].

  [rspec]: http://rspec.info/
  [jenkins]: http://jenkins-ci.org/
  [dgvncsz0f]: https://github.com/dgvncsz0f
  [dgvncsz0f/rspec_formatters]: https://github.com/dgvncsz0f/rspec_formatters
  [ci_reporter]: https://github.com/nicksieger/ci_reporter
  [bundler]: http://gembundler.com/
  [fuubar]: http://jeffkreeftmeijer.com/2010/fuubar-the-instafailing-rspec-progress-bar-formatter/
  [license]: https://github.com/sj26/rspec-junit-formatter/blob/master/LICENSE
