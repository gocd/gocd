require 'spec_helper'

describe Jasmine::Result do
  describe "data accessors" do
    it "exposes failed expectations" do
      result = Jasmine::Result.new(failing_raw_result)
      expectation = result.failed_expectations[0]
      expectation.message.should == "a failure message"
      expectation.stack.should == "a stack trace"
    end

    it "exposes only the last 7 lines of the stack trace" do
      raw_result = failing_raw_result
      raw_result["failedExpectations"][0]["stack"] = "1\n2\n3\n4\n5\n6\n7\n8\n9"

      result = Jasmine::Result.new(raw_result)
      expectation = result.failed_expectations[0].stack
      expectation.should match(/1/)
      expectation.should match(/7/)
      expectation.should_not match(/8/)
      expectation.should_not match(/9/)
    end

    it "exposes the full stack trace when configured" do
      stack_trace = "1\n2\n3\n4\n5\n6\n7\n8\n9"
      raw_result = failing_raw_result
      raw_result["failedExpectations"][0]["stack"] = stack_trace

      result = Jasmine::Result.new(raw_result.merge!("show_full_stack_trace" => true))
      expectation = result.failed_expectations[0].stack
      expectation.should == stack_trace
    end

    it "handles failed specs with no stack trace" do
      raw_result = failing_result_no_stack_trace

      result = Jasmine::Result.new(raw_result)
      expectation = result.failed_expectations[0].stack
      expectation.should match(/No stack/)
    end

  end
end

