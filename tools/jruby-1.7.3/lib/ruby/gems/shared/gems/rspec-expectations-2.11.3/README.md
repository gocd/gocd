# RSpec Expectations [![Build Status](https://secure.travis-ci.org/rspec/rspec-expectations.png?branch=master)](http://travis-ci.org/rspec/rspec-expectations) [![Code Climate](https://codeclimate.com/badge.png)](https://codeclimate.com/github/rspec/rspec-expectations)

RSpec::Expectations lets you express expected outcomes on an object in an
example.

    account.balance.should eq(Money.new(37.42, :USD))

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

The `describe` and `it` methods come from rspec-core.  The `Order`, `LineItem`,
and `Item` classes would be from _your_ code. The last line of the example
expresses an expected outcome. If `order.total == Money.new(5.55, :USD)`, then
the example passes. If not, it fails with a message like:

    expected: #<Money @value=5.55 @currency=:USD>
         got: #<Money @value=1.11 @currency=:USD>

## Built-in matchers

### Equivalence

```ruby
actual.should eq(expected)  # passes if actual == expected
actual.should == expected   # passes if actual == expected
actual.should eql(expected) # passes if actual.eql?(expected)
```

### Identity

```ruby
actual.should be(expected)    # passes if actual.equal?(expected)
actual.should equal(expected) # passes if actual.equal?(expected)
```

### Comparisons

```ruby
actual.should be >  expected
actual.should be >= expected
actual.should be <= expected
actual.should be <  expected
actual.should be_within(delta).of(expected)
```

### Regular expressions

```ruby
actual.should =~ /expression/
actual.should match(/expression/)
```

### Types/classes

```ruby
actual.should be_an_instance_of(expected)
actual.should be_a_kind_of(expected)
```

### Truthiness

```ruby
actual.should be_true  # passes if actual is truthy (not nil or false)
actual.should be_false # passes if actual is falsy (nil or false)
actual.should be_nil   # passes if actual is nil
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
actual.should be_xxx         # passes if actual.xxx?
actual.should have_xxx(:arg) # passes if actual.has_xxx?(:arg)
```

### Ranges (Ruby >= 1.9 only)

```ruby
(1..10).should cover(3)
```

### Collection membership

```ruby
actual.should include(expected)
actual.should start_with(expected)
actual.should end_with(expected)
```

#### Examples

```ruby
[1,2,3].should include(1)
[1,2,3].should include(1, 2)
[1,2,3].should start_with(1)
[1,2,3].should start_with(1,2)
[1,2,3].should end_with(3)
[1,2,3].should end_with(2,3)
{:a => 'b'}.should include(:a => 'b')
"this string".should include("is str")
"this string".should start_with("this")
"this string".should end_with("ring")
```

## `expect` syntax

In addition to the `should` syntax, rspec-expectations supports
a new `expect` syntax as of version 2.11.0:

```ruby
expect(actual).to eq expected
expect(actual).to be > 3
expect([1, 2, 3]).to_not include 4
```

If you want your project to only use one of these syntaxes, you can
configure it:

```ruby
RSpec.configure do |config|
  config.expect_with :rspec do |c|
    c.syntax = :expect
    # or
    c.syntax = :should
    # or
    c.syntax = [:should, :expect]
  end
end
```

See
[RSpec::Expectations::Syntax#expect](http://rubydoc.info/gems/rspec-expectations/RSpec/Expectations/Syntax:expect)
for more information.

### Motivation for `expect`

We added the `expect` syntax to resolve some edge case issues, most notably
that objects whose definitions wipe out all but a few methods were throwing
`should` and `should_not` away. `expect` solves that by not monkey patching
those methods onto `Kernel` (or any global object).

See
[http://myronmars.to/n/dev-blog/2012/06/rspecs-new-expectation-syntax](http://myronmars.to/n/dev-blog/2012/06/rspecs-new-expectation-syntax)
for a detailed explanation.

### One-liners

The one-liner syntax supported by
[rspec-core](http://rubydoc.info/gems/rspec-core)  uses `should` even when
`config.syntax = :expect`. It reads better than the alternative, and does not
require a global monkey patch:

```ruby
describe User do
  it { should validate_presence_of :email }
end
```

## Also see

* [http://github.com/rspec/rspec](http://github.com/rspec/rspec)
* [http://github.com/rspec/rspec-core](http://github.com/rspec/rspec-core)
* [http://github.com/rspec/rspec-mocks](http://github.com/rspec/rspec-mocks)
