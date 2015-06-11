# CHANGELOG

## 4.0.3 (Apr 4, 2014)

* Make possible to use sprockets-rails 2.1.

## 4.0.2 (Mar 13, 2014)

* Lock sprockets version to <= 2.11.0. Fixes #191.

## 4.0.1 (Oct 15, 2013)

* Remove Post Processors from asset evaluation.

## 4.0.0 (Jun 25, 2013)

* Add support for importing ERB files.
* Remove `Sass::Rails::Compressor`. Use `Sprockets::SassCompressor` (`:sass` option) instead.
* Remove `tilt` dependency.
* Bump `sprockets-rails` to `2.0.0.rc4`.

## 4.0.0.rc1 (Apr 18, 2013)

* No changes.

## 4.0.0.beta1 (Feb 25, 2013)

* Remove `compress` option from `config.assets`. Instead, turn on
  compression for all environments except development.
* Deprecate `asset_path` and `asset_url` with two arguments.
* Add Rails 4 support.
* Drop Ruby 1.8 support.

## 3.1.5.rc.1 (Oct 16, 2011)

* Several bug fixes.
* Performance and bug fixes when compressing stylesheets.
* Update dependency to latest sass stable release to fix caching issues.

## 3.1.2 (Sept 15, 2011)

* Add `asset-data-url` helper for base-64 encoding of assets within stylesheets.
* Add explicit dependency on sprockets.

## 3.1.1 (Sept 13, 2011)

* Add explicit version dependency on tilt.
* Add MIT License.
