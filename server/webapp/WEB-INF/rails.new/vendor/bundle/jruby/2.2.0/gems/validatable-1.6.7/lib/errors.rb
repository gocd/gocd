module Validatable
  class Errors
    extend Forwardable
    include Enumerable

    def_delegators :errors, :clear, :each, :each_pair, :empty?, :length, :size

    # call-seq: on(attribute)
    # 
    # * Returns nil, if no errors are associated with the specified +attribute+.
    # * Returns the error message, if one error is associated with the specified +attribute+.
    # * Returns an array of error messages, if more than one error is associated with the specified +attribute+.
    def on(attribute)
      return nil if errors[attribute.to_sym].nil?
      errors[attribute.to_sym].size == 1 ? errors[attribute.to_sym].first : errors[attribute.to_sym]
    end

    def add(attribute, message) #:nodoc:
      errors[attribute.to_sym] = [] if errors[attribute.to_sym].nil?
      errors[attribute.to_sym] << message
    end

    def merge!(errors) #:nodoc:
      errors.each_pair{|k, v| add(k,v)}
      self
    end

    # call-seq: replace(attribute)
    # 
    # * Replaces the errors value for the given +attribute+
    def replace(attribute, value)
      errors[attribute.to_sym] = value
    end

    # call-seq: raw(attribute)
    # 
    # * Returns an array of error messages associated with the specified +attribute+.
    def raw(attribute)
      errors[attribute.to_sym]
    end
    
    def errors #:nodoc:
      @errors ||= {}
    end

    def count #:nodoc:
      errors.values.flatten.size
    end

    # call-seq: full_messages -> an_array_of_messages
    # 
    # Returns an array containing the full list of error messages.
    def full_messages
      full_messages = []

      errors.each_key do |attribute|
        errors[attribute].each do |msg|
          next if msg.nil?

          if attribute.to_s == "base"
            full_messages << msg
          else
            full_messages << humanize(attribute.to_s) + " " + msg
          end
        end
      end
      full_messages
    end
    
    def humanize(lower_case_and_underscored_word) #:nodoc:
      lower_case_and_underscored_word.to_s.gsub(/_id$/, "").gsub(/_/, " ").capitalize
    end
  end
end