# encoding: UTF-8

module TZInfo
  module Definitions
    module Factory
      include TimezoneDefinition
      
      timezone 'Factory' do |tz|
        tz.offset :o0, 0, 0, :'-00'
        
      end
    end
  end
end
