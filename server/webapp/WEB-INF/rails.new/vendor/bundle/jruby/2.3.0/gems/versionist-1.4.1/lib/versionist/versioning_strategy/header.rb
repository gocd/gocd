module Versionist
  module VersioningStrategy
    # Implements the header versioning strategy.
    class Header < Base

      # Creates a new Header VersioningStrategy object. config must contain the following keys:
      # - :header the header hash to inspect
      def initialize(config)
        super
        raise ArgumentError, "you must specify :name in the :header configuration Hash" if !config[:header].has_key?(:name)
        raise ArgumentError, "you must specify :value in the :header configuration Hash" if !config[:header].has_key?(:value)
        Versionist.configuration.header_versions << self if !Versionist.configuration.header_versions.include?(self)
      end

      def matches?(request)
        header_string = request.headers[config[:header][:name]].to_s
        if !header_string.blank?
          potential_matches = Versionist.configuration.header_versions.select {|hv| header_string.include?(hv.config[:header][:value])}
          if !potential_matches.empty?
            if potential_matches.include?(self)
              if potential_matches.size == 1
                return true
              else
                # when finding multiple potential matches, the match with the longest value wins
                #  (i.e. v2.1 trumps v2), as one is a subset of the other
                longest = potential_matches.max {|a,b| a.config[:header][:value].length <=> b.config[:header][:value].length}
                return longest == self
              end
            end
          end
        end
        false
      end

      def ==(other)
        super
        return false if !other.is_a?(Versionist::VersioningStrategy::Header)
        return config[:header][:name] == other.config[:header][:name] && self.config[:header][:value] == other.config[:header][:value]
      end
    end
  end
end
