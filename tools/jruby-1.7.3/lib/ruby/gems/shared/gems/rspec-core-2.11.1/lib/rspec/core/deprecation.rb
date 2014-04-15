module RSpec
  class << self
    # @private
    #
    # Used internally to print deprecation warnings
    def deprecate(method, alternate_method=nil, version=nil)
      version_string = version ? "rspec-#{version}" : "a future version of RSpec"

      message = <<-NOTICE

*****************************************************************
DEPRECATION WARNING: you are using deprecated behaviour that will
be removed from #{version_string}.

#{caller(0)[2]}

* #{method} is deprecated.
NOTICE
      if alternate_method
        message << <<-ADDITIONAL
* please use #{alternate_method} instead.
ADDITIONAL
      end

      message << "*****************************************************************"
      warn_deprecation(message)
    end

    # @private
    #
    # Used internally to print deprecation warnings
    def warn_deprecation(message)
      send :warn, message
    end
  end
end
