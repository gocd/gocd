# bail if you have nothing to do.
unless ARGV[0]
  print <<-STOP_PRINT
    Encrypts/decrypts a string of text.
    Usage:  Give it some text!
  STOP_PRINT
  exit
end

# the obligatory deck of cards.
###############################################################################
class Deck
  attr_reader :cards, :order

  def initialize
    @cards = []
    @order = []
    build_deck
  end

  def to_s
    return @cards.to_s
  end

  def build_deck
    [:Clubs, :Diamonds, :Hearts, :Spades].each do |suit|
      rank = 0
      'A23456789TJQK'.each_byte do |name|
        # 'real' cards have a rank value
        rank += 1
        add_card(name.chr, suit, rank)
      end
    end

    # Jokers have no rank value
    'AB'.each_byte {|name| add_card(name.chr, :Joker, 0)}
  end

  # build order while adding.
  def add_card(name, suit, rank)
    card = Card.new(name, suit, rank)
    @cards << card
    @order << card.to_s
  end

  # Uses order to hunt for cards (Joker searches).
  def find_card(name, suit)
    return @order.index(Card.to_s(name, suit))
  end

  # does as many cuts as you give it.
  def cut_cards(cuts)
    cards = []
    loc = 0
    [cuts].flatten.each_with_index do |cut, idx|
      cards[idx] = @cards[loc...cut]
      loc = cut
    end
    cards << @cards[loc...@cards.length]
  end

  def cards=(cards)
    # flatten to handle cut results.
    @cards = cards.flatten
    # rebuild @order each time the deck changes.
    update_order
  end

  # simple, but not very efficient.
  def update_order
    @order = @cards.collect {|card| card.to_s}
  end

end

# the above deck is made up of...
###############################################################################
class Card
  @@SUITS = {
    :Clubs    =>  0,
    :Diamonds => 13,
    :Hearts   => 26,
    :Spades   => 39,
    :Joker    => 53
  }

  def self.to_s(name, suit)
    return name + ' ' + suit.to_s + "\n"
  end

  attr_reader :name, :suit, :rank

  def initialize(name, suit, rank)
    @name = name
    @suit = suit
    @rank = rank + @@SUITS[suit]
  end

  def to_s
    Card.to_s(@name, @suit)
  end
end

###############################################################################
class Solitaire

  attr_reader :deck

  def initialize(text)
    @deck = Deck.new
    @text = text.to_s
  end

  def process
    # does it look encrypted?  5 letter blocks all uppercase?
    looks_encrypted = @text.gsub(/[A-Z]{5}\s?/, '').empty?
    results = ''

    # prep the text for parsing.
    if looks_encrypted
      # strip off the blanks for consistency
      text = @text.gsub(/\s/, '')
    else
      # Discard any non A to Z characters, and uppercase all remaining
      text = @text.upcase.gsub!(/[^A-Z]/, '')
      # Split the message into five character groups,
      words, padding = word_count(text, 5)
      # using Xs to pad the last group
      text += padding
    end

    # parse it, and build up results.
    text.each_byte do |char|
      if looks_encrypted
        char -= next_key
        char += 26 if char < 65
      else
        char += next_key
        char -= 26 if char > 90
      end
      results += char.chr
    end

    return space_text(results, 5)
  end

  # counts words as 5 char blocks
  def word_count(text, len)
    words, strays = text.length.divmod(len)
    words += 1 if strays > 0
    pad = "X" * (len - strays)
    return [words, pad]
  end

  def space_text(text, len)
    # adds a space every 5 letters.
    # not sure how efficient this is.
    return text.unpack(('A' + len.to_s) * word_count(text, len)[0]).join(' ')
  end

  def shift_card(name, suit, count)
    # find the card
    idx = @deck.find_card(name, suit)
    # remove it from the deck.
    card = @deck.cards.slice!(idx)
    # calculate new placement.
    idx += count
    # the slice above makes length 'look' zero-based
    idx -= @deck.cards.length if idx > @deck.cards.length

    # glue the deck together as cards before, card, cards after.
    @deck.cards = @deck.cards[0...idx] + [card] +
@deck.cards[idx...@deck.cards.length]
  end

  def next_key
    shift_card('A', :Joker, 1)
    shift_card('B', :Joker, 2)

    # find the 2 jokers, and sort them for the cut.
    jokers = [@deck.find_card('A', :Joker), @deck.find_card('B', :Joker)].sort
    # increment the 2nd joker pos -- cut uses 'up to, but not including'
    jokers[1] += 1
    # reverse works nicely for the triple cut.
    @deck.cards = @deck.cut_cards(jokers).reverse

    # get the value from the last card, and cut up to it.
    cuts = @deck.cut_cards([@deck.cards.last.rank, @deck.cards.length - 1])
    @deck.cards = cuts[1] + cuts[0] + cuts[2]

    # read top card value, count down that many cards + 1
    key = @deck.cards[@deck.cards[0].rank].rank
    # convert it to a letter, adjust if needed.
    key -= 26 if key > 26

    # if key is still > 26, then it's a joker!
    return (key) unless key > 26
    # try again if it's a joker!
    next_key
  end
end

test = Solitaire.new(ARGV[0])
puts test.process
puts test.deck
