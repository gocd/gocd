class Java::JavaUtilRegex::Pattern
  def =~(str)
    m = self.matcher(str)
    m.find ? m.start : nil
  end
  
  def ===(str)
    self.matcher(str).find
  end

  def match(str)
    m = self.matcher(str)
    m.str = str
    m.find ? m : nil
  end
end

class Java::JavaUtilRegex::Matcher
  attr_accessor :str
  
  def captures
    g = self.group_count
    capt = []
    count.times do |i|
      capt << self.group(i+1)
    end
    capt
  end
  
  def [](*args)
    self.to_a[*args]
  end

  def begin(ix)
    self.start(ix)
  end
  
  def end(ix)
    self.end(ix)
  end
  
  def to_a
    arr = []
    self.group_count.times do |gg|
      if self.start(gg) == -1
        arr << nil
      else
        arr << self.group(gg)
      end
    end
    arr
  end
  
  def size
    self.group_count
  end
  
  alias length size
  
  def values_at(*args)
    self.to_a.values_at(*args)
  end

  def select
    yield self.to_a
  end
  
  def offset(ix)
    [self.start(ix), self.end(ix)]
  end

  def pre_match
    self.str[0..(self.start(0))]
  end
  
  def post_match
    self.str[(self.end(0))..-1]
  end
  
  def string
    self.group(0)
  end
end
