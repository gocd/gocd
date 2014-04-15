def succ?(*a)
  begin
    (0..(a.size-2)).each {|i| return false if a[i].succ != a[i+1]}
    return true
  rescue
    return false
  end
end

class Deck
  def initialize
    @deck = (1..52).to_a + [:A, :B]
  end

  def move_a
    move_down(@deck.index(:A))
  end

  def move_b
    move_down(move_down(@deck.index(:B)))
  end

  def move_down(index)
    if index < 53
      new = index + 1
      @deck[new], @deck[index] = @deck[index], @deck[new]
    else
      @deck = @deck[0,1] + @deck[-1,1] + @deck[1..-2]
      new = 1
    end
    return new
  end

  def triple_cut
    jokers = [@deck.index(:A), @deck.index(:B)]
    top, bottom = jokers.min, jokers.max
    @deck = @deck[(bottom+1)..-1] + @deck[top..bottom] + @deck[0...top]
  end

  def number_value(x)
    return 53 if x == :A or x == :B
    return x
  end

  def count_cut
    count = number_value( @deck[-1] )
    @deck = @deck[count..-2] + @deck[0,count] + @deck[-1,1]
  end

  def output
    return @deck[ number_value(@deck[0]) ]
  end

  def update
    move_a
    move_b
    triple_cut
    count_cut
  end

  def get
    while true
      update
      c = output
      if c != :A and c != :B
        letter = ( ((c-1) % 26) + 65 ).chr
        return letter
      end
    end
  end

  def to_s
    a = []
    @deck.each_index {|i|
      if  succ?(a[-1], @deck[i], @deck[i+1])
        a << "..."
      elsif a[-1] == "..." and succ?(@deck[i-1], @deck[i], @deck[i+1])
        # nop
      else
        a << @deck[i]
      end
    }
    return a.join(" ")
  end
end

class Encrypter
  def initialize(keystream)
    @keystream = keystream
  end

  def sanitize(s)
    s = s.upcase
    s = s.gsub(/[^A-Z]/, "")
    s = s + "X" * ((5 - s.size % 5) % 5)
    out = ""
    (s.size / 5).times {|i| out << s[i*5,5] << " "}
    return out
  end

  def mod(c)
    return c - 26 if c > 26
    return c + 26 if c < 1
    return c
  end

  def process(s, &combiner)
    s = sanitize(s)
    out = ""
    s.each_byte { |c|
      if c >= ?A and c <= ?Z
        key = @keystream.get
        res = combiner.call(c, key[0])
        out << res.chr
      else
        out << c.chr
      end
    }
    return out
  end

  def encrypt(s)
    return process(s) {|c, key| 64 + mod(c + key - 128)}
  end

  def decrypt(s)
    return process(s) {|c, key| 64 + mod(c -key)}
  end
end


def test
  d = Deck.new()
  d.update
  puts d

  e = Encrypter.new( Deck.new() )
  cipher =  e.encrypt('Code in Ruby, live longer!')
  puts cipher

  e = Encrypter.new( Deck.new() )
  puts e.decrypt(cipher)

  e = Encrypter.new( Deck.new() )
  puts e.decrypt("CLEPK HHNIY CFPWH FDFEH")

  e = Encrypter.new( Deck.new() )
  puts e.decrypt("ABVAW LWZSY OORYK DUPVH")
end

test()
