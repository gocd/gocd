require "spec_helper"
require "bundler"
require "bundler/friendly_errors"

describe Bundler, "friendly errors" do
  it "rescues Thor::AmbiguousTaskError and raises SystemExit" do
    expect {
      Bundler.with_friendly_errors do
        raise Thor::AmbiguousTaskError.new("")
      end
    }.to raise_error(SystemExit)
  end
end
