# Webpacker

![travis-ci status](https://api.travis-ci.org/rails/webpacker.svg?branch=master)
[![node.js](https://img.shields.io/badge/node-%3E%3D%206.0.0-brightgreen.svg)](https://nodejs.org/en/)
[![Gem](https://img.shields.io/gem/v/webpacker.svg)](https://github.com/rails/webpacker)

Webpacker makes it easy to use the JavaScript pre-processor and bundler
[Webpack 3.x.x+](https://webpack.js.org/)
to manage application-like JavaScript in Rails. It coexists with the asset pipeline,
as the primary purpose for Webpack is app-like JavaScript, not images, CSS, or
even JavaScript Sprinkles (that all continues to live in app/assets).

However, it is possible to use Webpacker for CSS, images and fonts assets as well,
in which case you may not even need the asset pipeline. This is mostly relevant when exclusively using component-based JavaScript frameworks.

<!-- START doctoc generated TOC please keep comment here to allow auto update -->
<!-- DON'T EDIT THIS SECTION, INSTEAD RE-RUN doctoc TO UPDATE -->
## Table of Contents

- [Prerequisites](#prerequisites)
- [Features](#features)
- [Installation](#installation)
  - [Upgrading](#upgrading)
  - [Usage](#usage)
  - [Development](#development)
  - [Webpack configuration](#webpack-configuration)
- [Integrations](#integrations)
  - [React](#react)
  - [Angular with TypeScript](#angular-with-typescript)
  - [Vue](#vue)
  - [Elm](#elm)
- [Paths](#paths)
  - [Resolved](#resolved)
  - [Watched](#watched)
- [Deployment](#deployment)
- [Docs](#docs)
- [License](#license)

<!-- END doctoc generated TOC please keep comment here to allow auto update -->


## Prerequisites

* Ruby 2.2+
* Rails 4.2+
* Node.js 6.0.0+
* Yarn 0.25.2+


## Features

* [Webpack 3.x.x](https://webpack.js.org/)
* ES6 with [babel](https://babeljs.io/)
* Automatic code splitting using multiple entry points
* Stylesheets - SASS and CSS
* Images and fonts
* PostCSS - Auto-Prefixer
* Asset compression, source-maps, and minification
* CDN support
* React, Angular, Elm and Vue support out-of-the-box
* Rails view helpers
* Extensible and configurable


## Installation

You can either add Webpacker during setup of a new Rails 5.1+ application
using new `--webpack` option:

```bash
# Available Rails 5.1+
rails new myapp --webpack
```

Or add it to your `Gemfile`:

```ruby
# Gemfile
gem 'webpacker', '~> 3.0'

# OR if you prefer to use master
gem 'webpacker', git: 'https://github.com/rails/webpacker.git'
```

and finally, run following to install webpacker:

```bash
bundle
bundle exec rails webpacker:install

# OR (on rails version < 5.0)
bundle exec rake webpacker:install
```


### Usage

Once installed you can start writing modern ES6-flavored JavaScript app today:

```yml
app/javascript:
  ├── packs:
  │   # only webpack entry files here
  │   └── application.js
  └── src:
  │   └── application.css
  └── images:
      └── logo.svg
```

You can then link the javascript pack in Rails view using `javascript_pack_tag` helper.
If you have styles imported in your pack file, you can link using `stylesheet_pack_tag`:

```erb
<%= javascript_pack_tag 'application' %>
<%= stylesheet_pack_tag 'application' %>
```

If you want to link a static asset for `<link rel="prefetch">` or `<img />` tag, you
can use `asset_pack_path` helper:

```erb
<link rel="prefetch" href="<%= asset_pack_path 'application.css' %>" />
<img src="<%= asset_pack_path 'images/logo.svg' %>" />
```

**Note:** In order for your styles or static assets files to be available in your view,
you would need to link them in your "pack" or entry file.


### Development

Webpacker ships with two binstubs: `./bin/webpack` and `./bin/webpack-dev-server`.
Both are thin wrappers around the standard `webpack.js` and `webpack-dev-server.js`
executable to ensure that the right configuration file and environment variables
are loaded depending on your environment.

In development, Webpacker compiles on demand rather than upfront by default. This
happens when you refer to any of the pack assets using the Webpacker helper methods.
That means you don't have to run any separate process. Compilation errors are logged
to the standard Rails log.

If you want to use live code reloading, or you have enough JavaScript that on-demand compilation is too slow, you'll need to run `./bin/webpack-dev-server` or `ruby ./bin/webpack-dev-server` if on windows,
in a separate terminal from `bundle exec rails s`. This process will watch for changes
in the `app/javascript/packs/*.js` files and automatically reload the browser to match.

```bash
# webpack dev server
./bin/webpack-dev-server

# watcher
./bin/webpack --colors --progress

# standalone build
./bin/webpack
```

Once you start this development server, Webpacker will automatically start proxying all
webpack asset requests to this server. When you stop the server, it'll revert to
on-demand compilation again.

You can use environment variables as options supported by [webpack-dev-server](https://webpack.js.org/configuration/dev-server/) in the form `WEBPACKER_DEV_SERVER_<OPTION>`. Please note that these environment variables will always take precedence over the ones already set in the configuration file.

```bash
WEBPACKER_DEV_SERVER_HOST=example.com WEBPACKER_DEV_SERVER_INLINE=true WEBPACKER_DEV_SERVER_HOT=false ./bin/webpack-dev-server
```

By default, webpack dev server listens on `localhost` in development for security
but if you want your app to be available over local LAN IP or VM instance like vagrant
you can set the `host` when running `./bin/webpack-dev-server` binstub:

```bash
WEBPACKER_DEV_SERVER_HOST=0.0.0.0 ./bin/webpack-dev-server
```

**Note:** Don't forget to prefix `ruby` when running these binstubs on windows

### Webpack configuration

See [docs/Webpack](docs/webpack.md) for modifying webpack configuration and loaders.


### Upgrading

You can run following commands to upgrade webpacker to the latest stable version, this involves upgrading the gem and npm module:

```bash
bundle update webpacker
yarn upgrade @rails/webpacker --latest
```

## Integrations

Webpacker ships with basic out-of-the-box integration for React, Angular, Vue and Elm.
You can see a list of available commands/tasks by running `bundle exec rails webpacker`:

### React

To use Webpacker with [React](https://facebook.github.io/react/), create a
new Rails 5.1+ app using `--webpack=react` option:

```bash
# Rails 5.1+
rails new myapp --webpack=react
```

(or run `bundle exec rails webpacker:install:react` in a existing Rails app already
setup with webpacker).

The installer will add all relevant dependencies using yarn, any changes
to the configuration files and an example React component to your
project in `app/javascript/packs` so that you can experiment with React right away.


### Angular with TypeScript

To use Webpacker with [Angular](https://angular.io/), create a
new Rails 5.1+ app using `--webpack=angular` option:

```bash
# Rails 5.1+
rails new myapp --webpack=angular
```

(or run `bundle exec rails webpacker:install:angular` on a Rails app already
setup with webpacker).

The installer will add TypeScript and Angular core libraries using yarn plus
any changes to the configuration files. An example component is written in
TypeScript will also be added to your project in `app/javascript` so that
you can experiment with Angular right away.


### Vue

To use Webpacker with [Vue](https://vuejs.org/), create a
new Rails 5.1+ app using `--webpack=vue` option:

```bash
# Rails 5.1+
rails new myapp --webpack=vue
```
(or run `bundle exec rails webpacker:install:vue` on a Rails app already setup with webpacker).

The installer will add Vue and required libraries using yarn plus
any changes to the configuration files. An example component will
also be added to your project in `app/javascript` so that you can
experiment Vue right away.


### Elm

To use Webpacker with [Elm](http://elm-lang.org), create a
new Rails 5.1+ app using `--webpack=elm` option:

```
# Rails 5.1+
rails new myapp --webpack=elm
```

(or run `bundle exec rails webpacker:install:elm` on a Rails app already setup with webpacker).

The Elm library and core packages will be added via Yarn and Elm itself.
An example `Main.elm` app will also be added to your project in `app/javascript`
so that you can experiment with Elm right away.


## Paths

By default, webpacker ships with simple conventions for where the javascript
app files and compiled webpack bundles will go in your rails app,
but all these options are configurable from `config/webpacker.yml` file.

The configuration for what Webpack is supposed to compile by default rests
on the convention that every file in `app/javascript/packs/*`**(default)**
or whatever path you set for `source_entry_path` in the `webpacker.yml` configuration
is turned into their own output files (or entry points, as Webpack calls it). Therefore you don't want to put anything inside `packs` directory that you do want to be
an entry file. As a rule thumb, put all files your want to link in your views inside
"packs" directory and keep everything else under `app/javascript`.

Suppose you want to change the source directory from `app/javascript`
to `frontend` and output to `assets/packs`. This is how you would do it:

```yml
# config/webpacker.yml
source_path: frontend
source_entry_path: packs
public_output_path: assets/packs # outputs to => public/assets/packs
```

Similarly you can also control and configure `webpack-dev-server` settings from `config/webpacker.yml` file:

```yml
# config/webpacker.yml
development:
  dev_server:
    host: localhost
    port: 3035
```

If you have `hmr` turned to true, then the `stylesheet_pack_tag` generates no output, as you will want to configure your styles to be inlined in your JavaScript for hot reloading. During production and testing, the `stylesheet_pack_tag` will create the appropriate HTML tags.


### Resolved

If you are adding webpacker to an existing app that has most of the assets inside
`app/assets` or inside an engine and you want to share that
with webpack modules then you can use `resolved_paths`
option available in `config/webpacker.yml`, which lets you
add additional paths webpack should lookup when resolving modules:

```yml
resolved_paths: ['app/assets']
```

You can then import them inside your modules like so:

```js
// Note it's relative to parent directory i.e. app/assets
import 'stylesheets/main'
import 'images/rails.png'
```

**Note:** Please be careful when adding paths here otherwise it
will make the compilation slow, consider adding specific paths instead of
whole parent directory if you just need to reference one or two modules


### Watched

By default, the lazy compilation is cached until a file is changed under
tracked paths. You can configure the paths tracked
by adding new paths to `watched_paths` array, much like rails `autoload_paths`:

```rb
# config/initializers/webpacker.rb
# or config/application.rb
Webpacker::Compiler.watched_paths << 'bower_components'
```


## Deployment

Webpacker hooks up a new `webpacker:compile` task to `assets:precompile`, which gets run whenever you run `assets:precompile`. If you are not using sprockets you
can manually trigger `NODE_ENV=production bundle exec rails webpacker:compile`
during your app deploy.


## Docs

You can find more detailed guides under [docs](./docs).


## License
Webpacker is released under the [MIT License](https://opensource.org/licenses/MIT).
