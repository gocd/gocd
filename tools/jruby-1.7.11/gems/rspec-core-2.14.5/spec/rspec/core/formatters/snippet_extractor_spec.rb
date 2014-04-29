require 'spec_helper'
require 'rspec/core/formatters/snippet_extractor'

module RSpec
  module Core
    module Formatters
      describe SnippetExtractor do
        it "falls back on a default message when it doesn't understand a line" do
          expect(RSpec::Core::Formatters::SnippetExtractor.new.snippet_for("blech")).to eq(["# Couldn't get snippet for blech", 1])
        end

        it "falls back on a default message when it doesn't find the file" do
         expect(RSpec::Core::Formatters::SnippetExtractor.new.lines_around("blech", 8)).to eq("# Couldn't get snippet for blech")
        end

        it "falls back on a default message when it gets a security error" do
          message = nil
          safely do
            message = RSpec::Core::Formatters::SnippetExtractor.new.lines_around("blech", 8)
          end
          expect(message).to eq("# Couldn't get snippet for blech")
        end
      end
    end
  end
end
