require 'spec_helper'

module RSpec
  module Mocks
    module ArgumentMatchers
      describe HashExcludingMatcher do

        it "describes itself properly" do
          expect(HashExcludingMatcher.new(:a => 5).description).to eq "hash_not_including(:a=>5)"
        end

        describe "passing" do
          it "matches a hash without the specified key" do
            expect(hash_not_including(:c)).to eq({:a => 1, :b => 2})
          end

          it "matches a hash with the specified key, but different value" do
            expect(hash_not_including(:b => 3)).to eq({:a => 1, :b => 2})
          end

          it "matches a hash without the specified key, given as anything()" do
            expect(hash_not_including(:c => anything)).to eq({:a => 1, :b => 2})
          end

          it "matches an empty hash" do
            expect(hash_not_including(:a)).to eq({})
          end

          it "matches a hash without any of the specified keys" do
            expect(hash_not_including(:a, :b, :c)).to eq(:d => 7)
          end

        end

        describe "failing" do
          it "does not match a non-hash" do
            expect(hash_not_including(:a => 1)).not_to eq 1
          end

          it "does not match a hash with a specified key" do
            expect(hash_not_including(:b)).not_to eq(:b => 2)
          end

          it "does not match a hash with the specified key/value pair" do
            expect(hash_not_including(:b => 2)).not_to eq(:a => 1, :b => 2)
          end

          it "does not match a hash with the specified key" do
            expect(hash_not_including(:a, :b => 3)).not_to eq(:a => 1, :b => 2)
          end

          it "does not match a hash with one of the specified keys" do
            expect(hash_not_including(:a, :b)).not_to eq(:b => 2)
          end

          it "does not match a hash with some of the specified keys" do
            expect(hash_not_including(:a, :b, :c)).not_to eq(:a => 1, :b => 2)
          end

          it "does not match a hash with one key/value pair included" do
            expect(hash_not_including(:a, :b, :c, :d => 7)).not_to eq(:d => 7)
          end
        end
      end
    end
  end
end
