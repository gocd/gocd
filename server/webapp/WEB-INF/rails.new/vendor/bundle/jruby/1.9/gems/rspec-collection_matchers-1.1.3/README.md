# RSpec::CollectionMatchers [![Build Status](https://secure.travis-ci.org/rspec/rspec-collection_matchers.svg?branch=master)](http://travis-ci.org/rspec/rspec-collection_matchers)

RSpec::CollectionMatchers lets you express expected outcomes on collections
of an object in an example.

    expect(account.shopping_cart).to have_exactly(3).items

## Install

Add this line to your application's Gemfile:

    gem 'rspec-collection_matchers'

And then execute:

    $ bundle

Or install it yourself as:

    $ gem install rspec-collection_matchers

## Basic usage

First of all, you need to require rspec-collection matchers. Add the following line to your `spec_helper.rb`:

```ruby
require 'rspec/collection_matchers'
```

Using `rspec-collection_matchers` you can match the number of items in a
collection directly, e.g.:

```ruby
it 'matches number of items in a collection' do
  expect([1,2,3]).to have_at_least(3).items
end
```

You can also match the number of items returned by a method on an object, e.g.:

```ruby
class Cart
  def initialize(*products)
    @products = products
  end
  attr_reader :products
end

it 'matches number of items returned from a method' do
  cart = Cart.new('product a', 'product b')
  expect(cart).to have_at_most(2).products
end
```

The last line of the example expresses an expected outcome:
if `cart.products.size <= 2` then the example passes, otherwise it fails with a message like:

    expected at most 2 products, got 3

## Available matchers

```ruby
expect(collection).to have(n).items
expect(collection).to have_exactly(n).items
expect(collection).to have_at_most(n).items
expect(collection).to have_at_least(n).items
```

## See also

* [http://github.com/rspec/rspec](http://github.com/rspec/rspec)
* [http://github.com/rspec/rspec-core](http://github.com/rspec/rspec-core)
* [http://github.com/rspec/rspec-expectations](http://github.com/rspec/rspec-expectations)
