# TODO java.util.Comparator support?
module java::util::Map
  include Enumerable
  def each(&block)
    entrySet.each { |pair| block.call([pair.key, pair.value]) }
  end
  def [](key)
    get(key)
  end
  def []=(key,val)
    put(key,val)
    val
  end
end

module java::lang::Iterable
  include Enumerable

  def each
    iter = iterator
    yield(iter.next) while iter.hasNext
  end

  def each_with_index
    index = 0
    iter = iterator
    while iter.hasNext
      yield(iter.next, index)
      index += 1
    end
  end
end
  
module java::lang::Comparable
  include Comparable
  def <=>(a)
    return nil if a.nil?
    compareTo(a)
  end
end

module java::util::Collection
  include Enumerable
  def each(&block)
    iter = iterator
    while iter.hasNext
      block.call(iter.next)
    end
  end
  def <<(a); add(a); self; end
  def +(oth)
    nw = self.dup
    nw.addAll(oth)
    nw
  end
  def -(oth)
    nw = self.dup
    nw.removeAll(oth)
    nw
  end
  def length
    self.size
  end
  def join(*args)
    self.to_a.join(*args)
  end
end

module java::util::Enumeration
  include Enumerable
  
  def each
    while (has_more_elements)
      yield next_element
    end
  end
end

module java::util::Iterator
  include Enumerable
  
  def each
    while (has_next)
      yield self.next
    end
  end
end

module java::util::List
  def [](ix1, ix2 = nil)
    if (ix2)
      sub_list(ix1, ix1 + ix2)
    elsif (ix1.is_a?(Range))
      sub_list(ix1.first, ix1.last)
    elsif ix1 < size
      get(ix1)
    else
      nil
    end
  end
  def []=(ix,val)
    if (ix.is_a?(Range))
      ix.each { |i| remove(i) }
      add_all(ix.first, val)
    elsif size < ix
      ((ix-size)+1).times { self << nil }
    end
    set(ix,val)
    val
  end
  def sort()
    comparator = java::util::Comparator.new
    
    if block_given?
      # These gymnastics are needed because using def will not capture the block in it's closure
      comparator_singleton = (class << comparator; self; end)
      comparator_singleton.send :define_method, :compare do |o1, o2|
        yield(o1, o2)
      end
    else
      def comparator.compare(o1, o2)
        o1 <=> o2
      end
    end

    # This should probably return a new instance of self class instead of ArrayList
    list = java::util::ArrayList.new
    list.addAll(self)

    java::util::Collections.sort(list, comparator)

    list
  end
  def sort!()
    comparator = java::util::Comparator.new
    if block_given?
      # These gymnastics are needed because using def will not capture the block in it's closure
      comparator_singleton = (class << comparator; self; end)
      comparator_singleton.send :define_method, :compare do |o1, o2|
        yield(o1, o2)
      end
    else
      def comparator.compare(o1, o2)
        o1 <=> o2
      end
    end

    java::util::Collections.sort(java_object, comparator)

    self
  end
  def _wrap_yield(*args)
    p = yield(*args)
    p p
  end
end
