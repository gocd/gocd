$: << File.join( File.dirname( __FILE__ ), "..", "lib" )
require 'test/unit'
require 'cipher'

class TC_KeyStream < Test::Unit::TestCase

  def setup
    @stream = KeyStream.new
    @stream.deck = Deck.new
  end

  def test_next
    expected = %w{ D W J X H Y R F D G }
    expected.each do |expected_letter|
      assert_equal expected_letter, @stream.next
    end
  end

end
