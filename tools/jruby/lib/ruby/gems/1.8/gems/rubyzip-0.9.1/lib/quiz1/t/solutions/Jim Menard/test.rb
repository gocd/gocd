#! /usr/bin/env ruby

require 'test/unit.rb'
require 'test/unit/ui/console/testrunner'
require 'solitaire_cypher.rb'

class SolitaireCypherTest < Test::Unit::TestCase

    KNOWN_PLAINTEXT = 'CODEI NRUBY LIVEL ONGER'
    KNOWN_CYPHER = 'GLNCQ MJAFF FVOMB JIYCB'

    def setup
	@deck = Deck.new.key
	@crypt_keeper = CryptKeeper.new(@deck)
    end

    def test_value_to_chr
	assert_equal('A', Card.value_to_chr(1))
	assert_equal('Z', Card.value_to_chr(26))
    end

    def test_chr_to_value
	assert_equal(1, Card.chr_to_value("A"))
	assert_equal(26, Card.chr_to_value("Z"))
    end

    def test_keystream
	expected = %w(D W J X H Y R F D G)
	deck = Deck.new.key
	expected.each { | exp |
	    key = deck.next_keystream
	    if exp != Card.value_to_chr(key)
		@errors << "expected #{exp}, key = #{Card.value_to_chr(key)}"
	    end
	}
    end

    def test_decrypt_known_cypher
	assert_equal(KNOWN_PLAINTEXT, @crypt_keeper.decrypt(KNOWN_CYPHER))
    end

    def test_encrypt_known_message
	assert_equal(KNOWN_CYPHER, @crypt_keeper.encrypt(KNOWN_PLAINTEXT))
    end
end

Test::Unit::UI::Console::TestRunner.run(SolitaireCypherTest)
