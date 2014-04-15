$: << File.join( File.dirname( __FILE__ ), "..", "lib" )
require 'test/unit'
require 'cipher'

class TC_KeyingAlgorithms < Test::Unit::TestCase

  class MockRegistry
    def service( name )
      return "found" if name == "something.mock"
      return nil
    end
  end

  def setup
    @algorithms = KeyingAlgorithms.new
    @algorithms.algorithms = { "mock" => "something.mock" }
    @algorithms.registry = MockRegistry.new
  end

  def test_get_not_found
    assert_raise( RuntimeError ) do
      @algorithms.get( "bogus" )
    end
  end

  def test_get_found
    svc = @algorithms.get( "mock" )
    assert_equal svc, "found"
  end

end
