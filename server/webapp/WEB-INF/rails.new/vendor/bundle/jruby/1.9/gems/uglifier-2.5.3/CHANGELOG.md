## 2.5.3 (18 July 2014)

- no changes

## 2.5.2 (18 July 2014)

- update UglifyJS to 2.4.15

## 2.5.1 (13 June 2014)

- update UglifyJS to 2.4.14

## 2.5.0 (15 March 2014)

- update UglifyJS to 2.4.13
- process Angular @ngInject annotations
- add keep_fargs option
- change `ascii_only` default to true

## 2.4.0 (19 December 2013)

- update UglifyJS to 2.4.8
- add drop_console compress option

## 2.3.3 (12 December 2013)

- update UglifyJS to 2.4.7

## 2.3.2 (1 December 2013)

- update UglifyJS to 2.4.6
- document missing mangler and output options

## 2.3.1 (8 November 2013)

 - update UglifyJS to 2.4.3

## 2.3.0 (26 October 2013)

  - use JSON gem instead of multi_json
  - update UglifyJS to 2.4.1
  - fix issues with some Unicode JS identifiers (#47, #58)

## 2.2.1 (28 August 2013)

  - fix IE8 compatibility

## 2.2.0 (25 August 2013)

  - update UglifyJS to 2.4.0
  - add `negate_iife` compressor option
  - escape null characters as \x00, so that null followed by number isn't
    interpreted as octal (#47)

## 2.1.2 (7 July 2013)

  - update UglifyJS to 2.3.6

## 2.1.1 (18 May 2013)

  - fix JScript compatibility
  - update UglifyJS to 2.3.4

## 2.1.0 (8 May 2013)

  - update to UglifyJS 2.3.0
  - add enclose and screw_ie8 options

## 2.0.1 (6 April 2013)

  - fix compatibility with Sprockets 2.9.0

## 2.0.0 (6 April 2013)

This release is backwards incompatible for JS compressor options.

  - update UglifyJS to 2.2.5
  - change compressor arguments to align with UglifyJS2
  - `compile_with_map`: generate source maps for minified code
