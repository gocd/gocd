require_relative 'rainbow/global'
require_relative 'rainbow/legacy'

module Rainbow

  def self.new
    Wrapper.new(global.enabled)
  end

  self.enabled = false unless STDOUT.tty? && STDERR.tty?
  self.enabled = false if ENV['TERM'] == 'dumb'
  self.enabled = true if ENV['CLICOLOR_FORCE'] == '1'

  # On Windows systems, try to load the local ANSI support library if Ruby version < 2
  # Ruby 2.x on Windows includes ANSI support. 
  if RUBY_PLATFORM =~ /mswin|cygwin|mingw/ && RUBY_VERSION.to_i < 2
    begin
      require 'Win32/Console/ANSI'
    rescue LoadError
      self.enabled = false
    end
  end

end
