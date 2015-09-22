<!--
Marked Style: GitHub
-->
# RequireJS for Rails

Integrates [RequireJS](http://requirejs.org/) into the Rails 3+ Asset Pipeline.

**UPGRADE NOTES:** Users upgrading within the 0.x series should read the Changes section for relevant usage changes.  We're pushing hard to 1.0, when the configuration and setup details will be declared stable.  Until that time expect some bumps as things bake out.

## Usage

1.  Add this to your Rails app's `Gemfile`:

    ```
    gem 'requirejs-rails'
    ```

2.  Remove all Sprockets directives such as `//= require jquery` from `application.js` and elsewhere.  Instead establish JavaScript dependencies using AMD-style `define()` and `require()` calls.

3.  Use `requirejs_include_tag` at the top-level of your app's layout(s).  Other modules will be pulled in dynamically by `require.js` in development and for production builds optimized by `r.js`.  Here's a basic `app/views/layouts/application.html.erb` modified for `requirejs-rails`:

    ```erb
    <!DOCTYPE html>
    <html>
    <head>
      <title>Frobnitz Online</title>
      <%= stylesheet_link_tag   "application" %>
      <%= requirejs_include_tag "application" %>
      <%= csrf_meta_tags %>
      <meta charset="utf-8">
    </head>
    <body>

    <%= yield %>

    </body>
    </html>
    ```

4.  Organize your JavaScript or CoffeeScript code into modules using `define()`:

    ```coffeescript
    # app/assets/javascripts/views/tweet_view.js.coffee

    define ['backbone'], (Backbone) ->
      class TweetView extends Backbone.View
        # ...
    ```

5.  Instantiate your app using `require()` from a top-level module such as `application.js`:

    ```coffeescript
    # app/assets/javascripts/application.js.coffee

    require ['jquery', 'backbone', 'TheApp'], ($, Backbone, TheApp) ->

      # Start up the app once the DOM is ready
      $ ->
        window.App = new TheApp()
        Backbone.history.start
          pushState: true
        window.App.start()
    ```

6.  When ready, build your assets for production deployment as usual.
    `requirejs-rails` defaults to a single-file build of `application.js`.
    Additional modules and r.js layered builds may be specified via
    `config/requirejs.yml`; see the Configuration section below.

    ```rake assets:precompile```

## Configuration

### The Basics

Configuration lives in `config/requirejs.yml`.  These values are inspected and
used by `requirejs-rails` and passed along as configuration for require.js and
`r.js`.  The default configuration declares `application.js` as the sole
top-level module.  This can be overridden by creating
a `config/requirejs.yml`, such as:

```yaml
modules:
  - name: 'mytoplevel'
```

You may pass in [require.js config
options](http://requirejs.org/docs/api.html#config) as needed.  For example,
to add path parameters:

```yaml
paths:
  d3: "d3/d3"
  "d3.time": "d3/d3.time"
```

### Layered builds

Only modules specified in the configuration will be created as build artifacts
by `r.js`.  [Layered r.js
builds](http://requirejs.org/docs/faq-optimization.html#priority) be
configured like so:

```yaml
modules:
  - name: 'appcommon'
  - name: 'page1'
    exclude: ['appcommon']
  - name: 'page2'
    exclude: ['appcommon']
priority: ['appcommon']
```

In this example, only modules `page1` and `page2` are intended for direct
loading via `requirejs_include_tag`. The `appcommon` module contains
dependencies shared by the per-page modules.  As a guideline, each module in
the configuration should be referenced by one of:

- A `requirejs_include_tag` in a template
- Pulled in via a dynamic `require()` call.  Modules which are solely
  referenced by a dynamic `require()` call (i.e. a call not optimized by r.js)
  **must** be specified in the modules section in order to produce a correct
  build.
- Be a common library module like `appcommon`, listed in the `priority` config
  option.

### Almond support

This gem supports single-file builds with
[almond](https://github.com/jrburke/almond). Use the following setting in
`application.rb` to enable it:

```ruby
config.requirejs.loader = :almond
```

Almond builds have the restriction that there must be exactly one modules entry in
`requirejs.yml`.  Typically the [wrap option](https://github.com/jrburke/r.js/blob/master/build/example.build.js#L275) will be used to create a self-contained build:

```yaml
modules:
  - name: 'main'
wrap: true
```

### Build-time asset filter

The `requirejs-rails` build process uses the Asset Pipeline to assemble assets
for the `r.js` build.  By default, assets ending in `.js`, `.html`, and `.txt`
will be made available to the build.  If you have other asset suffixes to
include, use the `logical_path_patterns` config setting to add them.

For example, if your templates all end in `.templ` like so...

```javascript
// in app/assets/javascripts/myapp.js
define(function (require) {
  var stuff = require('text!stuff.templ');
  // ...
});
```

... then this config setting will ensure they're picked up in the build:

```ruby
# in config/application.rb
config.requirejs.logical_path_patterns += [/\.templ$/]
```

## Advanced features

### Additional data attributes

`requirejs_include_tag` accepts an optional block which should return a hash.
This hash will be used to populate additional `data-...` attributes like so:

```erb
<%= requirejs_include_tag "page1" do |controller|
      { 'foo' => controller.foo,
        'bar' => controller.bar
      }
    end
%>
```

This will generate a script tag like so:

```
<script data-main="/assets/page1.js" data-foo="..." data-bar="..." src="/assets/require.js"></script>
```

### External domain (CDN) support

There are two ways in which requirejs-rails supports the use of different
domains for serving built JavaScript modules, as is the case when using
a [CDN](http://en.wikipedia.org/wiki/Content_delivery_network).

1.  URLs in paths config in `requirejs.yml`:

    If requirejs-rails encounters an URL as the right-hand side of a paths
    configuration, it will correctly emit that as `"empty:"` during the build
    process so that [r.js will do the right thing](http://requirejs.org/docs/optimization.html#empty).

    Example:

    ```yaml
    paths:
      jquery: "https://ajax.googleapis.com/ajax/libs/jquery/1.7.2/jquery.min.js"
    ```

2.  Deploying all requirejs-rails assets to a CDN:

    In `config/environments/production.rb` (or another environment)
    set the run_config as follows:

    ```ruby
    config.requirejs.run_config['baseUrl'] = 'http://mycdn.example.com/12345abc/assets'
    ```

    The [`asset_sync` gem](https://github.com/rumblelabs/asset_sync) is one
    tool that can be used to deploy your built assets to a CDN (S3, in this
    case).

## Troubleshooting

### Avoid `config.assets.precompile`

Don't set `config.assets.precompile` to reference any of your AMD module code.
Avoid it altogether, except to reference non-AMD code that you're loading via
javascript_include_tag, and which is **never** referenced by the AMD codebase.

## Using AMD libraries

I currently recommend placing your AMD libraries into
`vendor/assets/javascripts`.  The needs of a few specific libraries are
discussed below.

### jQuery

jQuery users must use jQuery 1.7 or later (`jquery-rails >= 1.0.17`) to use it as an [AMD module](https://github.com/amdjs/amdjs-api/wiki/AMD) with RequireJS.  To use jQuery in a module:

```coffeescript
# app/assets/javascripts/hello.js

define ['jquery'], ($) ->
  (id) ->
    $(id).append('<div>hello!</div>')
```

### Backbone.js

Backbone 0.9.x doesn't support AMD natively.  I recommend the [amdjs
fork of Backbone](https://github.com/amdjs/backbone/) which adds AMD
support and actively tracks mainline.

### Underscore.js

Underscore 1.3.x likewise doesn't have AMD support.  Again, see
the [amdjs fork of Underscore](https://github.com/amdjs/underscore).

## 0.x API Changes

Usage changes that may break functionality for those upgrading along the 0.x
series are documented here. See [the Changelog](https://github.com/jwhitley/requirejs-rails/blob/master/CHANGELOG.md) for the full
list of feature additions, bugfixes, etc.

### v0.9.2

- Support for Rails 4.

### v0.9.0

- The upgrade to RequireJS and r.js 2.0 includes changes that will break some
  apps.

### v0.5.1

- `requirejs_include_tag` now generates a data-main attribute if given an argument, ala:

    ```erb
    <%= requirejs_include_tag "application" %>
    ```

    This usage is preferred to using a separate
    `javascript_include_tag`, which will produce errors from require.js or
    r.js if the included script uses define anonymously, or not at all.

### v0.5.0

- `application.js` is configured as the default top-level module for r.js builds.
- It is no longer necessary or desirable to specify `baseUrl` explicitly in the configuration.
- Users should migrate application configuration previously in `application.js` (ala `require.config(...)`) to `config/requirejs.yml`



## TODOs

Please check out [our GitHub issues page](https://github.com/jwhitley/requirejs-rails/issues)
to see what's upcoming and to file feature requests and bug reports.

----

Copyright 2011-2014 John Whitley.  See the file MIT-LICENSE for terms.
