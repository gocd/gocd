# Built-in Matchers

Here is a list of matchers that ship with rspec-expectations. Each matcher
can be used with `should` or `should_not` e.g.

    result.should eq(3)
    list.should_not be_empty

## Object identity

    actual.should be(expected) # passes if actual.equal?(expected)
    
## Object equivalence

    actual.should eq(expected) # passes if actual == expected

## Optional APIs for identity/equivalence

    actual.should == expected     # passes if actual == expected
    actual.should eql(expected)   # passes if actual.eql?(expected)
    actual.should equal(expected) # passes if actual.equal?(expected)

    # NOTE: this can't work in Ruby 1.8, so we don't support it at all:
    #   actual.should != expected
    # The reason is that Ruby 1.8 parses it as:
    #   !(actual.should.==(expected)),
    # so by the time RSpec sees it it has no way to know that it's
    # been negated. Use either of these instead:
    #   actual.should_not eq(expected)
    #   actual.should_not == expected

## Comparisons

    actual.should be >  expected
    actual.should be >= expected
    actual.should be <= expected
    actual.should be <  expected
    actual.should =~ /expression/
    actual.should match(/expression/)
    actual.should be_within(delta).of(expected)

## Types/classes

    actual.should be_instance_of(expected)
    actual.should be_kind_of(expected)

## Truthiness and existentialism

    actual.should be_true  # passes if actual is truthy (not nil or false)
    actual.should be_false # passes if actual is falsy (nil or false)
    actual.should be_nil   # passes if actual is nil
    actual.should be       # passes if actual is truthy (not nil or false)

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

    actual.should be_xxx         # passes if actual.xxx?
    actual.should have_xxx(:arg) # passes if actual.has_xxx?(:arg)

### Examples

    [].should be_empty # passes because [].empty? returns true
    { :a => 1 }.should have_key(:a) # passes because the hash has the key :a

## Collection membership

    actual.should include(expected)

### Examples

    [1,2,3].should include(1)
    [1,2,3].should include(1, 2)
    {:a => 'b'}.should include(:a => 'b')
    "this string".should include("is str")

## Ranges (1.9 only)

    (1..10).should cover(3)
