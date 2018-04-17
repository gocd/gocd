# encoding: UTF-8

# This file contains data derived from the IANA Time Zone Database
# (http://www.iana.org/time-zones).

module TZInfo
  module Data
    module Definitions
      module Etc
        module GMT__m__13
          include TimezoneDefinition
          
          timezone 'Etc/GMT-13' do |tz|
            tz.offset :o0, 46800, 0, :'+13'
            
          end
        end
      end
    end
  end
end
