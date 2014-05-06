rspec_lib = File.dirname(__FILE__) + "/../../../../../../lib"
$:.unshift rspec_lib unless $:.include?(rspec_lib)
require 'spec/autorun'
require 'spec/test/unit'

describe "example group with errors" do
  it "should raise errors" do
    raise "error raised in example group with errors"
  end
end