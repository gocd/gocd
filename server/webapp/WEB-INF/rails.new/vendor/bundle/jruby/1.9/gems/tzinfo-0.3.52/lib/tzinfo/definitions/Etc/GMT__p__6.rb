# encoding: UTF-8

module TZInfo
  module Definitions
    module Etc
      module GMT__p__6
        include TimezoneDefinition
        
        timezone 'Etc/GMT+6' do |tz|
          tz.offset :o0, -21600, 0, :'-06'
          
        end
      end
    end
  end
end
