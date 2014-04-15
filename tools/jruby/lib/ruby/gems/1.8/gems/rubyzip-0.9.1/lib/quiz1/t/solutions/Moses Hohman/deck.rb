require 'util'

module Solitaire
	class Deck
		NUM_CARDS = 54

		def initialize(order=nil)
			if order.nil?
				@order = Array.new(NUM_CARDS) { |x| x+1 }
			else
				@order = order.collect { |val| val.instance_of?(Range) ? val.to_a : val }.flatten
				@order.collect! { |val| Card.parse(val).code }
			end
		end
		attr_reader :order
		protected :order

		def [](index)
			Card.parse(@order[index])
		end

		def move_card!(card, offset)
			current_index = @order.index(card.code)
			new_index = current_index+offset
			if new_index >= NUM_CARDS
				new_index -= NUM_CARDS - 1
			elsif new_index < 0
				new_index += NUM_CARDS - 1
			end
			@order.delete_at(current_index)
			@order.insert(new_index, card.code)
		end

		def triple_cut!(cards)
			raise "exactly two cards required for triple cut: #{cards}" unless cards.size==2 
			indices = [@order.index(cards[0].code), @order.index(cards[1].code)].sort
			@order = @order[(indices[1]+1)..@order.size] + @order[indices[0]..indices[1]] + @order[0..(indices[0]-1)]
		end

		def count_cut!
			num_moved = Card.parse(@order.last).value
			if num_moved!=53
				@order = @order[num_moved, NUM_CARDS - num_moved - 1] + @order[0..(num_moved-1)] + [@order.last]
			end
			self
		end

		def to_s
			"<Deck: #{@order.inspect}>"
		end
		
		def ==(val)
			@order == val.order
		end
	end

	class Card
		ACE = 1
		JACK = 11
		QUEEN = 12
		KING = 13

		def initialize(suit, value)
			@code = suit.value + value
		end
		attr_reader :code
		alias_method :value, :code

		def Card.parse(code)
			if (1..52).member?(code)
				Card.new(Suit.by_value(code), code.offset_mod(13)) 
			elsif (53..54).member?(code)
				Card.joker(code+12)
			elsif (65..66).member?(code)
				Card.joker(code)
			elsif code =~ /\A[AB]\Z/
				Card.joker(code[0])
			else
				raise "Illegal class or value for parameter value, #{code.class} #{code.inspect}"
			end
		end

		def Card.joker(char)
			JokerCard.new(char.chr)
		end

		def is_joker?
			false
		end

		def ==(other)
			code==other.code
		end

		def <=>(other)
			code<=>other.code
		end

		def to_s
			"Card: #{@code}"
		end
	end

	class JokerCard < Card
		JOKER_VALUE = 53

		def initialize(which_one)
			raise "No such joker: #{which_one}" unless which_one =~ /\A[AB]\Z/
			@code = 52 + (which_one[0].to_i-64)
		end

		def is_joker?
			true
		end

		def value
			JOKER_VALUE
		end
	end

	class Suit
		@@byValue = {}

		def Suit.by_value(val)
			@@byValue[(val-1)/13*13]
		end

		def initialize(name, value)
			@name = name
			@value = value
			@@byValue[value] = self
		end
		attr_reader :name, :value

		CLUBS = Suit.new("clubs", 0)
		DIAMONDS = Suit.new("diamonds", 13)
		HEARTS = Suit.new("hearts", 26)
		SPADES = Suit.new("spades", 39)
	end
end
