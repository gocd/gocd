# Sprockets ES6

**Experimental**

A Sprockets transformer that converts ES6 code into vanilla ES5 with [Babel JS](https://babeljs.io).

## Usage

``` ruby
# Gemfile
gem 'sprockets', '>= 3.0.0'
gem 'sprockets-es6'
```


``` ruby
require 'sprockets/es6'
```

``` js
// app.es6

let square = (x) => x * x

class Animal {
  constructor(name) {
    this.name = name
  }
}
```

## Releases

This plugin is primarily experimental and will never reach a stable 1.0. The
purpose is to test out BabelJS features on Sprockets 3.x and include it by default
in Sprockets 4.x.

## Asset manifests required for precompiling

`.es6` won't work directly with `config.assets.precompile = %w( foo.es6 )` for annoying compatibility reasons with Sprockets 2.x. Besides, you should look into moving away from `config.assets.precompile` and using manifests instead. See [Sprockets 3.x UPGRADING guide](https://github.com/rails/sprockets/blob/master/UPGRADING.md#preference-for-asset-manifest-and-links).
