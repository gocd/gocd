# Jasmine::JUnitReporter

Wraps [jasmine-junitreporter][jju] in a gem, suitable for use with [jasmine-rails][jr].

[jju]: https://github.com/shepmaster/jasmine-junitreporter
[jr]: https://github.com/searls/jasmine-rails

## Installation

Add this line to your application's Gemfile:

    gem 'jasmine-junitreporter'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install jasmine-junitreporter

## Usage

Add this to your `jasmine.yml`:

```yaml
reporters:
  junit:
    - "JUnitReporter.js"
    - "JUnitReporter.boot.js"
```

`JUnitReporter.js` is provided by this gem. `JUnitReporter.boot.js` is
the glue code you need to provide to configure and enable the
reporter. Create the file with content like:

```js
(function() {
  var reporter = new jasmine.JUnitReporter({
    outputDir: 'output/dir'
  });
  jasmine.getEnv().addReporter(reporter);
})();
```

Alternatively, you can create `JUnitReporter.boot.js.erb`, allowing
for configuration via any Ruby code, including environment variables.

You can then run the tests as:

```
$ RAILS_ENV=test REPORTERS='console,junit' rake spec:javascripts
```

## Contributing

1. Fork it ( http://github.com/shepmaster/jasmine-junitreporter-gem/fork )
2. Create your feature branch (`git checkout -b my-new-feature`)
3. Commit your changes (`git commit -am 'Add some feature'`)
4. Push to the branch (`git push origin my-new-feature`)
5. Create new Pull Request
