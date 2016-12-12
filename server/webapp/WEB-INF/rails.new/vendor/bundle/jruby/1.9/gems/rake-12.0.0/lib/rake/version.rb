module Rake
  VERSION = "12.0.0"

  module Version # :nodoc: all
    MAJOR, MINOR, BUILD, *OTHER = Rake::VERSION.split "."

    NUMBERS = [MAJOR, MINOR, BUILD, *OTHER]
  end
end
