class Deck
  def initialize
    @deck = Array.new(54) {|i| i}
  end

  def create_keystream(count)
    stream = []
    count.times do
      letter = next_letter
      redo unless letter
      stream << letter
    end
    return stream
  end

  def next_letter
    ##
    # move the jokers
    ##

    2.times do |j|
      # find the joker
      index = @deck.index(52 + j)

      # remove it from the deck
      @deck.delete_at(index)

      # calculate new index
      index = ((index + j) % 53) + 1

      # insert the joker at that index
      @deck[index, 0] = 52 + j
    end

    ##
    # do the tripple cut
    ##

    # first find both jokers
    a = @deck.index(52)
    b = @deck.index(53)

    # sort the two indeces
    low, hi = [a, b].sort

    # get the lower and upper parts of the deck
    upper = @deck.slice!((hi + 1)..-1)
    lower = @deck.slice!(0, low)

    # swap them
    @deck = upper + @deck + lower

    ##
    # do the count cut
    ##

    # find out the number of cards to cut
    count = value_at(53)

    # remove them from the top of the deck
    cards = @deck.slice!(0, count)

    # reinsert them just above the lowest card
    @deck[-1, 0] = cards

    return letter_at(value_at(0))
  end

  def value_at(index)
    id = @deck[index]
    (id > 51) ? 53 : id + 1
  end

  def letter_at(index)
    id = @deck[index]
    (id > 51) ? nil : (id % 26) + 1
  end

  def to_s
    @deck.map {|v| (v > 51) ? (v + 13).chr : (v + 1).to_s}.join(' ')
  end
end

def encode(data, keystream)
  result = []
  data.size.times {|i| result << ((data[i] + keystream[i]) % 26)}
  return result
end

def decode(data, keystream)
  encode(data, keystream.map {|v| 26 - v})
end

def data_to_string(data)
  data = data.map {|v| (v + 65).chr}.join
  (0...(data.size / 5)).map {|i| data[i * 5, 5]}.join(' ')
end

if ARGV.size != 1
  puts "Usage: solitaire.rb MESSAGE"
  exit
end

data = ARGV[0].upcase.split(//).select {|c| c =~ /[A-Z]/}.map {|c| c[0] - 65}
data += [?X - 65] * (4 - (data.size + 4) % 5)

deck = Deck.new
keystream = deck.create_keystream(data.size)

puts 'encoded:', data_to_string(encode(data, keystream))
puts 'decoded:', data_to_string(decode(data, keystream))
