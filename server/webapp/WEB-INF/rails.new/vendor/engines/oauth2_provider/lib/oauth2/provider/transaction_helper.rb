# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2
  module Provider
    module TransactionHelper
      def self.included(receiver)
        receiver.extend         ClassMethods
      end
      
      class TransactionFilter
        def filter(controller, &block)
          ModelBase.transaction(&block)
        end
      end
  
      module ClassMethods
        def transaction_actions(*actions)
          self.around_filter TransactionFilter.new, :only => actions
        end
      end
    end
  end
end