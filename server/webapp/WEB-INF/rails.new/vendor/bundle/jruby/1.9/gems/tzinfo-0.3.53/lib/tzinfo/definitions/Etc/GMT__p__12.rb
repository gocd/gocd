# encoding: UTF-8

module TZInfo
  module Definitions
    module Etc
      module GMT__p__12
        include TimezoneDefinition
        
        timezone 'Etc/GMT+12' do |tz|
          tz.offset :o0, -43200, 0, :'-12'
          
        end
      end
    end
  end
end
