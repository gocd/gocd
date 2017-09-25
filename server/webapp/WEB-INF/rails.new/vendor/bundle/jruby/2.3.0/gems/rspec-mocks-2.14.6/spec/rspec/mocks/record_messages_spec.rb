require 'spec_helper'

module RSpec
  module Mocks
    describe "a mock" do
      before(:each) do
        @mock = double("mock").as_null_object
      end
      it "answers false for received_message? when no messages received" do
        expect(@mock.received_message?(:message)).to be_false
      end
      it "answers true for received_message? when message received" do
        @mock.message
        expect(@mock.received_message?(:message)).to be_true
      end
      it "answers true for received_message? when message received with correct args" do
        @mock.message 1,2,3
        expect(@mock.received_message?(:message, 1,2,3)).to be_true
      end
      it "answers false for received_message? when message received with incorrect args" do
        @mock.message 1,2,3
        expect(@mock.received_message?(:message, 1,2)).to be_false
      end
    end
  end
end
