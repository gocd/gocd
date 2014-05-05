require 'spec_helper'
require 'bundler/settings'

describe Bundler::Settings do
  describe "#set_local" do
    context "when the local config file is not found" do
      it "raises a GemfileNotFound error with explanation" do
        expect{ subject.set_local("foo", "bar") }.
          to raise_error(Bundler::GemfileNotFound, "Could not locate Gemfile")
      end
    end
  end
end
