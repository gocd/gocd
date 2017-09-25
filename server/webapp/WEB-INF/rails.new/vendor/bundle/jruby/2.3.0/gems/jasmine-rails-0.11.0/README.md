# jasmine-rails gem

[![Build Status](https://secure.travis-ci.org/searls/jasmine-rails.png)](http://travis-ci.org/searls/jasmine-rails)

This project is intended to make it a little easier to integrate [Jasmine](https://github.com/pivotal/jasmine/wiki) into your workflow, particularly if you're working in Rails 3.2 or later. (If you're on earlier versions of Rails, I'd suggest directly using the combination of Pivotal's [jasmine gem](https://github.com/pivotal/jasmine-gem) and [jasmine-headless-webkit](http://johnbintz.github.com/jasmine-headless-webkit/).)

By bundling this gem and configuring your project, you can expect to:

* Be able to run Jasmine specs in a browser (powered by Rails engine mounted into your application)
* Be able to run Jasmine specs from the command line (powered by
  [PhantomJS](http://phantomjs.org/))
* Write specs or source in [CoffeeScript](http://jashkenas.github.com/coffee-script/), leveraging the [asset pipeline](http://railscasts.com/episodes/279-understanding-the-asset-pipeline) to pre-process it

## Installation

First, add jasmine-rails to your Gemfile, like so

    group :test, :development do
      gem 'jasmine-rails'
    end

Next:

```
$ bundle install
```

And finally, run the Rails generator:

```
$ rails generate jasmine_rails:install
```

The generator will create the necessary configuration files and mount a test
runner to [/specs](http://localhost:3000/specs) so that you can get started
writing specs!

## Configuration

Configuring the Jasmine test runner is done in `spec/javascripts/support/jasmine.yml`.

## Asset Pipeline Support

The jasmine-rails gem fully supports the Rails asset pipeline which means you can:

* use `coffee_script` or other Javascript precompilers for source or
  test files
* use sprockets directives to control inclusion/exclusion of dependent
  files
* leverage asset pipeline search paths to include assets from various
  sources/gems

**If you choose to use the asset pipeline support, many of the `jasmine.yml`
configurations become unnecessary** and you can rely on the Rails asset
pipeline to do the hard work of controlling what files are included in
your testsuite.

```yaml
# minimalist jasmine.yml configuration when leveraging asset pipeline
spec_files:
  - "**/*[Ss]pec.{js,coffee}"
```

You can write a spec to test Foo in `spec/javascripts/foo_spec.js`:

```javascript
// include spec/javascripts/helpers/some_helper_file.js and app/assets/javascripts/foo.js
//= require helpers/some_helper_file
//= require foo
describe('Foo', function() {
  it("does something", function() {
    expect(1 + 1).toBe(2);
  });
});
```
\*As noted above, spec_helper and foo.js must be required in order for foo_spec.js to run.

## Spec files in engine

If you have an engine mounted in your project and you need to test the engine's javascript files,
you can instruct jasmine to include and run the spec files from that engine directory.

Given your main project is located in `/workspace/my_project` and your engine in `/workspace/engine`,
you can add the following in the the `jasmine.yml` file:

```yaml
spec_dir:
  - spec/javascripts
  - ../engine/spec/javascripts
```

## Include javascript from external source

If you need to test javascript files that are not part of the assets pipeline (i.e if you have a mobile application
that resides outside of your rails app) you can add the following in the the `jasmine.yml` file:

```yaml
include_dir:
  - ../mobile_app/public/js
```

## Running from the command line

If you were to run:

    RAILS_ENV=test bundle exec rake spec:javascript

You'd hopefully see something like:

    Running Jasmine specs...

    PASS: 0 tests, 0 failures, 0.001 secs.

You can filter execution by passing the `SPEC` option as well:

    RAILS_ENV=test bundle exec rake spec:javascript SPEC=my_test

If you experience an error at this point, the most likely cause is JavaScript being loaded out of order, or otherwise conflicting with other existing JavaScript in your project. See "Debugging" below.

## Running from your browser

Startup your Rails server (ex: `bundle exec rails s`), and navigate to the path you have configured in your routes.rb file (ex: [http://localhost:3000/specs](http://localhost:3000/specs)).
The Jasmine spec runner should appear and start running your testsuite instantly.

## Debugging

### In your browser

In my workflow, I like to work with specs in the command line until I hit a snag and could benefit from debugging in [Web Inspector](http://www.webkit.org/blog/1091/more-web-inspector-updates/) or [Firebug](http://getfirebug.com/) to figure out what's going on.

### From the command line

Even though they both read from the same config file, it's certainly possible that your specs will pass in the browser and fail from the command line. In this case, you can try to debug or analyze what's going on loading the headless runner.html file into your browser environment. The generated runner.html file is written out to `tmp/jasmine/runner.html` after each run.

### Ajax / XHRs

As a general rule, Jasmine is designed for unit testing, and as a result real network requests are not appropriate for tests written in Jasmine. (Isolation strategies can include spying on asynchronous libraries and then synchronously testing callback behavior, as [demonstrated in this gist](https://gist.github.com/searls/946704)).

If your application code issues XHR requests during your test run, please note that **XHR requests for the local filesystem** are blocked by default for most browsers for security reasons.  To debug local XHR requests (for example, if you jasmine-jquery fixtures), you will need to enable local filesystem requests in your browser.

Example for Google Chrome (in Mac OS X):
    open -a "Google Chrome" tmp/jasmine/runner.html --args --allow-file-access-from-files

Again, it's the opinion of the present author that this shouldn't be necessary in any situation but legacy rescue of an existing test suite. With respect specifically to HTML fixtures, please consider [jasmine-fixture](https://github.com/searls/jasmine-fixture) and [my rationale](http://searls.testdouble.com/posts/2011-12-11-jasmine-fixtures.html) for it.

### Custom Helpers

If you need to write a custom spec runner template (for example, using requireJS to load components from your specs), you might benefit from
custom helper functions.  The controller will attempt to load `JasmineRails::SpecHelper` if it exists. An example:

```ruby
# in lib/jasmine_rails/spec_helper.rb
module JasmineRails
  module SpecHelper
    def custom_function
      "hello world"
    end
  end
end
```

Create a custom layout in app/views/layouts/jasmine_rails/spec_runner.html.erb and reference your helper:

```erb
<%= custom_function %>
```

If you wanted to do something like this using [requirejs-rails](https://github.com/jwhitley/requirejs-rails), your helper
might look like this:

```ruby
# in lib/jasmine_rails/spec_helper.rb
module JasmineRails
  module SpecHelper
    # Gives us access to the require_js_include_tag helper
    include RequirejsHelper
  end
end
```

Remove any reference to `src_files` in `spec/javascripts/support/jasmine.yml`, to ensure files aren't loaded prematurely.

Create your custom layout `app/views/layouts/jasmine_rails/spec_runner.html.erb` like so:
```erb

<!DOCTYPE html>
<html>
  <head>
    <meta content="text/html;charset=UTF-8" http-equiv="Content-Type"/>
    <title>Jasmine Specs</title>

    <%= stylesheet_link_tag *jasmine_css_files %>
    <%= requirejs_include_tag %>
    <%= javascript_include_tag *jasmine_js_files %>
  </head>
  <body>
    <div id="jasmine_content"></div>
    <%= yield %>
  </body>
</html>

```

Use require with a callback to load your components:

```coffeescript

describe 'test my module', ->
  require ['my/module'], (Module) ->
    it 'does something', ->
      expect(Module.method).toEqual 'something'
```

### Custom Reporter

You can configure custom reporter files to use when running from the
command line in `jasmine.yml`:

```yml
reporters:
  cool-reporter:
    - "cool-reporter.js"
  awesome-reporter:
    - "awesome-part-1.js"
    - "awesome-part-2.js"
```

Then, specify which reporters to use when you run the rake task:

```
RAILS_ENV=test REPORTERS='cool-reporter,awesome-reporter' rake spec:javascripts
```

The console reporter shipped with jasmine-rails will be used by
default, and you can explicitly use it by the name `console`.

See [jasmine-junitreporter][j-junit] for an example with JUnit output.

[j-junit]: https://github.com/shepmaster/jasmine-junitreporter-gem

## PhantomJS binary

By default the [PhantomJS gem](https://github.com/colszowka/phantomjs-gem) will
be responsible for finding and using an appropriate version of PhantomJS. If
however, you wish to manage your own phantom executable you can set:

```yml
use_phantom_gem: false
```

This will then try and use the `phantom` executable on the current `PATH`.

## PhantomJS command-line options

If you want to pass command-line options to phantomjs executable, you can set:

```yml
phantom_options: --web-security=no
```

This will pass everything defined on `phantom_options` as
[options](http://phantomjs.org/api/command-line.html).
