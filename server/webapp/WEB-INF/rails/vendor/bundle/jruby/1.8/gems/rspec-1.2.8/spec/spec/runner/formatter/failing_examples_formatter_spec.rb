require File.dirname(__FILE__) + '/../../../spec_helper'
require 'spec/runner/formatter/failing_examples_formatter'

module Spec
  module Runner
    module Formatter
      describe FailingExamplesFormatter do
        before(:each) do
          @io = StringIO.new
          options = mock('options')
          @formatter = FailingExamplesFormatter.new(options, @io)
        end

        it "should add example name for each failure" do
          example_group_1 = Class.new(::Spec::Example::ExampleGroupDouble).describe("A")
          example_group_2 = Class.new(example_group_1).describe("B")

          @formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(example_group_1))
          @formatter.example_failed(example_group_1.it("a1"){}, nil, ::Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          @formatter.example_group_started(Spec::Example::ExampleGroupProxy.new(example_group_2))
          @formatter.example_failed(example_group_2.it("b2"){}, nil, ::Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          @formatter.example_failed(example_group_2.it("b3"){}, nil, ::Spec::Runner::Reporter::Failure.new("g", nil, RuntimeError.new))
          @io.string.should eql(<<-EOF
A a1
A B b2
A B b3
EOF
)
        end
      end
    end
  end
end
