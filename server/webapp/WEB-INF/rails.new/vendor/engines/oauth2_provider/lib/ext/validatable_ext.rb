# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

require 'validatable'

module Validatable
  class Errors
    unless method_defined?(:add_without_humanize_options)
      alias :add_without_humanize_options :add
      def add(attribute, message, humanized_name=nil) #:nodoc:
        humanized_names[attribute.to_sym] = humanized_name
        add_without_humanize_options(attribute, message)
      end
    
      alias :humanize_without_humanize_options :humanize
    
      def humanize(lower_case_and_underscored_word) #:nodoc:
        humanized_names[lower_case_and_underscored_word.to_sym] || humanize_without_humanize_options(lower_case_and_underscored_word)
      end

      private
      def humanized_names
        @humanized_names ||= {}
      end
    end
  end
end
