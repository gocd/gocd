# encoding: UTF-8

module TZInfo
  module Definitions
    module Antarctica
      module Vostok
        include TimezoneDefinition
        
        timezone 'Antarctica/Vostok' do |tz|
          tz.offset :o0, 0, 0, :'-00'
          tz.offset :o1, 21600, 0, :VOST
          
          tz.transition 1957, 12, :o1, 4872377, 2
        end
      end
    end
  end
end
