#!/usr/bin/env ruby
require 'test/unit'
require 'cipher'

module Solitaire
	class TestChunker < Test::Unit::TestCase
		def test_chunks
			chunker = Chunker.new("Code in Ruby, live longer!")
			assert_equal(["CODEI","NRUBY","LIVEL","ONGER"], chunker.chunks)
		end

		def test_pads_with_Xs
			chunker = Chunker.new("sty")
			assert_equal(["STYXX"], chunker.chunks)
		end

		def test_number_chunks
			chunker = Chunker.new("Code in Ruby, live longer!")
			assert_equal([[3,15,4,5,9], [14,18,21,2,25], [12,9,22,5,12], [15,14,7,5,18]], chunker.number_chunks)
		end

		def test_to_letters
			assert_equal(["CODEI","NRUBY","LIVEL","ONGER"],
				Chunker.to_letters([[3,15,4,5,9], [14,18,21,2,25], [12,9,22,5,12], [15,14,7,5,18]]))
		end
	end

	class TestKeystream < Test::Unit::TestCase
		def setup
			@keystream = Keystream.new
		end

		def test_keystream_letters
			chunker = Chunker.new("Code in Ruby, live longer!")
			assert_equal(["DWJXH","YRFDG","TMSHP","UURXJ"], @keystream.keystream_letters(chunker.chunks))
		end

		def test_card_to_letter
			assert_equal("", Keystream.card_to_letter(Card.joker(?A)), "A joker")
			assert_equal("", Keystream.card_to_letter(Card.joker(?B)), "B joker")
			assert_equal("A", Keystream.card_to_letter(Card.new(Suit::CLUBS, Card::ACE)), "AC")
			assert_equal("Z", Keystream.card_to_letter(Card.new(Suit::DIAMONDS, Card::KING)), "KD")
			assert_equal("A", Keystream.card_to_letter(Card.new(Suit::HEARTS, Card::ACE)), "AH")
			assert_equal("Z", Keystream.card_to_letter(Card.new(Suit::SPADES, Card::KING)), "KS")
		end
	end

	class TestCipher < Test::Unit::TestCase
		def test_encrypt
			cipher = Cipher.new("Code in Ruby, live longer!")
			assert_equal("encrypt", cipher.mode)
			assert_equal("GLNCQ MJAFF FVOMB JIYCB", cipher.crypt)
		end

		def test_decrypt
			cipher = Cipher.new("GLNCQ MJAFF FVOMB JIYCB")
			assert_equal("decrypt", cipher.mode)
			assert_equal("CODEI NRUBY LIVEL ONGER", cipher.crypt)
		end

		def test_crypt_idempotent
			cipher = Cipher.new("GLNCQ MJAFF FVOMB JIYCB")
			assert_equal("decrypt", cipher.mode)
			assert_equal("CODEI NRUBY LIVEL ONGER", cipher.crypt)
			assert_equal("CODEI NRUBY LIVEL ONGER", cipher.crypt)
		end
	end
end
