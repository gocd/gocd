## [3.0.2] - 2017-10-04

### Added

- Allow dev server connect timeout (in seconds) to be configurable, default: 0.01

```rb
# Change to 1s
Webpacker.dev_server.connect_timeout = 1
```

- Restrict the source maps generated in production [#770](https://github.com/rails/webpacker/pull/770)

- Binstubs [#833](https://github.com/rails/webpacker/pull/833)

- Allow dev server settings to be overriden by env variables [#843](https://github.com/rails/webpacker/pull/843)

- A new `lookup` method to manifest to perform lookup without raise and return `nil`

```rb
Webpacker.manifest.lookup('foo.js')
# => nil
Webpacker.manifest.lookup!('foo.js')
# => raises Webpacker::Manifest::MissingEntryError
```

- Catch all exceptions in `DevServer.running?` and return false [#878](https://github.com/rails/webpacker/pull/878)

### Removed

- Inline CLI args for dev server binstub, use env variables instead

- Coffeescript as core dependency. You have to manually add coffeescript now, if you are using
it in your app.

```bash
yarn add coffeescript@1.12.7

# OR coffeescript 2.0
yarn add coffeescript
```

## [3.0.1] - 2017-09-01

### Fixed

- Missing `node_modules/.bin/*` files by bumping minimum Yarn version to 0.25.2 [#727](https://github.com/rails/webpacker/pull/727)

- `webpacker:compile` task so that fails properly when webpack compilation fails [#728](https://github.com/rails/webpacker/pull/728)

- Rack dev server proxy middleware when served under another proxy (example: pow), which uses `HTTP_X_FORWARDED_HOST` header resulting in `404` for webpacker assets

- Make sure tagged logger works with rails < 5 [#716](https://github.com/rails/webpacker/pull/716)

### Added

- Allow webpack dev server listen host/ip to be configurable using additional `--listen-host` option

  ```bash
  ./bin/webpack-dev-server --listen-host 0.0.0.0 --host localhost
  ```

### Removed

- `watchContentBase` from devServer config so it doesn't unncessarily trigger
live reload when manifest changes. If you have applied this workaround from [#724](https://github.com/rails/webpacker/issues/724), please revert the change from `config/webpack/development.js` since this is now fixed.


## [3.0.0] - 2017-08-30

### Added

- `resolved_paths` option to allow adding additional paths webpack should lookup when resolving modules

```yml
  # config/webpacker.yml
  # Additional paths webpack should lookup modules
  resolved_paths: [] # empty by default
```

- `Webpacker::Compiler.fresh?` and `Webpacker::Compiler.stale?` answer the question of whether compilation is needed.
  The old `Webpacker::Compiler.compile?` predicate is deprecated.

- Dev server config class that exposes config options through singleton.

  ```rb
  Webpacker.dev_server.running?
  ```

- Rack middleware proxies webpacker requests to dev server so we can always serve from same-origin and the lookup works out of the box - no more paths prefixing

- `env` attribute on `Webpacker::Compiler` allows setting custom environment variables that the compilation is being run with

  ```rb
  Webpacker::Compiler.env['FRONTEND_API_KEY'] = 'your_secret_key'
  ```

### Breaking changes

**Note:** requires running `bundle exec rails webpacker:install`

`config/webpack/**/*.js`:

- The majority of this config moved to the [@rails/webpacker npm package](https://www.npmjs.com/package/@rails/webpacker). `webpacker:install` only creates `config/webpack/{environment,development,test,production}.js` now so if you're upgrading from a previous version you can remove all other files.

`webpacker.yml`:

- Move dev-server config options under defaults so it's transparently available in all environments

- Add new `HMR` option for hot-module-replacement

- Add HTTPS

### Removed

- Host info from manifest.json, now looks like this:

  ```json
  {
    "hello_react.js": "/packs/hello_react.js"
  }
  ```

### Fixed

- Update `webpack-dev-server.tt` to respect RAILS_ENV and NODE_ENV values [#502](https://github.com/rails/webpacker/issues/502)
- Use `0.0.0.0` as default listen address for `webpack-dev-server`
- Serve assets using `localhost` from dev server - [#424](https://github.com/rails/webpacker/issues/424)

```yml
  dev_server:
    host: localhost
```

- On Windows, `ruby bin/webpacker` and `ruby bin/webpacker-dev-server` will now bypass yarn, and execute via `node_modules/.bin` directly - [#584](https://github.com/rails/webpacker/pull/584)

### Breaking changes

- Add `compile` and `cache_path` options to `config/webpacker.yml` for configuring lazy compilation of packs when a file under tracked paths is changed [#503](https://github.com/rails/webpacker/pull/503). To enable expected behavior, update `config/webpacker.yml`:

  ```yaml
    default: &default
      cache_path: tmp/cache/webpacker
    test:
      compile: true

    development:
      compile: true

    production:
      compile: false
  ```

- Make test compilation cacheable and configurable so that the lazy compilation
only triggers if files are changed under tracked paths.
Following paths are watched by default -

  ```rb
    ["app/javascript/**/*", "yarn.lock", "package.json", "config/webpack/**/*"]
  ```

  To add more paths:

  ```rb
  # config/initializers/webpacker.rb or config/application.rb
  Webpacker::Compiler.watched_paths << 'bower_components'
  ```

## [2.0] - 2017-05-24

### Fixed
- Update `.babelrc` to fix compilation issues - [#306](https://github.com/rails/webpacker/issues/306)

- Duplicated asset hosts - [#320](https://github.com/rails/webpacker/issues/320), [#397](https://github.com/rails/webpacker/pull/397)

- Missing asset host when defined as a `Proc` or on `ActionController::Base.asset_host` directly - [#397](https://github.com/rails/webpacker/pull/397)

- Incorrect asset host when running `webpacker:compile` or `bin/webpack` in development mode - [#397](https://github.com/rails/webpacker/pull/397)

- Update `webpacker:compile` task to use `stdout` and `stderr` for better logging - [#395](https://github.com/rails/webpacker/issues/395)

- ARGV support for `webpack-dev-server` - [#286](https://github.com/rails/webpacker/issues/286)


### Added
- [Elm](http://elm-lang.org) support. You can now add Elm support via the following methods:
  - New app: `rails new <app> --webpack=elm`
  - Within an existing app: `rails webpacker:install:elm`

- Support for custom `public_output_path` paths independent of `source_entry_path` in `config/webpacker.yml`. `output` is also now relative to `public/`. - [#397](https://github.com/rails/webpacker/pull/397)

    Before (compile to `public/packs`):
    ```yaml
      source_entry_path: packs
      public_output_path: packs
    ```
    After (compile to `public/sweet/js`):
    ```yaml
      source_entry_path: packs
      public_output_path: sweet/js
    ```

- `https` option to use `https` mode, particularly on platforms like - https://community.c9.io/t/running-a-rails-app/1615 or locally - [#176](https://github.com/rails/webpacker/issues/176)

- [Babel] Dynamic import() and Class Fields and Static Properties babel plugin to `.babelrc`

```json
{
  "presets": [
    ["env", {
      "modules": false,
      "targets": {
        "browsers": "> 1%",
        "uglify": true
      },
      "useBuiltIns": true
    }]
  ],

  "plugins": [
    "syntax-dynamic-import",
    "transform-class-properties", { "spec": true }
  ]
}
```

- Source-map support for production bundle


#### Breaking Change

- Consolidate and flatten `paths.yml` and `development.server.yml` config into one file - `config/webpacker.yml` - [#403](https://github.com/rails/webpacker/pull/403). This is a breaking change and requires you to re-install webpacker and cleanup old configuration files.

  ```bash
  bundle update webpacker
  bundle exec rails webpacker:install

  # Remove old/unused configuration files
  rm config/webpack/paths.yml
  rm config/webpack/development.server.yml
  rm config/webpack/development.server.js
  ```

  __Warning__: For now you also have to add a pattern in `.gitignore` by hand.
  ```diff
   /public/packs
  +/public/packs-test
   /node_modules
   ```

## [1.2] - 2017-04-27
Some of the changes made requires you to run below commands to install new changes.

```
bundle update webpacker
bundle exec rails webpacker:install
```


### Fixed
- Support Spring - [#205](https://github.com/rails/webpacker/issues/205)

  ```ruby
  Spring.after_fork { Webpacker.bootstrap } if defined?(Spring)
  ```
- Check node version and yarn before installing webpacker - [#217](https://github.com/rails/webpacker/issues/217)

- Include webpacker helper to views - [#172](https://github.com/rails/webpacker/issues/172)

- Webpacker installer on windows - [#245](https://github.com/rails/webpacker/issues/245)

- Yarn duplication - [#278](https://github.com/rails/webpacker/issues/278)

- Add back Spring for `rails-erb-loader` - [#216](https://github.com/rails/webpacker/issues/216)

- Move babel presets and plugins to .babelrc - [#202](https://github.com/rails/webpacker/issues/202)


### Added
- A changelog - [#211](https://github.com/rails/webpacker/issues/211)
- Minimize CSS assets - [#218](https://github.com/rails/webpacker/issues/218)
- Pack namespacing support - [#201](https://github.com/rails/webpacker/pull/201)

  For example:
  ```
  app/javascript/packs/admin/hello_vue.js
  app/javascript/packs/admin/hello.vue
  app/javascript/packs/hello_vue.js
  app/javascript/packs/hello.vue
  ```
- Add tree-shaking support - [#250](https://github.com/rails/webpacker/pull/250)
- Add initial test case by @kimquy [#259](https://github.com/rails/webpacker/pull/259)
- Compile assets before test:controllers and test:system


### Removed
- Webpack watcher - [#295](https://github.com/rails/webpacker/pull/295)


## [1.1] - 2017-03-24

This release requires you to run below commands to install new features.

```
bundle update webpacker
bundle exec rails webpacker:install

# if installed react, vue or angular
bundle exec rails webpacker:install:[react, angular, vue]
```

### Added (breaking changes)
- Static assets support - [#153](https://github.com/rails/webpacker/pull/153)
- Advanced webpack configuration - [#153](https://github.com/rails/webpacker/pull/153)


### Removed

```rb
config.x.webpacker[:digesting] = true
```
