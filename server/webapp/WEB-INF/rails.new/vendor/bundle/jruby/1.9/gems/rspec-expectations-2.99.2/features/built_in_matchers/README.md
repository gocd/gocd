# Built-in Matchers

rspec-expectations ships with a number of built-in matchers.
Each matcher can be used with `expect(..).to` or `expect(..).not_to` to define
positive and negative expectations respectively on an object. Most matchers can
also be accessed using the `(...).should` and `(...).should_not` syntax, see
[using should syntax](https://github.com/rspec/rspec-expectations/blob/master/Should.md)
for why we recommend using `expect`.

e.g.

    expect(result).to   eq(3)
    expect(list).not_to be_empty
    pi.should be > 3

## Object identity

    expect(actual).to be(expected) # passes if actual.equal?(expected)

## Object equivalence

    expect(actual).to eq(expected) # passes if actual == expected

## Optional APIs for identity/equivalence

    expect(actual).to eql(expected)   # passes if actual.eql?(expected)
    expect(actual).to equal(expected) # passes if actual.equal?(expected)

    # NOTE: `expect` does not support `==` matcher.

## Comparisons

    expect(actual).to be >  expected
    expect(actual).to be >= expected
    expect(actual).to be <= expected
    expect(actual).to be <  expected
    expect(actual).to match(/expression/)
    expect(actual).to be_within(delta).of(expected)

    # NOTE: `expect` does not support `=~` matcher.

## Types/classes

    expect(actual).to be_instance_of(expected)
    expect(actual).to be_kind_of(expected)

## Truthiness and existentialism

    expect(actual).to be_true  # passes if actual is truthy (not nil or false)
    expect(actual).to be_false # passes if actual is falsy (nil or false)
    expect(actual).to be_nil   # passes if actual is nil
    expect(actual).to be       # passes if actual is truthy (not nil or false)

## Expecting errors

    expect { ... }.to raise_error
    expect { ... }.to raise_error(ErrorClass)
    expect { ... }.to raise_error("message")
    expect { ... }.to raise_error(ErrorClass, "message")

## Expecting throws

    expect { ... }.to throw_symbol
    expect { ... }.to throw_symbol(:symbol)
    expect { ... }.to throw_symbol(:symbol, 'value')

## Predicate matchers

    expect(actual).to be_xxx         # passes if actual.xxx?
    expect(actual).to have_xxx(:arg) # passes if actual.has_xxx?(:arg)

### Examples

    expect([]).to      be_empty
    expect(:a => 1).to have_key(:a)

## Collection membership

    expect(actual).to include(expected)

### Examples

    expect([1,2,3]).to       include(1)
    expect([1,2,3]).to       include(1, 2)
    expect(:a => 'b').to     include(:a => 'b')
    expect("this string").to include("is str")

## Ranges (1.9 only)

    expect(1..10).to cover(3)
