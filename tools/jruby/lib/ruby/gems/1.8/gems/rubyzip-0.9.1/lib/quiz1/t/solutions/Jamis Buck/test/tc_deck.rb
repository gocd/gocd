$: << File.join( File.dirname( __FILE__ ), "..", "lib" )
require 'test/unit'
require 'cipher'

class TC_Deck < Test::Unit::TestCase

  def setup
    @deck = Deck.new
  end

  def test_content
    expected = (1..52).to_a + [ "A", "B" ]
    assert_equal expected, @deck.to_a
  end

  def test_shuffle
    @deck.cipher_shuffle!
    expected = (2..52).to_a + [ "A", "B", 1 ]
    assert_equal expected, @deck.to_a
  end

  def test_letter
    expected = %w{ D W J nil X H Y R F D G }
    expected.each do |expected_letter|
      @deck.cipher_shuffle!
      assert_equal expected_letter, @deck.cipher_letter || "nil"
    end
  end

end
