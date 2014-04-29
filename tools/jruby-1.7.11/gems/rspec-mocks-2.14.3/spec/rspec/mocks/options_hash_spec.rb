require 'spec_helper'

module RSpec
  module Mocks
    describe "calling :should_receive with an options hash" do
      it "reports the file and line submitted with :expected_from" do
        begin
          mock = RSpec::Mocks::Mock.new("a mock")
          mock.should_receive(:message, :expected_from => "/path/to/blah.ext:37")
          verify mock
        rescue Exception => e
        ensure
          expect(e.backtrace.to_s).to match(/\/path\/to\/blah.ext:37/m)
        end
      end

      it "uses the message supplied with :message" do
        expect {
          m = RSpec::Mocks::Mock.new("a mock")
          m.should_receive(:message, :message => "recebi nada")
          verify m
        }.to raise_error("recebi nada")
      end

      it "uses the message supplied with :message after a similar stub" do
        expect {
          m = RSpec::Mocks::Mock.new("a mock")
          m.stub(:message)
          m.should_receive(:message, :message => "from mock")
          verify m
        }.to raise_error("from mock")
      end
    end
  end
end
