$: << File.join( File.dirname( __FILE__ ), "..", "lib" )
require 'test/unit'
require 'cipher'

class TC_UnkeyedAlgorithm < Test::Unit::TestCase

  def setup
    @algo = UnkeyedAlgorithm.new
  end

  def test_new_deck
    expected = (1..52).to_a + [ "A", "B" ]
    deck = @algo.new_deck
    assert_equal expected, deck.to_a
  end

end
