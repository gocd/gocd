# RSpec JUnit Formatter

[![Build results](http://img.shields.io/travis/sj26/rspec_junit_formatter/master.svg)](https://travis-ci.org/sj26/rspec_junit_formatter) 
[![Gem version](http://img.shields.io/gem/v/rspec_junit_formatter.svg)](https://rubygems.org/gems/rspec_junit_formatter)

[RSpec][rspec] 2 & 3 results that [Jenkins][jenkins] can read. Probably a few other CI services, too.

Inspired by the work of [Diego Souza][dgvncsz0f] on [RSpec Formatters][dgvncsz0f/rspec_formatters] after frustration with [CI Reporter][ci_reporter].

## Usage

Install the gem:

```sh
gem install rspec_junit_formatter
```

Use it:

```sh
rspec --format RspecJunitFormatter --out rspec.xml
```

You'll get an XML file `rspec.xml` with your results in it.

You can use it in combination with other [formatters][rspec-formatters], too:

```sh
rspec --format progress --format RspecJunitFormatter --out rspec.xml
```

### Using in your project with Bundler

Add it to your Gemfile if you're using [Bundler][bundler]. Put it in the same groups as rspec.

```ruby
group :test do
  gem "rspec"
  gem "rspec_junit_formatter"
end
```

Put the same arguments as the commands above in [your `.rspec`][rspec-file]:

```sh
--format RspecJunitFormatter
--out rspec.xml
```

### Parallel tests

For use with `parallel_tests`, add `$TEST_ENV_NUMBER` in the output file option (in `.rspec` or `.rspec_parallel`) to avoid concurrent process write conflicts.

```sh
--format RspecJunitFormatter
--out tmp/rspec<%= ENV["TEST_ENV_NUMBER"] %>.xml
```

The formatter includes `$TEST_ENV_NUMBER` in the test suite name within the XML, too.

## Caveats

 * XML can only represent a [limited subset of characters][xml-charsets] which
   excludes null bytes and most control characters. This gem will use character
   entities where possible and fall back to replacing invalid characters with
   Ruby-like escape codes otherwise. For example, the null byte becomes `\0`.

## Roadmap

 * It would be nice to split things up into individual test suites, although
   would this correspond to example groups? The subject? The spec file? Not
   sure yet.

## License

The MIT License, see [LICENSE][license].

  [rspec]: http://rspec.info/
  [rspec-formatters]: https://relishapp.com/rspec/rspec-core/v/3-6/docs/formatters
  [rspec-file]: https://relishapp.com/rspec/rspec-core/v/3-6/docs/configuration/read-command-line-configuration-options-from-files
  [jenkins]: http://jenkins-ci.org/
  [dgvncsz0f]: https://github.com/dgvncsz0f
  [dgvncsz0f/rspec_formatters]: https://github.com/dgvncsz0f/rspec_formatters
  [ci_reporter]: https://github.com/nicksieger/ci_reporter
  [bundler]: http://gembundler.com/
  [fuubar]: http://jeffkreeftmeijer.com/2010/fuubar-the-instafailing-rspec-progress-bar-formatter/
  [license]: https://github.com/sj26/rspec-junit-formatter/blob/master/LICENSE
  [xml-charsets]: https://www.w3.org/TR/xml/#charsets
