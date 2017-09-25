require "spec_helper"

module RSpec
  module Mocks

    describe ".allow_message" do
      let(:subject) { Object.new }

      it "sets up basic message allowance" do
        expect {
          ::RSpec::Mocks.allow_message(subject, :basic)
        }.to change {
          subject.respond_to?(:basic)
        }.to(true)

        expect(subject.basic).to eq(nil)
      end

      it "sets up message allowance with params and return value" do
        expect {
          ::RSpec::Mocks.allow_message(subject, :x).with(:in).and_return(:out)
        }.to change {
          subject.respond_to?(:x)
        }.to(true)

        expect(subject.x(:in)).to eq(:out)
      end

      it "supports block implementations" do
        ::RSpec::Mocks.allow_message(subject, :message) { :value }
        expect(subject.message).to eq(:value)
      end

      it "does not set an expectation that the message will be received" do
        ::RSpec::Mocks.allow_message(subject, :message)
        expect { verify subject }.not_to raise_error
      end
    end

    describe ".expect_message" do
      let(:subject) { Object.new }

      it "sets up basic message expectation, verifies as uncalled" do
        expect {
          ::RSpec::Mocks.expect_message(subject, :basic)
        }.to change {
          subject.respond_to?(:basic)
        }.to(true)

        expect { verify subject }.to raise_error(RSpec::Mocks::MockExpectationError)
      end

      it "fails if never is specified and the message is called" do
        expect {
          ::RSpec::Mocks.expect_message(subject, :foo).never
          subject.foo
        }.to raise_error(/expected.*0 times/)
      end

      it "sets up basic message expectation, verifies as called" do
        ::RSpec::Mocks.expect_message(subject, :basic)
        subject.basic
        verify subject
      end

      it "sets up message expectation with params and return value" do
        ::RSpec::Mocks.expect_message(subject, :msg).with(:in).and_return(:out)
        expect(subject.msg(:in)).to eq(:out)
        verify subject
      end

      it "accepts a block implementation for the expected message" do
        ::RSpec::Mocks.expect_message(subject, :msg) { :value }
        expect(subject.msg).to eq(:value)
        verify subject
      end

    end

  end
end
