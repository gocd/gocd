# encoding: UTF-8

module TZInfo
  module Definitions
    module America
      module Montreal
        include TimezoneDefinition
        
        linked_timezone 'America/Montreal', 'America/Toronto'
      end
    end
  end
end
