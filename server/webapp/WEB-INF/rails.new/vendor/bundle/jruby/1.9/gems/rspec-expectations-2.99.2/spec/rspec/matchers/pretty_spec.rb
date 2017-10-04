require 'spec_helper'

module RSpec
  module Matchers
    describe Pretty do
      include Pretty

      describe "#_pretty_print" do
        it 'is deprecated' do
          expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /_pretty_print/)
          _pretty_print([1, 2])
        end
      end

      describe "#expected_to_sentence" do
        it 'is deprecated' do
          expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /expected_to_sentence/)
          expected_to_sentence
        end
      end
    end
  end
end
