class IO
  public
  def nonblock?
    !JRuby.reference(self).blocking?
  end

  def nonblock=(nonblocking)
    JRuby.reference(self).blocking = !nonblocking
  end

  def nonblock(nonblocking = true)
    JRuby.reference(self).blocking = !nonblocking;
    if block_given?
      begin
        yield self
      ensure
        JRuby.reference(self).blocking = nonblocking;
      end
    end
  end
end