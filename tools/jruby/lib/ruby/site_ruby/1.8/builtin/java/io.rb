require 'jruby'

class java::io::InputStream
  def to_io
    JRuby.dereference(org.jruby.RubyIO.new(JRuby.runtime, self))
  end
end

class java::io::OutputStream
  def to_io
    JRuby.dereference(org.jruby.RubyIO.new(JRuby.runtime, self))
  end
end

module java::nio::channels::Channel
  def to_io
    JRuby.dereference(org.jruby.RubyIO.new(JRuby.runtime, self))
  end
end
