class Symbol
  def to_proc
    proc { |*args| args.shift.__send__(self, *args) } 
  end
end
