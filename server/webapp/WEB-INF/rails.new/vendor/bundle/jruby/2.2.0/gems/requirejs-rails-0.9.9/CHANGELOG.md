<!-- Marked Style: GitHub -->

## requirejs-rails changelog

### v0.9.9

- Fix an asset digesting regression in #238 (credit @jonhyman).
- Update `require.js` and `r.js` to `2.1.19`.

### v0.9.8

- Fix a bad regression and yank v0.9.7.

### v0.9.7

- Fix a corner case where modules could appear in the build config's `paths`.
- Fix regressions introduced in v0.9.6.
- Allow protocol relative urls for CDNs in the build config's `paths` (credit @remybach).

### v0.9.6

- Update `require.js` and `r.js` to `2.1.17`, and `almond.js` to `0.3.1`.
- Improve handling of Bower packages.
- Make a small fix for Sprockets 3 support.

### v0.9.5

- Update `require.js` and `r.js` to `2.1.15`, and `almond.js` to `0.3.0`.
- Remove `data-main` attribute generation from `requirejs_include_tag` and replicate its functionality with an explicit
  `require` call. This paves the way for proper almond support.
- Instead of disabling JS compression everywhere, only do so when staging `r.js` input files. This enables `require.js`
  itself to be compiled (or not) by the asset pipeline, like any other asset.
- Add support for `bower.json` modules.

### v0.9.4

- Restore compression of non-module JS assets if specified, including `require.js` itself.
- Update `require.js` to `2.1.14`.

### v0.9.3

- Add detection and proper handling of Bower-based modules.
- Update `require.js` to `2.1.11` and `almond.js` to `0.2.9`.

### v0.9.2

- Update `require.js` to `2.1.10` and `almond.js` to `0.2.5`.
- Rails 4 support.

### v0.9.1

- Updated to to require.js \ r.js 2.1.2, and almond 0.2.3

### v0.9.0

- **BREAKING CHANGE**: Upgrade RequireJS and r.js to v2.0.0
- order.js was removed. See the new [RequireJS 2.0 shim config](https://github.com/jrburke/requirejs/wiki/Upgrading-to-RequireJS-2.0#wiki-shim).
- Build support for `empty:` is now handled by r.js 2.0 natively.

### v0.8.2

- Fix for `requirejs_include_tag` error when `config/requirejs.yml` has no
  `paths` key.  Thanks to @JustinLove for the issue and failing test!

### v0.8.0

- Build will now substitute `empty:` for the right-hand side of
  `config/requirejs.yml` paths entries that are URLs.
- Documented how to configure assets hosted on a CDN.

### v0.7.3

- Upgrade RequireJS and r.js to v1.0.8

### v0.7.2

- Add require.js to config.assets.precompile in all environments.  Closes #45.
  This change allows builds to work in Rails environments other than
  'production', e.g. 'staging'.  Thanks to @hollow for the fix.

### v0.7.1

- Liberalize asset path filtering.  `0.7.0` added filtering on the logical
  asset path which was too aggressive in that only `.js` files were allowed in
  builds.  The RequireJS config variable `logical_asset_filter` has been
  added, which allows `.js`, `.html`, `.txt` files by default and is user
  configurable.

### v0.7.0

- Support for [almond](https://github.com/jrburke/almond) via
  `config.requirejs.loader = :almond` in application.rb.
- Builds with `config.assets.initialize_on_precompile = false` now work.
  This supports building on Heroku, builds with Devise, etc. all of
  which require that setting.
- We should now play much better with existing Rails Engines that
  leverage the asset pipeline for their needs.  Thanks to @hollow for the
  patch.

### v0.6.1

- Fix regression in production env when `paths` specified in requirejs.yml.

### v0.6.0

**NOTE:** Upgrade to 0.6.1! This was yanked due to a regression.

- We now generate a paths config to hit digested assets when needed (in
  `production` or when `config.assets.digest` is true). Fixes #20.
- Support for generating additional data attributes on the require.js script
  tag via `requirejs_include_tag`. See [README](README.md) for details. Closes 
  pull request #32; thanks to @hollow for the submission!

### v0.5.6

- Upgrade to RequireJS and r.js 1.0.7

### v0.5.5

- Support for Rails 3.2.x.  Rails 3.1.x is also supported by this release.

### v0.5.4

- Upgrade to RequireJS and r.js 1.0.5
- Pull request #31, closes #30.  Thanks @karelim!

### v0.5.3

- Upgrade to RequireJS and r.js 1.0.4
- Pulled #22, fix for asset compliation failure with no config/requirejs.yml.
  Thanks @arehberg!

### v0.5.2

- Upgrade to RequireJS and r.js 1.0.3

### v0.5.1

- This is a quick turn to fix an issue that could trigger an Anonymous mismatched define() error from require.js and/or r.js.

    The preferred way to use the helper tag is now with an argument, like
    so:

    ```erb
    <%= requirejs_include_tag "application" %>
    ```

    This usage ensures that the above helper will correctly generate a
    data-main attribute for the script tag.  The requirejs_include_tag
    helper still works without an argument, and won't generate data-main
    in that case.

    Thanks to Andrew de Andrade for the catch.

### v0.5.0

- Precompilation via `rake assets:precompile` is now implemented.
- gem configuration via application.js is deprecated.
- Application-specific require.js configuration lives in `config/requirejs.yml`.
- See [README](README.md) for updated usage details.

### v0.0.2

- Fixed stupid problems with Rails::Engine instantiation.
- Test improvements
- Upgrade to RequireJS 1.0.2

### v0.0.1

- Birthday!
- This gem makes `require.js` and the `order.js` plugin available to the Rails 3 Asset Pipeline.
