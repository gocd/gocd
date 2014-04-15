#!/usr/bin/env ruby

require 'optparse'
require 'ostruct'

# Handles the deck
class Deck

  # Initializes the deck with the default values
  def initialize
    @deck = (1..52).to_a << 'A' << 'B'
  end

  # Operation "move a" (step 2)
  def move_A
    move_down( @deck.index( 'A' ) )
  end

  # Operation "move b" (step 3)
  def move_B
    2.times { move_down( @deck.index( 'B' ) ) }
  end

  # Operation "triple cut" (step 4)
  def triple_cut
    a = @deck.index( 'A' )
    b = @deck.index( 'B' )
    a, b = b, a if a > b
    @deck.replace( [@deck[(b + 1)..-1], @deck[a..b], @deck[0...a]].flatten )
  end

  # Operation "count cut" (step 5)
  def count_cut
    temp = @deck[0..(@deck[-1] - 1)]
    @deck[0..(@deck[-1] - 1)] = []
    @deck[-1..-1] = [temp, @deck[-1]].flatten
  end

  # Operation "output the found letter" (step 6)
  def output_letter
    a = @deck.first
    a = 53 if a.instance_of? String
    output = @deck[a]
    if output.instance_of? String
      nil
    else
      output -= 26 if output > 26
      (output + 64).chr
    end
  end

  # Shuffle the deck using the initialization number +init+ and the method +method+.
  # Currently there are only two methods: <tt>:fisher_yates</tt> and <tt>naive</tt>.
  def shuffle( init, method = :fisher_yates )
    srand( init )
    self.send( method.id2name + "_shuffle", @deck )
  end

  private

  # From pleac.sf.net
  def fisher_yates_shuffle( a )
    (a.size-1).downto(0) { |i|
      j = rand(i+1)
      a[i], a[j] = a[j], a[i] if i != j
    }
  end

  # From pleac.sf.net
  def naive_shuffle( a )
    for i in 0...a.size
      j = rand(a.size)
      a[i], a[j] = a[j], a[i]
    end
  end

  # Moves the index one place down while treating the used array as circular list.
  def move_down( index )
    if index == @deck.length - 1
      @deck[1..1] = @deck[index], @deck[1]
      @deck.pop
    else
      @deck[index], @deck[index + 1] = @deck[index + 1], @deck[index]
    end
  end

end


# Implements the Solitaire Cipher algorithm
class SolitaireCipher

  # Initialize the deck
  def initialize( init = -1, method = :fisher_yates )
    @deck = Deck.new
    @deck.shuffle( init, method ) unless init == -1
  end

  # Decodes the given +msg+ using the internal deck
  def decode( msg )
    msgNumbers = to_numbers( msg )
    cipherNumbers = to_numbers( generate_keystream( msg.length ) )

    resultNumbers = []
    msgNumbers.each_with_index do |item, index|
      item += 26 if item <= cipherNumbers[index]
      temp = item - cipherNumbers[index]
      resultNumbers << temp
    end

    return to_chars( resultNumbers )
  end

  # Encodes the given +msg+ using the internal deck
  def encode( msg )
    msg = msg.gsub(/[^A-Za-z]/, '').upcase
    msg += "X"*(5 - (msg.length % 5)) unless msg.length % 5 == 0

    msgNumbers = to_numbers( msg )
    cipherNumbers = to_numbers( generate_keystream( msg.length ) )

    resultNumbers = []
    msgNumbers.each_with_index do |item, index|
      temp = item + cipherNumbers[index]
      temp = temp - 26 if temp > 26
      resultNumbers << temp
    end

    return to_chars( resultNumbers )
  end

  private

  # Converts the string of uppercase letters into numbers (A=1, B=2, ...)
  def to_numbers( chars )
    chars.unpack("C*").collect {|x| x - 64}
  end

  # Converts the array of numbers to a string (1=A, 2=B, ...)
  def to_chars( numbers )
    numbers.collect {|x| x + 64}.pack("C*")
  end

  # Generates a keystream for the given +length+.
  def generate_keystream( length )
    deck = @deck.dup
    result = []
    while result.length != length
      deck.move_A
      deck.move_B
      deck.triple_cut
      deck.count_cut
      letter = deck.output_letter
      result << letter unless letter.nil?
    end
    result.join
  end

end


options = OpenStruct.new
options.key = -1
options.shuffle = :fisher_yates
options.call_method = :decode

opts = OptionParser.new do |opts|
  opts.banner = "Usage: #{File.basename($0, '.*')} [options] message"
  opts.separator ""
  opts.separator "Options:"
  opts.on( "-d", "--decode", "Decode the message" ) do
    options.call_method = :decode
  end
  opts.on( "-e", "--encode", "Encode the message" ) do
    options.call_method = :encode
  end
  opts.on( "-k", "--key KEY", Integer, "Key for shuffling the deck" ) do |key|
    options.key = key
  end
  opts.on( "-m", "--method METHOD", [:fisher_yates, :naive],
           "Select the shuffling method (fisher_yates, naive" ) do |method|
    options.shuffle = method
  end
  opts.on( "-h", "--help", "Show help" ) do
    puts opts
    exit
  end
end


if ARGV.length == 0
  puts opts
  exit
end

message = opts.permute!( ARGV )[0]
cipher = SolitaireCipher.new( options.key, options.shuffle )
puts cipher.send( options.call_method, message )
