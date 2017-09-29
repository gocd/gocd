module Sickill
  module Rainbow

    def self.enabled=(value)
      STDERR.puts("Rainbow gem notice: Sickill::Rainbow.enabled= is " \
                  "deprecated, use Rainbow.enabled= instead.")
      ::Rainbow.enabled = value
    end

    def self.enabled
      ::Rainbow.enabled
    end

  end
end
