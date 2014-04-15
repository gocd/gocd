#!/usr/bin/env ruby
require 'test/unit'
require 'deck'

module Solitaire
	class TestDeck < Test::Unit::TestCase
		def test_constructor_accepts_ranges_in_array
			assert_equal(Deck.new, Deck.new([1, 2, 3, 4..52, 53..54]))
		end

		def test_construcor_accepts_chars_for_jokers
			assert_equal(Deck.new, Deck.new((1..52).to_a + [?A, ?B]))
		end

		def test_move_card_lower
			deck = Deck.new
			deck.move_card!(Card.new(Suit::DIAMONDS, 6), +5)
			assert_equal(Deck.new([1..18,20..24,19,25..54]).to_s, deck.to_s)
		end

		def test_move_card_one_lower
			deck = Deck.new
			deck.move_card!(Card.new(Suit::DIAMONDS, 6), +1)
			assert_equal(Deck.new([1..18,20,19,21..54]).to_s, deck.to_s)
		end

		def test_move_card_higher
			deck = Deck.new
			deck.move_card!(Card.new(Suit::DIAMONDS, 6), -5)
			assert_equal(Deck.new([1..13,19,14..18,20..54]), deck)
		end

		def test_move_card_is_cyclic_plus_1
			deck = Deck.new
			deck.move_card!(Card.joker(?A), +2)
			assert_equal(Deck.new([1,53,2..52,54]), deck)
		end

		def test_move_card_to_end
			deck = Deck.new
			deck.move_card!(Card.joker(?A), +1)
			assert_equal(Deck.new([1..52,54,53]), deck)
		end

		def test_move_card_to_one_before_end
			deck = Deck.new([2..54,1])
			deck.move_card!(Card.joker(?A), +1)
			assert_equal(Deck.new([2..52,54,53,1]).to_s, deck.to_s)
		end

		def test_move_card_to_beginning
			deck = Deck.new
			deck.move_card!(Card.new(Suit::CLUBS, 2), -1)
			assert_equal(Deck.new([2,1,3..54]), deck)
		end

		def test_move_card_is_cyclic_pass_end_forward
			deck = Deck.new
			deck.move_card!(Card.joker(?B), +1)
			assert_equal(Deck.new([1,54,2..53]), deck)
		end

		def test_move_card_is_cyclic_pass_end_backward
			deck = Deck.new
			deck.move_card!(Card.new(Suit::CLUBS, Card::ACE), -1)
			assert_equal(Deck.new([2..53,1,54]), deck)
		end

		def test_move_card_cyclic_backward_five
			deck = Deck.new
			deck.move_card!(Card.new(Suit::CLUBS, 5), -5)
			assert_equal(Deck.new([1..4,6..53,5,54]), deck)
		end

		def test_triple_cut
			deck = Deck.new
			deck.triple_cut!([Card.new(Suit::CLUBS, Card::JACK), Card.new(Suit::HEARTS, Card::KING)])
			assert_equal(Deck.new([40..54,11..39,1..10]), deck)
		end

		def test_triple_cut_with_empty_side
			deck = Deck.new([2..53,1,54])
			deck.triple_cut!([Card.joker(?A), Card.joker(?B)])
			assert_equal(Deck.new([53,1,54,2..52]), deck)
		end

		def test_count_cut
			deck = Deck.new
			deck.move_card!(Card.new(Suit::CLUBS, 5), 49)
			deck.count_cut!
			assert_equal(Deck.new([7..54,1..4,6,5]), deck)
		end
	end

	class TestCard < Test::Unit::TestCase
		def test_parse_joker_string
			assert_equal(Card.joker(?A), Card.parse("A"))
		end

		def test_parse_joker_char
			assert_equal(Card.joker(?A), Card.parse(?A))
		end

		def test_parse_normal_card
			assert_equal(Card.new(Suit::CLUBS, 5), Card.parse(5))
		end

		def test_value
			assert_equal(1, Card.new(Suit::CLUBS, Card::ACE).value, "AC")
			assert_equal(7, Card.new(Suit::CLUBS, 7).value, "7C")
			assert_equal(11, Card.new(Suit::CLUBS, Card::JACK).value, "JC")
			assert_equal(13, Card.new(Suit::CLUBS, Card::KING).value, "KC")
			assert_equal(14, Card.new(Suit::DIAMONDS, Card::ACE).value, "AD")
			assert_equal(26, Card.new(Suit::DIAMONDS, Card::KING).value, "KD")
			assert_equal(29, Card.new(Suit::HEARTS, 3).value, "3H")
			assert_equal(39, Card.new(Suit::HEARTS, Card::KING).value, "KH")
			assert_equal(40, Card.new(Suit::SPADES, Card::ACE).value, "AS")
			assert_equal(52, Card.new(Suit::SPADES, Card::KING).value, "KS")
			assert_equal(53, Card.joker(?A).value, "A Joker")
			assert_equal(53, Card.joker(?B).value, "B Joker")
		end

		def test_is_joker_when_joker
			assert(Card.joker(?A).is_joker?, "A joker not joker")
			assert(Card.joker(?B).is_joker?, "B joker not joker")
		end

		def test_is_joker_when_not_joker
			assert(!Card.new(Suit::SPADES, 7).is_joker?)
		end
	end

	class TestSuit < Test::Unit::TestCase
		def test_by_value_clubs
			assert_equal("clubs", Suit.by_value(1).name)
		end

		def test_by_value_hearts
			assert_equal("hearts", Suit.by_value(27).name)
		end

		def test_hearts
			assert_equal("hearts", Suit::HEARTS.name)
		end
	end
end
