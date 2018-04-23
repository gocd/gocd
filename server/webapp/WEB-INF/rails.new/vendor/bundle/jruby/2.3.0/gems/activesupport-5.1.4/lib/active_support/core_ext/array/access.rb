class Array
  # Returns the tail of the array from +position+.
  #
  #   %w( a b c d ).from(0)  # => ["a", "b", "c", "d"]
  #   %w( a b c d ).from(2)  # => ["c", "d"]
  #   %w( a b c d ).from(10) # => []
  #   %w().from(0)           # => []
  #   %w( a b c d ).from(-2) # => ["c", "d"]
  #   %w( a b c ).from(-10)  # => []
  def from(position)
    self[position, length] || []
  end

  # Returns the beginning of the array up to +position+.
  #
  #   %w( a b c d ).to(0)  # => ["a"]
  #   %w( a b c d ).to(2)  # => ["a", "b", "c"]
  #   %w( a b c d ).to(10) # => ["a", "b", "c", "d"]
  #   %w().to(0)           # => []
  #   %w( a b c d ).to(-2) # => ["a", "b", "c"]
  #   %w( a b c ).to(-10)  # => []
  def to(position)
    if position >= 0
      take position + 1
    else
      self[0..position]
    end
  end

  # Returns a copy of the Array without the specified elements.
  #
  #   people = ["David", "Rafael", "Aaron", "Todd"]
  #   people.without "Aaron", "Todd"
  #   # => ["David", "Rafael"]
  #
  # Note: This is an optimization of `Enumerable#without` that uses `Array#-`
  # instead of `Array#reject` for performance reasons.
  def without(*elements)
    self - elements
  end

  # Equal to <tt>self[1]</tt>.
  #
  #   %w( a b c d e ).second # => "b"
  def second
    self[1]
  end

  # Equal to <tt>self[2]</tt>.
  #
  #   %w( a b c d e ).third # => "c"
  def third
    self[2]
  end

  # Equal to <tt>self[3]</tt>.
  #
  #   %w( a b c d e ).fourth # => "d"
  def fourth
    self[3]
  end

  # Equal to <tt>self[4]</tt>.
  #
  #   %w( a b c d e ).fifth # => "e"
  def fifth
    self[4]
  end

  # Equal to <tt>self[41]</tt>. Also known as accessing "the reddit".
  #
  #   (1..42).to_a.forty_two # => 42
  def forty_two
    self[41]
  end

  # Equal to <tt>self[-3]</tt>.
  #
  #   %w( a b c d e ).third_to_last # => "c"
  def third_to_last
    self[-3]
  end

  # Equal to <tt>self[-2]</tt>.
  #
  #   %w( a b c d e ).second_to_last # => "d"
  def second_to_last
    self[-2]
  end
end
