require 'spec_helper'

module RSpec
  module Expectations
    # so our examples below can set expectations about the target
    ExpectationTarget.send(:attr_reader, :target)

    describe ExpectationTarget do
      context 'when constructed via #expect' do
        it 'constructs a new instance targetting the given argument' do
          expect(expect(7).target).to eq(7)
        end

        it 'constructs a new instance targetting the given block' do
          block = lambda {}
          expect(expect(&block).target).to be(block)
        end

        it 'raises an ArgumentError when given an argument and a block' do
          expect {
            expect(7) { }
          }.to raise_error(ArgumentError)
        end

        it 'raises a wrong number of args ArgumentError when given two args' do
          expect {
            expect(1, 2)
          }.to raise_error(ArgumentError, /wrong number of arg/)
        end

        it 'raises an ArgumentError when given neither an argument nor a block' do
          expect {
            expect
          }.to raise_error(ArgumentError)
        end

        it 'can be passed nil' do
          expect(expect(nil).target).to be_nil
        end

        it 'passes a valid positive expectation' do
          expect(5).to eq(5)
        end

        it 'passes a valid negative expectation' do
          expect(5).not_to eq(4)
        end

        it 'passes a valid negative expectation with a split infinitive' do
          expect(5).to_not eq(4)
        end

        it 'fails an invalid positive expectation' do
          expect {
            expect(5).to eq(4)
          }.to fail_with(/expected: 4.*got: 5/m)
        end

        it 'fails an invalid negative expectation' do
          message = /expected 5 not to be a kind of Fixnum/
          expect {
            expect(5).not_to be_a(Fixnum)
          }.to fail_with(message)
        end

        it 'fails an invalid negative expectation with a split infinitive' do
          message = /expected 5 not to be a kind of Fixnum/
          expect {
            expect(5).to_not be_a(Fixnum)
          }.to fail_with(message)
        end

        it 'does not support operator matchers from #to' do
          expect {
            expect(3).to == 3
          }.to raise_error(ArgumentError)
        end

        it 'does not support operator matchers from #not_to' do
          expect {
            expect(3).not_to == 4
          }.to raise_error(ArgumentError)
        end
      end

      context "when passed a block" do
        it 'can be used with a block matcher' do
          expect { }.not_to raise_error
        end

        context 'when passed a value matcher' do
          it 'issues a warning to instruct the user to use a value expression or fix the matcher (for `to`)' do
            expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /block expectation/)
            expect { }.to be_an(Object)
          end

          it 'issues a warning to instruct the user to use a value expression or fix the matcher (for `not_to`)' do
            expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /block expectation/)
            expect { }.not_to be_an(String)
          end

          it 'issues a warning to instruct the user to use a value expression or fix the matcher (for `to_not`)' do
            expect_deprecation_with_call_site(__FILE__, __LINE__ + 1, /block expectation/)
            expect { }.to_not be_an(String)
          end

          it 'assumes a custom matcher that does not define `supports_block_expectations?` is not a block matcher (since it is relatively rare)' do
            custom_matcher = Module.new do
              def self.matches?(value); true; end
              def self.description; "foo"; end
            end

            expect_deprecation_with_call_site(__FILE__, __LINE__ + 2, /block expectation/)
            expect(3).to custom_matcher # to show the custom matcher can be used as a matcher
            expect { 3 }.to custom_matcher
          end

          it "uses the matcher's `description` in the warning" do
            custom_matcher = Module.new do
              def self.matches?(value); true; end
              def self.description; "matcher-description"; end
            end

            expect_deprecation_with_replacement(/\(matcher-description\)/)
            expect { }.to custom_matcher
          end

          context 'when the matcher does not define `description` (since it is an optional part of the protocol)' do
            it 'uses `inspect` in the warning instead' do
              custom_matcher = Module.new do
                def self.matches?(value); true; end
                def self.inspect; "matcher-inspect"; end
              end

              expect_deprecation_with_replacement(/\(matcher-inspect\)/)
              expect { }.to custom_matcher
            end
          end
        end
      end
    end
  end
end

