rspec_lib = File.dirname(__FILE__) + "/../../../../../../lib"
$:.unshift rspec_lib unless $:.include?(rspec_lib)
require 'spec/autorun'
require 'spec/test/unit'

class TestCaseThatPasses < Test::Unit::TestCase
  def test_that_passes
    true.should be_true
  end
end