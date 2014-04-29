require 'spec_helper'

module RSpec
  module Matchers
    describe "expect(actual).to be_close(expected, delta)" do
      before(:each) do
        allow(RSpec).to receive(:deprecate)
      end

      it "is deprecated" do
        expect(RSpec).to receive(:deprecate).with(/be_close.*/, :replacement => "be_within(0.5).of(3.0)")
        be_close(3.0, 0.5)
      end

      it "delegates to be_within(delta).of(expected)" do
        should_receive(:be_within).with(0.5).and_return( be_within_matcher = double )
        be_within_matcher.should_receive(:of).with(3.0)
        be_close(3.0, 0.5)
      end
    end
  end
end
