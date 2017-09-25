[![Build Status](https://travis-ci.org/mipearson/webpack-rails.svg?branch=master)](https://travis-ci.org/mipearson/webpack-rails) [![Gem Version](https://badge.fury.io/rb/webpack-rails.svg)](http://badge.fury.io/rb/webpack-rails)

# webpack-rails

**webpack-rails** gives you tools to integrate Webpack in to an existing Ruby on Rails application.

It will happily co-exist with sprockets but does not use it for production fingerprinting or asset serving. **webpack-rails** is designed with the assumption that if you're using Webpack you treat Javascript as a first-class citizen. This means that you control the webpack config, package.json, and use yarn to install Webpack & its plugins.

In development mode [webpack-dev-server](http://webpack.github.io/docs/webpack-dev-server.html) is used to serve webpacked entry points and offer hot module reloading. In production entry points are built in to `public/webpack`. **webpack-rails** uses [stats-webpack-plugin](https://www.npmjs.com/package/stats-webpack-plugin) to translate entry points in to asset paths.

It was designed for use at [Marketplacer](http://www.marketplacer.com) to assist us in migrating our Javascript (and possibly our SCSS) off of Sprockets. It first saw production use in June 2015.

Our examples show **webpack-rails** co-existing with sprockets (as that's how environment works), but sprockets is not used or required for development or production use of this gem.

This gem has been tested against Rails 4.2 and Ruby 2.2. Earlier versions of Rails (>= 3.2) and Ruby (>= 2.0) may work, but we haven't tested them.

## Using webpack-rails

**We have a demo application: [webpack-rails-demo](https://github.com/mipearson/webpack-rails-demo)**

### Installation

  1. Install [yarn](https://yarnpkg.com/en/docs/install) if you haven't already
  1. Add `webpack-rails` to your gemfile
  1. Run `bundle install` to install the gem
  1. Run `bundle exec rails generate webpack_rails:install` to copy across example files
  1. Run `foreman start` to start `webpack-dev-server` and `rails server` at the same time
  1. Add the webpack entry point to your layout (see next section)
  1. Edit `webpack/application.js` and write some code


### Adding the entry point to your Rails application

To add your webpacked javascript in to your app, add the following to the `<head>` section of your to your `layout.html.erb`:

```erb
<%= javascript_include_tag *webpack_asset_paths("application") %>
```

Take note of the splat (`*`): `webpack_asset_paths` returns an array, as one entry point can map to multiple paths, especially if hot reloading is enabled in Webpack.

If your webpack is configured to output both CSS and JS, you can use the `extension:` argument to filter which files are returned by the helper:

```erb
<%= javascript_include_tag *webpack_asset_paths('application', extension: 'js') %>
<%= stylesheet_link_tag *webpack_asset_paths('application', extension: 'css') %>
```

#### Use with webpack-dev-server live reload

If you're using the webpack dev server's live reload feature (not the React hot reloader), you'll also need to include the following in your layout template:

``` html
<script src="http://localhost:3808/webpack-dev-server.js"></script>
```

### How it works

Have a look at the files in the `examples` directory. Of note:

  * We use [foreman](https://github.com/ddollar/foreman) and a `Procfile` to run our rails server & the webpack dev server in development at the same time
  * The webpack and gem configuration must be in sync - look at our railtie for configuration options
  * We require that **stats-webpack-plugin** is loaded to automatically generate a production manifest & resolve paths during development

### Configuration Defaults

  * Webpack configuration lives in `config/webpack.config.js`
  * Webpack & Webpack Dev Server binaries are in `node_modules/.bin/`
  * Webpack Dev Server will run on port 3808 on localhost via HTTP
  * Webpack Dev Server is enabled in development & test, but not in production
  * Webpacked assets will be compiled to `public/webpack`
  * The manifest file is named `manifest.json`

#### Dynamic host

To have the host evaluated at request-time, set `host` to a proc:

```ruby
config.webpack.dev_server.host = proc { request.host }
```

This is useful when accessing your Rails app over the network (remember to bind both your Rails app and your WebPack server to `0.0.0.0`).

#### Use with docker-compose

If you're running `webpack-dev-server` as part of docker compose rather than `foreman`, you might find that the host and port that rails needs to use to retrieve the manifest isn't the same as the host and port that you'll be giving to the browser to retrieve the assets.

If so, you can set the `manifest_host` and `manifest_port` away from their default of `localhost` and port 3808.

### Working with browser tests

In development, we make sure that the `webpack-dev-server` is running when browser tests are running.

#### Continuous Integration

In CI, we manually run `webpack` to compile the assets to public and set `config.webpack.dev_server.enabled` to `false` in our `config/environments/test.rb`:

``` ruby
config.webpack.dev_server.enabled = !ENV['CI']
```

### Production Deployment

Add `rake webpack:compile` to your deployment. It serves a similar purpose as Sprockets' `assets:precompile` task. If you're using Webpack and Sprockets (as we are at Marketplacer) you'll need to run both tasks - but it doesn't matter which order they're run in.

If you deploy to Heroku, you can add the special
[webpack-rails-buildpack](https://github.com/febeling/webpack-rails-buildpack)
in order to perform this rake task on each deployment.

If you're using `[chunkhash]` in your build asset filenames (which you should be, if you want to cache them in production), you'll need to persist built assets between deployments. Consider in-flight requests at the time of deployment: they'll receive paths based on the old `manifest.json`, not the new one.

## TODO

* Drive config via JSON, have webpack.config.js read same JSON?
* Custom webpack-dev-server that exposes errors, stats, etc
* [react-rails](https://github.com/reactjs/react-rails) fork for use with this workflow
* Integration tests

## Contributing

Pull requests & issues welcome. Advice & criticism regarding webpack config approach also welcome.

Please ensure that pull requests pass both rubocop & rspec. New functionality should be discussed in an issue first.

## Acknowledgements

* Len Garvey for his [webpack-rails](https://github.com/lengarvey/webpack-rails) gem which inspired this implementation
* Sebastian Porto for [Rails with Webpack](https://reinteractive.net/posts/213-rails-with-webpack-why-and-how)
* Clark Dave for [How to use Webpack with Rails](http://clarkdave.net/2015/01/how-to-use-webpack-with-rails/)
