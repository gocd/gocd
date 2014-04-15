$: << File.join( File.dirname( __FILE__ ), "..", "lib" )
require 'test/unit'
require 'cipher'

class TC_SolitaireCipher < Test::Unit::TestCase

  class MockAlgorithms
    def get( name )
      MockAlgorithm.new
    end
  end

  class MockDeck
    def cipher_shuffle!
    end

    def cipher_letter
      "X"
    end
  end

  class MockAlgorithm
    def new_deck
      MockDeck.new
    end
  end

  def setup
    @cipher = SolitaireCipher.new( UnkeyedAlgorithm.new )
    @cipher.algorithms = MockAlgorithms.new
    @cipher.stream = KeyStream.new
  end

  def test_use_algorithm
    @cipher.use_algorithm "mock"
    assert_equal "FCJJM", @cipher.encrypt( "HELLO" )
    assert_equal "JGNNQ", @cipher.decrypt( "HELLO" )
  end

  def test_encrypt
    msg = "Code in Ruby! Live longer."
    expected = "GLNCQ MJAFF FVOMB JIYCB"
    assert_equal expected, @cipher.encrypt( msg )
  end

  def test_decrypt_bad
    assert_raise( RuntimeError ) do
      @cipher.decrypt( "not good" )
    end

    assert_raise( RuntimeError ) do
      @cipher.decrypt( "BOGUS 12345" )
    end
  end

  def test_decrypt_good
    msg = "CLEPK HHNIY CFPWH FDFEH"
    expected = "YOURCIPHERISWORKINGX"
    assert_equal expected, @cipher.decrypt( msg )

    msg = "ABVAW LWZSY OORYK DUPVH"
    expected = "WELCOMETORUBYQUIZXXX"
    assert_equal expected, @cipher.decrypt( msg )
  end

end
