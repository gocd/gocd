require 'optparse'

class Deck

  def initialize
    @deck = (1..52).to_a + [ "A", "B" ]
    @length = @deck.length
  end

  def cipher_shuffle!
    # move joker A down one card, circularly
    reposition_card( "A", 1 )

    # move joker B down two cards, circularly
    reposition_card( "B", 2 )

    joker_A = @deck.index( "A" )
    joker_B = @deck.index( "B" )

    # move all cards above the top-most joker, below the bottom-most joker, and
    # all cards below the bottom-most joker, above the top-most joker.
    top    = ( joker_A < joker_B ? joker_A : joker_B )
    bottom = ( joker_A > joker_B ? joker_A : joker_B )
    @deck = @deck[bottom+1..-1] + @deck[top..bottom] + @deck[0,top]

    # take value of the bottom-most card, and cut that many cards off the
    # top, inserting them just before the bottom-most card.
    cut = @deck.last
    @deck = @deck[cut..-2] + @deck[0,cut] + [ @deck.last ]
  end

  def cipher_letter
    count = @deck.first
    count = 53 if count.is_a?( String )
    result = @deck[ count ]
    return nil unless result.is_a? Fixnum
    result -= 26 while result > 26
    return (result+64).chr
  end

  def to_a
    @deck.dup
  end

  def cards=( deck )
    @deck = deck
    @length = @deck.length
    raise "the deck must contain an 'A' joker" unless @deck.include?("A")
    raise "the deck must contain a 'B' joker" unless @deck.include?("B")
  end

  def reposition_card( card, delta )
    pos = @deck.index card
    @deck.delete_at pos
    new_pos = pos + delta
    new_pos = 1 + new_pos % @length if new_pos >= @length
    @deck.insert new_pos, card
    new_pos
  end
  private :reposition_card

end

class KeyingAlgorithms

  attr_writer :algorithms
  attr_writer :registry

  def get( name )
    svc_name = @algorithms[ name ]
    raise "No such algorithm #{name.inspect}" if svc_name.nil?

    return @registry.service( svc_name )
  end

end

class UnkeyedAlgorithm

  def new_deck
    Deck.new
  end

end

class KeyStream

  attr_writer :deck

  def next
    loop do
      @deck.cipher_shuffle!
      letter = @deck.cipher_letter
      return letter if letter
    end
  end

end

class SolitaireCipher

  attr_writer :algorithms
  attr_writer :stream

  def initialize( default_algorithm )
    @algorithm = default_algorithm
  end

  def use_algorithm( keying_algorithm )
    @algorithm = @algorithms.get( keying_algorithm )
  end

  def encrypt( message )
    reset

    chars = message.split(//).map { |c| c.upcase }. reject { |c| c !~ /[A-Z]/ }
    chars.concat ["X"] * ( 5 - chars.length % 5 ) if chars.length % 5 > 0
    chars.map! { |c| c[0] - 64 }
    key = generate_key( chars.length )
    code = chars.zip( key ).map { |c,k| ( c + k > 26 ? c + k - 26 : c + k ) }.map { |c| (c+64).chr }

    msg = ""
    (code.length/5).times do
      msg << " " if msg.length > 0
      5.times { msg << code.shift }
    end

    return msg
  end

  def decrypt( message )
    raise "bad decrypt message: #{message.inspect}" if message =~ /[^A-Z ]/

    reset
    chars = message.split(//).reject { |c| c == " " }.map { |c| c[0] - 64 }
    key = generate_key( chars.length )
    chars.zip( key ).map { |c,k| ( k >= c ? c + 26 - k : c - k ) }.map { |c| (c+64).chr }.join
  end

  def generate_key( length )
    key = []
    length.times { key << @stream.next }
    key.map { |c| c[0] - 64 }
  end
  private :generate_key

  def reset
    @stream.deck = @algorithm.new_deck
  end
  private :reset

end

class Options

  attr_reader :strings
  attr_reader :keying_algorithm

  def initialize( argv = ARGV )
    @named_options = Hash.new
    @run_app = true
    @keying_algorithm = "unkeyed"

    OptionParser.new do |opts|
      opts.banner = "Usage: #{$0} [options] [strings]"
      opts.separator ""

      opts.on( "-o", "--option NAME=VALUE",
               "Specify a named value, for use by a component of the cipher."
      ) do |pair|
        name, value = pair.split( / *= */, 2 )
        @named_options[ name ] = value
      end

      opts.on( "-k", "--key NAME", "Specify the keying algorithm to use" ) do |value|
        @keying_algorithm = value
      end

      opts.separator ""

      opts.on_tail( "-h", "--help", "This help text" ) do
        puts opts
        @run_app = false
      end

      opts.parse!( argv )
    end

    @strings = argv
  end

  def []( value )
    @named_options[ value ]
  end

  def run_app?
    @run_app
  end

end


class BackwardsAlgorithm

  def new_deck
    deck = Deck.new
    deck.cards = deck.to_a.reverse
    deck
  end

end

class ShuffleAlgorithm

  attr_writer :options

  def new_deck
    deck = Deck.new
    cards = deck.to_a

    seed = ( @options[ "seed" ] || 0 ).to_i
    srand seed

    7.times { cards.sort! { rand(3)-1 } }
    deck.cards = cards

    return deck
  end

end
