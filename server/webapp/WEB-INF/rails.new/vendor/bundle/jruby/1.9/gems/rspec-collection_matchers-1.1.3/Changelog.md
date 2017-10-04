### Development

Bug Fixes:

* Define `to_ary` on Ruby 1.9.2 to fix issue when diffing in compound expectations
  (Jon Rowe, #34)
* Prevent warning being issued due to incorrect definition of `respond_to`.
  (Matt Whipple, #33)

### 1.1.2 / 2014-11-11

[full changelog](http://github.com/rspec/rspec-collection_matchers/compare/v1.1.1...v1.1.2)

Bug Fixes:

* Fix bug of uninitialized constant RSpec::Expectations (Related to #20)

### 1.1.1 / 2014-11-10

[full changelog](http://github.com/rspec/rspec-collection_matchers/compare/v1.1.0...v1.1.1)

Bug Fixes:

* Remove virtual dependency on rspec-core (Thanks @jscheid for reporting that, #20)

### 1.1.0 / 2014-11-10

[full changelog](http://github.com/rspec/rspec-collection_matchers/compare/v1.0.0...v1.1.0)

Enhancements:

* Make matchers composable on RSpec 3 and above. (Johnson Denen, #19)

### 1.0.0 / 2014-06-09

[full changelog](http://github.com/rspec/rspec-collection_matchers/compare/v0.0.4...v1.0.0)

### 0.0.4 / 2014-04-24

[full changelog](http://github.com/rspec/rspec-collection_matchers/compare/v0.0.3...v0.0.4)

Enhancements:

* Add Rails extension `have(n).errors_on(:whatever)` (Bradley Schaefer)

### 0.0.3 / 2014-02-16

[full changelog](http://github.com/rspec/rspec-collection_matchers/compare/v0.0.2...v0.0.3)

Enhancements:

* Update to latest RSpec 3 matcher API. (Myron Marston)

Bug Fixes:

* Raise an error when you attempt to use matcher against an `Integer`
  which previously would have incorrectly used a `#size` of 8. (Kassio Borges)
