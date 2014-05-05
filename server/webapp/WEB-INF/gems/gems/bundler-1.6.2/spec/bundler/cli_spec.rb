require 'spec_helper'
require 'bundler/cli'

describe "bundle executable" do
  let(:source_uri) { "http://localgemserver.test" }

  it "returns non-zero exit status when passed unrecognized options" do
    bundle '--invalid_argument', :exitstatus => true
    expect(exitstatus).to_not be_zero
  end

  it "returns non-zero exit status when passed unrecognized task" do
    bundle 'unrecognized-tast', :exitstatus => true
    expect(exitstatus).to_not be_zero
  end
end
