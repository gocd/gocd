# Copyright (c) 2010 ThoughtWorks Inc. (http://thoughtworks.com)
# Licenced under the MIT License (http://www.opensource.org/licenses/mit-license.php)

module Oauth2Provider
  module TransactionHelper
    def self.included(receiver)
      receiver.extend         ClassMethods
    end

    class TransactionFilter
      class << self
        def around(controller, &block)
          ModelBase.transaction(&block)
        end
      end
    end

    module ClassMethods
      def transaction_actions(*actions)
        self.around_action TransactionFilter, :only => actions
      end
    end
  end
end