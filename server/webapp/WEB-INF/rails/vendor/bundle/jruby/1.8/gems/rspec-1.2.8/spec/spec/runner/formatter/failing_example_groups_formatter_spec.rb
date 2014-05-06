require File.dirname(__FILE__) + '/../../../spec_helper'
require 'spec/runner/formatter/failing_example_groups_formatter'

module Spec
  module Runner
    module Formatter
      describe FailingExampleGroupsFormatter do
        attr_reader :example_group, :formatter, :io

        before(:each) do
          @io = StringIO.new
          options = mock('options')
          @formatter = FailingExampleGroupsFormatter.new(options, io)
          @example_group = Class.new(::Spec::Example::ExampleGroup)
        end
        
        it "should add example name for each failure" do
          formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(Class.new(::Spec::Example::ExampleGroupDouble).describe("b 1")))
          formatter.example_failed("e 1", nil, Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(Class.new(::Spec::Example::ExampleGroupDouble).describe("b 2")))
          formatter.example_failed("e 2", nil, Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          formatter.example_failed("e 3", nil, Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          io.string.should include("b 1")
          io.string.should include("b 2")
        end

        it "should delimit ExampleGroup superclass descriptions with :" do
          parent_example_group = Class.new(example_group).describe("Parent")
          child_example_group = Class.new(parent_example_group).describe("#child_method")
          grand_child_example_group = Class.new(child_example_group).describe("GrandChild")

          formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(grand_child_example_group))
          formatter.example_failed("failure", nil, ::Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          io.string.should == "Parent#child_method GrandChild\n"
        end

        it "should remove druby url, which is used by Spec::Distributed" do
          @formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(Class.new(::Spec::Example::ExampleGroupDouble).describe("something something (druby://99.99.99.99:99)")))
          @formatter.example_failed("e 1", nil, ::Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          io.string.should == "something something\n"
        end
      end
    end
  end
end
