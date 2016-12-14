# encoding: UTF-8

module TZInfo
  module Definitions
    module Antarctica
      module Syowa
        include TimezoneDefinition
        
        timezone 'Antarctica/Syowa' do |tz|
          tz.offset :o0, 0, 0, :'-00'
          tz.offset :o1, 10800, 0, :'+03'
          
          tz.transition 1957, 1, :o1, 4871735, 2
        end
      end
    end
  end
end
