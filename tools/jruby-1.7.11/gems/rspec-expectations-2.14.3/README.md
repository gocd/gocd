# RSpec Expectations [![Build Status](https://secure.travis-ci.org/rspec/rspec-expectations.png?branch=master)](http://travis-ci.org/rspec/rspec-expectations) [![Code Climate](https://codeclimate.com/github/rspec/rspec-expectations.png)](https://codeclimate.com/github/rspec/rspec-expectations)

RSpec::Expectations lets you express expected outcomes on an object in an
example.

    expect(account.balance).to eq(Money.new(37.42, :USD))

## Install

If you want to use rspec-expectations with rspec, just install the rspec gem
and RubyGems will also install rspec-expectations for you (along with
rspec-core and rspec-mocks):

    gem install rspec

If you want to use rspec-expectations with another tool, like Test::Unit,
Minitest, or Cucumber, you can install it directly:

    gem install rspec-expectations

## Basic usage

Here's an example using rspec-core:

```ruby
describe Order do
  it "sums the prices of the items in its line items" do
    order = Order.new
    order.add_entry(LineItem.new(:item => Item.new(
      :price => Money.new(1.11, :USD)
    )))
    order.add_entry(LineItem.new(:item => Item.new(
      :price => Money.new(2.22, :USD),
      :quantity => 2
    )))
    expect(order.total).to eq(Money.new(5.55, :USD))
  end
end
```

The `describe` and `it` methods come from rspec-core.  The `Order`, `LineItem`, `Item` and `Money` classes would be from _your_ code. The last line of the example
expresses an expected outcome. If `order.total == Money.new(5.55, :USD)`, then
the example passes. If not, it fails with a message like:

    expected: #<Money @value=5.55 @currency=:USD>
         got: #<Money @value=1.11 @currency=:USD>

## Built-in matchers

### Equivalence

```ruby
expect(actual).to eq(expected)  # passes if actual == expected
expect(actual).to eql(expected) # passes if actual.eql?(expected)
```

Note: The new `expect` syntax no longer supports `==` matcher.

### Identity

```ruby
expect(actual).to be(expected)    # passes if actual.equal?(expected)
expect(actual).to equal(expected) # passes if actual.equal?(expected)
```

### Comparisons

```ruby
expect(actual).to be >  expected
expect(actual).to be >= expected
expect(actual).to be <= expected
expect(actual).to be <  expected
expect(actual).to be_within(delta).of(expected)
```

### Regular expressions

```ruby
expect(actual).to match(/expression/)
```

Note: The new `expect` syntax no longer supports `=~` matcher.

### Types/classes

```ruby
expect(actual).to be_an_instance_of(expected)
expect(actual).to be_a_kind_of(expected)
```

### Truthiness

```ruby
expect(actual).to be_true  # passes if actual is truthy (not nil or false)
expect(actual).to be_false # passes if actual is falsy (nil or false)
expect(actual).to be_nil   # passes if actual is nil
```

### Expecting errors

```ruby
expect { ... }.to raise_error
expect { ... }.to raise_error(ErrorClass)
expect { ... }.to raise_error("message")
expect { ... }.to raise_error(ErrorClass, "message")
```

### Expecting throws

```ruby
expect { ... }.to throw_symbol
expect { ... }.to throw_symbol(:symbol)
expect { ... }.to throw_symbol(:symbol, 'value')
```

### Yielding

```ruby
expect { |b| 5.tap(&b) }.to yield_control # passes regardless of yielded args

expect { |b| yield_if_true(true, &b) }.to yield_with_no_args # passes only if no args are yielded

expect { |b| 5.tap(&b) }.to yield_with_args(5)
expect { |b| 5.tap(&b) }.to yield_with_args(Fixnum)
expect { |b| "a string".tap(&b) }.to yield_with_args(/str/)

expect { |b| [1, 2, 3].each(&b) }.to yield_successive_args(1, 2, 3)
expect { |b| { :a => 1, :b => 2 }.each(&b) }.to yield_successive_args([:a, 1], [:b, 2])
```

### Predicate matchers

```ruby
expect(actual).to be_xxx         # passes if actual.xxx?
expect(actual).to have_xxx(:arg) # passes if actual.has_xxx?(:arg)
```

### Ranges (Ruby >= 1.9 only)

```ruby
expect(1..10).to cover(3)
```

### Collection membership

```ruby
expect(actual).to include(expected)
expect(actual).to start_with(expected)
expect(actual).to end_with(expected)
```

#### Examples

```ruby
expect([1,2,3]).to include(1)
expect([1,2,3]).to include(1, 2)
expect([1,2,3]).to start_with(1)
expect([1,2,3]).to start_with(1,2)
expect([1,2,3]).to end_with(3)
expect([1,2,3]).to end_with(2,3)
expect({:a => 'b'}).to include(:a => 'b')
expect("this string").to include("is str")
expect("this string").to start_with("this")
expect("this string").to end_with("ring")
```

## `should` syntax

In addition to the `expect` syntax, rspec-expectations continues to support the
`should` syntax:

```ruby
actual.should eq expected
actual.should be > 3
[1, 2, 3].should_not include 4
```

See [detailed information on the `should` syntax and its usage.](https://github.com/rspec/rspec-expectations/blob/master/Should.md)

## Also see

* [http://github.com/rspec/rspec](http://github.com/rspec/rspec)
* [http://github.com/rspec/rspec-core](http://github.com/rspec/rspec-core)
* [http://github.com/rspec/rspec-mocks](http://github.com/rspec/rspec-mocks)
