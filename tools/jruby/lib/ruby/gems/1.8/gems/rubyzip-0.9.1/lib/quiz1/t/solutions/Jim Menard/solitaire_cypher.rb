#! /usr/bin/env ruby

RANKS = %w(A 2 3 4 5 6 7 8 9 10 J Q K)
SUITS = %w(C D H S)
JOKER_RANK = 'joker'
JOKER_VALUE = -1

class Card
    def Card.value_to_chr(value)
	i = value
	i -= 26 while i > 26
	(i + ?A - 1).chr
    end
    def Card.chr_to_value(chr)
	i = chr[0] - ?A + 1
	i += 26 while i < 0
	i
    end

    def initialize(rank, suit)
	@rank = rank
	@suit = suit
	if rank == JOKER_RANK
	    @value = JOKER_VALUE
	else
	    @value = (SUITS.index(suit) * 13) + RANKS.index(rank) + 1
	end
    end

    def to_s
# 	return @value.to_s if @value != JOKER_VALUE
# 	return @suit.to_s
 	"#{@rank}#{@suit} #{@value.to_s}"
    end

    def to_i
	@value
    end

    def chr
	Card.value_to_chr(@value)
    end
end

class Deck

    def initialize
	@cards = []
	SUITS.each { | suit |
	    RANKS.each { | rank | @cards << Card.new(rank, suit) }
	}
	@joker_a = Card.new(JOKER_RANK, 'A')
	@cards << @joker_a
	@joker_b = Card.new(JOKER_RANK, 'B')
	@cards << @joker_b
    end

    # Keys the deck and returns itself.
    def key
	# do nothing; keyed when initialized
	self
    end

    # Return the next kestream value as a number (not a string).
    # Keep going until we have a non-joker value.
    def next_keystream
	val = JOKER_VALUE
	until val != JOKER_VALUE
	    val = generate_next_keystream_value
	end
	val
    end

    # Return the next keystream value as a number 1-26 (not a string).
    def generate_next_keystream_value
	move(@joker_a, 1)
	move(@joker_b, 2)
	triple_cut()
	count_cut()
	return output_number()
    end

    # Move a card a certain distance. Wrap around the end of the deck.
    def move(card, distance)
	old_pos = @cards.index(card)
	new_pos = old_pos + distance
	new_pos -= (@cards.length-1) if new_pos >= @cards.length
	@cards[old_pos,1] = []
	@cards[new_pos,0] = [card]
    end

    # Perform a triple cut around the two jokers. All cards above the top
    # joker move to below the bottom joker and vice versa. The jokers and the
    # cards between them do not move.
    def triple_cut
	i = @cards.index(@joker_a)
	j = @cards.index(@joker_b)
	j, i = i, j if j < i	# make sure i < j
	@cards = slice(j+1, -1) + slice(i, j) + slice(0, i-1)
    end

    # Perform a count cut using the value of the bottom card. Cut the bottom
    # card's value in cards off the top of the deck and reinsert them just
    # above the bottom card.
    def count_cut
	i = @cards[@cards.length - 1].to_i
	@cards = slice(i, -2) + slice(0, i-1) + [@cards[@cards.length-1]]
    end

    # Returns a non-nil cut of cards from the deck.
    def slice(from, to)
	slice = @cards[from..to]
	return slice || []
    end

    # Return the output number (not letter). Convert the top card to it's
    # value and count down that many cards from the top of the deck, with the
    # top card itself being card number one. Look at the card immediately
    # after your count and convert it to a letter. This is the next letter in
    # the keystream. If the output card is a joker, no letter is generated
    # this sequence. This step does not alter the deck.
    def output_number
	i = @cards[0].to_i
	i -= @cards.length if i >= @cards.length
	num = @cards[i].to_i
	num -= 26 if num > 26
	num
    end

    def to_s
	@cards.join(' ')
    end
end

class CryptKeeper

    def initialize(deck)
	@keyed_deck = deck
    end

    def decrypt(str)
	@deck = @keyed_deck.dup
	answer = ""
	str.split(//).each { | c |
	    if c == ' '
		answer << ' '
		next
	    end

	    msg_num = Card.chr_to_value(c)
	    key = @deck.next_keystream
	    diff = msg_num - key
	    diff += 26 if diff < 1
	    answer << Card.value_to_chr(diff)
	}
	answer
    end

    def encrypt(str)
	@deck = @keyed_deck.dup
	answer = ''
	str.split(//).each { | c |
	    if c == ' '
		answer << ' '
		next
	    end

	    msg_num = Card.chr_to_value(c)
	    key = @deck.next_keystream
	    sum = msg_num + key
	    sum -= 26 if sum > 26
	    answer << Card.value_to_chr(sum)
	}
	answer
    end

    def crypto_each(str)
	@deck = @keyed_deck.dup
	str.split(//).each { | c | yield c }
    end

end

def prep_arg(str)
    str = str.upcase.gsub(/[^A-Z]/, '')
    words = []
    while str.length > 0
	words << str[0...5]
	str[0...5] = ''
    end

    last_len = words[words.length-1].length
    words[words.length-1] += ('X' * (5 - last_len)) if last_len < 5
    words.join(' ')
end

if __FILE__ == $0
    if ARGV[0]
	puts CryptKeeper.new(Deck.new.key).decrypt(prep_arg(ARGV[0]))
    else
	puts CryptKeeper.new(Deck.new.key).decrypt('CLEPK HHNIY CFPWH FDFEH')
	puts CryptKeeper.new(Deck.new.key).decrypt('ABVAW LWZSY OORYK DUPVH')
    end
end
