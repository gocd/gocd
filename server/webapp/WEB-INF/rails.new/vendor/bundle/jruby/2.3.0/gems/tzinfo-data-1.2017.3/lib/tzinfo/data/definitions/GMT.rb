# encoding: UTF-8

# This file contains data derived from the IANA Time Zone Database
# (http://www.iana.org/time-zones).

module TZInfo
  module Data
    module Definitions
      module GMT
        include TimezoneDefinition
        
        linked_timezone 'GMT', 'Etc/GMT'
      end
    end
  end
end
