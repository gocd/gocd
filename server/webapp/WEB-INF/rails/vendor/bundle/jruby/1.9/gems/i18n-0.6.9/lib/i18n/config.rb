module I18n
  class Config
    # The only configuration value that is not global and scoped to thread is :locale.
    # It defaults to the default_locale.
    def locale
      @locale ||= default_locale
    end

    # Sets the current locale pseudo-globally, i.e. in the Thread.current hash.
    def locale=(locale)
      I18n.enforce_available_locales!(locale)
      @locale = locale.to_sym rescue nil
    end

    # Returns the current backend. Defaults to +Backend::Simple+.
    def backend
      @@backend ||= Backend::Simple.new
    end

    # Sets the current backend. Used to set a custom backend.
    def backend=(backend)
      @@backend = backend
    end

    # Returns the current default locale. Defaults to :'en'
    def default_locale
      @@default_locale ||= :en
    end

    # Sets the current default locale. Used to set a custom default locale.
    def default_locale=(locale)
      I18n.enforce_available_locales!(locale)
      @@default_locale = locale.to_sym rescue nil
    end

    # Returns an array of locales for which translations are available.
    # Unless you explicitely set these through I18n.available_locales=
    # the call will be delegated to the backend.
    def available_locales
      @@available_locales ||= nil
      @@available_locales || backend.available_locales
    end

    # Sets the available locales.
    def available_locales=(locales)
      @@available_locales = Array(locales).map { |locale| locale.to_sym }
      @@available_locales = nil if @@available_locales.empty?
    end

    # Returns the current default scope separator. Defaults to '.'
    def default_separator
      @@default_separator ||= '.'
    end

    # Sets the current default scope separator.
    def default_separator=(separator)
      @@default_separator = separator
    end

    # Return the current exception handler. Defaults to :default_exception_handler.
    def exception_handler
      @@exception_handler ||= ExceptionHandler.new
    end

    # Sets the exception handler.
    def exception_handler=(exception_handler)
      @@exception_handler = exception_handler
    end

    # Returns the current handler for situations when interpolation argument
    # is missing. MissingInterpolationArgument will be raised by default.
    def missing_interpolation_argument_handler
      @@missing_interpolation_argument_handler ||= lambda do |missing_key, provided_hash, string|
        raise MissingInterpolationArgument.new(missing_key, provided_hash, string)
      end
    end

    # Sets the missing interpolation argument handler. It can be any
    # object that responds to #call. The arguments that will be passed to #call
    # are the same as for MissingInterpolationArgument initializer. Use +Proc.new+
    # if you don't care about arity.
    #
    # == Example:
    # You can supress raising an exception and return string instead:
    #
    #   I18n.config.missing_interpolation_argument_handler = Proc.new do |key|
    #     "#{key} is missing"
    #   end
    def missing_interpolation_argument_handler=(exception_handler)
      @@missing_interpolation_argument_handler = exception_handler
    end

    # Allow clients to register paths providing translation data sources. The
    # backend defines acceptable sources.
    #
    # E.g. the provided SimpleBackend accepts a list of paths to translation
    # files which are either named *.rb and contain plain Ruby Hashes or are
    # named *.yml and contain YAML data. So for the SimpleBackend clients may
    # register translation files like this:
    #   I18n.load_path << 'path/to/locale/en.yml'
    def load_path
      @@load_path ||= []
    end

    # Sets the load path instance. Custom implementations are expected to
    # behave like a Ruby Array.
    def load_path=(load_path)
      @@load_path = load_path
    end

    # [Deprecated] this will default to true in the future
    # Defaults to nil so that it triggers the deprecation warning
    def enforce_available_locales
      defined?(@@enforce_available_locales) ? @@enforce_available_locales : nil
    end

    def enforce_available_locales=(enforce_available_locales)
      @@enforce_available_locales = enforce_available_locales
    end
  end
end
