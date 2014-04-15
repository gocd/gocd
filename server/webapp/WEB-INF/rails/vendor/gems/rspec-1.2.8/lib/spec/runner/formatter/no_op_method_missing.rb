module Spec
  module Runner
    module Formatter
      module NOOPMethodMissing
        def respond_to?(message, include_private = false)
          if include_private
            true
          else
            !private_methods.any? {|m| [message.to_s, message.to_sym].include?(m)}
          end
        end

      private
        
        def method_missing(sym, *args)
          # a no-op
        end
      end
    end
  end
end
