# encoding: UTF-8

module TZInfo
  module Definitions
    module Antarctica
      module DumontDUrville
        include TimezoneDefinition
        
        timezone 'Antarctica/DumontDUrville' do |tz|
          tz.offset :o0, 0, 0, :'-00'
          tz.offset :o1, 36000, 0, :'+10'
          
          tz.transition 1947, 1, :o1, 4864373, 2
          tz.transition 1952, 1, :o0, 29208301, 12
          tz.transition 1956, 11, :o1, 4871557, 2
        end
      end
    end
  end
end
