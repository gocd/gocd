# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Riyadh
        include TimezoneDefinition
        
        timezone 'Asia/Riyadh' do |tz|
          tz.offset :o0, 11212, 0, :LMT
          tz.offset :o1, 10800, 0, :'+03'
          
          tz.transition 1947, 3, :o1, 52536780797, 21600
        end
      end
    end
  end
end
