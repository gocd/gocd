# The Jasmine Gem [![Build Status](https://travis-ci.org/jasmine/jasmine-gem.png?branch=master)](https://travis-ci.org/jasmine/jasmine-gem)

The [Jasmine](http://github.com/jasmine/jasmine) Ruby Gem is a package of helper code for developing Jasmine projects for Ruby-based web projects (Rails, Sinatra, etc.) or for JavaScript projects where Ruby is a welcome partner. It serves up a project's Jasmine suite in a browser so you can focus on your code instead of manually editing script tags in the Jasmine runner HTML file.

## Contents
This gem contains:

* A small server that builds and executes a Jasmine suite for a project
* A script that sets up a project to use the Jasmine gem's server
* Generators for Ruby on Rails projects (Rails 3 and Rails 4)

You can get all of this by: `gem install jasmine` or by adding Jasmine to your `Gemfile`.

```ruby
group :development, :test do
  gem 'jasmine'
end
```

## Init A Project

To initialize a rails project for Jasmine

    rails g jasmine:install

    rails g jasmine:examples

For any other project (Sinatra, Merb, or something we don't yet know about) use

    jasmine init

    jasmine examples

## Usage

Start the Jasmine server:

    rake jasmine

Point your browser to `localhost:8888`. The suite will run every time this page is re-loaded.

For Continuous Integration environments, add this task to the project build steps:

    rake jasmine:ci

This uses PhantomJS to load and run the Jasmine suite. 

Please note that PhantomJS will be auto-installed by the [phantomjs-gem][phantomjs-gem] at the first `rake jasmine:ci` run. If you have a matching PhantomJS version somewhere on your path, it won't install. You can disable automatic installation altogether (and use the PhantomJS on your path) via the config helper.

[phantomjs-gem]: https://github.com/colszowka/phantomjs-gem#phantomjs-as-a-rubygem

## Configuration

Customize `spec/javascripts/support/jasmine.yml` to enumerate the source files, stylesheets, and spec files you would like the Jasmine runner to include.
You may use dir glob strings.

Alternatively, you may specify the path to your `jasmine.yml` by setting an environment variable:

`rake jasmine:ci JASMINE_CONFIG_PATH=relative/path/to/your/jasmine.yml`

In addition, the `spec_helper` key in your jasmine.yml specifies the path to a ruby file that can do programmatic configuration.
After running `jasmine init` or `rails g jasmine:install` it will point to `spec/javascripts/support/jasmine_helper.rb` which you can modify to fit your needs.

### Running Jasmine on a different port

The ports that `rake jasmine` (or `rake jasmine:server`) and `rake jasmine:ci` run on are configured independently, so they can both run at the same time.

To change the port that `rake jasmine` uses:

In your jasmine_helper.rb:

    Jasmine.configure do |config|
       config.server_port = 5555
    end

By default `rake jasmine:ci` will attempt to find a random open port, to set the port that `rake jasmine:ci` uses:

In your jasmine_helper.rb:

    Jasmine.configure do |config|
       config.ci_port = 1234
    end

## Support

Jasmine Mailing list: [jasmine-js@googlegroups.com](mailto:jasmine-js@googlegroups.com)
Twitter: [@jasminebdd](http://twitter.com/jasminebdd)

Please file issues here at Github

Copyright (c) 2008-2013 Pivotal Labs. This software is licensed under the MIT License.
