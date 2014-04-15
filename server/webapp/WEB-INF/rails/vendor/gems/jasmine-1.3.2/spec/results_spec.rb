require 'spec_helper'

describe Jasmine::Results do
  it "should be able to return suites" do
    suites = {:some => 'suite'}
    Jasmine::Results.new({}, suites, {}).suites.should == suites
  end

  it "should return a result for a particular spec id" do
    result1 = {:some => 'result'}
    result2 = {:some => 'other result'}
    raw_results = {'1' => result1, '2' => result2 }
    results = Jasmine::Results.new(raw_results, {}, {})
    results.for_spec_id('1').should == result1
    results.for_spec_id('2').should == result2
  end

  it "should return an example location for a particular string" do
    example_location1 = {:some => 'spec location'}
    example_location2 = {:some => 'other spec location'}
    example_locations = {'foo bar' => example_location1, 'baz quux' => example_location2 }
    results = Jasmine::Results.new({}, {}, example_locations)
    results.example_location_for('foo bar').should == example_location1
    results.example_location_for('baz quux').should == example_location2
  end
end

