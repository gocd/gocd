rspec_lib = File.dirname(__FILE__) + "/../../../../../../lib"
$:.unshift rspec_lib unless $:.include?(rspec_lib)
require 'spec/autorun'
require 'spec/test/unit'

class TestCaseWithErrors < Test::Unit::TestCase
  def test_with_error
    raise "error raised in TestCaseWithErrors"
  end
end