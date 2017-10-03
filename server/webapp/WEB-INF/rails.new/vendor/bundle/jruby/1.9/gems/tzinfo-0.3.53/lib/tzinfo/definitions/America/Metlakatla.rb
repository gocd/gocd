# encoding: UTF-8

module TZInfo
  module Definitions
    module America
      module Metlakatla
        include TimezoneDefinition
        
        timezone 'America/Metlakatla' do |tz|
          tz.offset :o0, 54822, 0, :LMT
          tz.offset :o1, -31578, 0, :LMT
          tz.offset :o2, -28800, 0, :PST
          tz.offset :o3, -28800, 3600, :PWT
          tz.offset :o4, -28800, 3600, :PPT
          tz.offset :o5, -28800, 3600, :PDT
          tz.offset :o6, -32400, 0, :AKST
          tz.offset :o7, -32400, 3600, :AKDT
          
          tz.transition 1867, 10, :o1, 34606898863, 14400
          tz.transition 1900, 8, :o2, 34779634063, 14400
          tz.transition 1942, 2, :o3, 29164799, 12
          tz.transition 1945, 8, :o4, 58360379, 24
          tz.transition 1945, 9, :o2, 19453831, 8
          tz.transition 1969, 4, :o5, 29284067, 12
          tz.transition 1969, 10, :o2, 19524167, 8
          tz.transition 1970, 4, :o5, 9972000
          tz.transition 1970, 10, :o2, 25693200
          tz.transition 1971, 4, :o5, 41421600
          tz.transition 1971, 10, :o2, 57747600
          tz.transition 1972, 4, :o5, 73476000
          tz.transition 1972, 10, :o2, 89197200
          tz.transition 1973, 4, :o5, 104925600
          tz.transition 1973, 10, :o2, 120646800
          tz.transition 1974, 1, :o5, 126698400
          tz.transition 1974, 10, :o2, 152096400
          tz.transition 1975, 2, :o5, 162381600
          tz.transition 1975, 10, :o2, 183546000
          tz.transition 1976, 4, :o5, 199274400
          tz.transition 1976, 10, :o2, 215600400
          tz.transition 1977, 4, :o5, 230724000
          tz.transition 1977, 10, :o2, 247050000
          tz.transition 1978, 4, :o5, 262778400
          tz.transition 1978, 10, :o2, 278499600
          tz.transition 1979, 4, :o5, 294228000
          tz.transition 1979, 10, :o2, 309949200
          tz.transition 1980, 4, :o5, 325677600
          tz.transition 1980, 10, :o2, 341398800
          tz.transition 1981, 4, :o5, 357127200
          tz.transition 1981, 10, :o2, 372848400
          tz.transition 1982, 4, :o5, 388576800
          tz.transition 1982, 10, :o2, 404902800
          tz.transition 1983, 4, :o5, 420026400
          tz.transition 1983, 10, :o2, 436352400
          tz.transition 2015, 11, :o6, 1446372000
          tz.transition 2016, 3, :o7, 1457866800
          tz.transition 2016, 11, :o6, 1478426400
          tz.transition 2017, 3, :o7, 1489316400
          tz.transition 2017, 11, :o6, 1509876000
          tz.transition 2018, 3, :o7, 1520766000
          tz.transition 2018, 11, :o6, 1541325600
          tz.transition 2019, 3, :o7, 1552215600
          tz.transition 2019, 11, :o6, 1572775200
          tz.transition 2020, 3, :o7, 1583665200
          tz.transition 2020, 11, :o6, 1604224800
          tz.transition 2021, 3, :o7, 1615719600
          tz.transition 2021, 11, :o6, 1636279200
          tz.transition 2022, 3, :o7, 1647169200
          tz.transition 2022, 11, :o6, 1667728800
          tz.transition 2023, 3, :o7, 1678618800
          tz.transition 2023, 11, :o6, 1699178400
          tz.transition 2024, 3, :o7, 1710068400
          tz.transition 2024, 11, :o6, 1730628000
          tz.transition 2025, 3, :o7, 1741518000
          tz.transition 2025, 11, :o6, 1762077600
          tz.transition 2026, 3, :o7, 1772967600
          tz.transition 2026, 11, :o6, 1793527200
          tz.transition 2027, 3, :o7, 1805022000
          tz.transition 2027, 11, :o6, 1825581600
          tz.transition 2028, 3, :o7, 1836471600
          tz.transition 2028, 11, :o6, 1857031200
          tz.transition 2029, 3, :o7, 1867921200
          tz.transition 2029, 11, :o6, 1888480800
          tz.transition 2030, 3, :o7, 1899370800
          tz.transition 2030, 11, :o6, 1919930400
          tz.transition 2031, 3, :o7, 1930820400
          tz.transition 2031, 11, :o6, 1951380000
          tz.transition 2032, 3, :o7, 1962874800
          tz.transition 2032, 11, :o6, 1983434400
          tz.transition 2033, 3, :o7, 1994324400
          tz.transition 2033, 11, :o6, 2014884000
          tz.transition 2034, 3, :o7, 2025774000
          tz.transition 2034, 11, :o6, 2046333600
          tz.transition 2035, 3, :o7, 2057223600
          tz.transition 2035, 11, :o6, 2077783200
          tz.transition 2036, 3, :o7, 2088673200
          tz.transition 2036, 11, :o6, 2109232800
          tz.transition 2037, 3, :o7, 2120122800
          tz.transition 2037, 11, :o6, 2140682400
          tz.transition 2038, 3, :o7, 59171927, 24
          tz.transition 2038, 11, :o6, 29588819, 12
          tz.transition 2039, 3, :o7, 59180663, 24
          tz.transition 2039, 11, :o6, 29593187, 12
          tz.transition 2040, 3, :o7, 59189399, 24
          tz.transition 2040, 11, :o6, 29597555, 12
          tz.transition 2041, 3, :o7, 59198135, 24
          tz.transition 2041, 11, :o6, 29601923, 12
          tz.transition 2042, 3, :o7, 59206871, 24
          tz.transition 2042, 11, :o6, 29606291, 12
          tz.transition 2043, 3, :o7, 59215607, 24
          tz.transition 2043, 11, :o6, 29610659, 12
          tz.transition 2044, 3, :o7, 59224511, 24
          tz.transition 2044, 11, :o6, 29615111, 12
          tz.transition 2045, 3, :o7, 59233247, 24
          tz.transition 2045, 11, :o6, 29619479, 12
          tz.transition 2046, 3, :o7, 59241983, 24
          tz.transition 2046, 11, :o6, 29623847, 12
          tz.transition 2047, 3, :o7, 59250719, 24
          tz.transition 2047, 11, :o6, 29628215, 12
          tz.transition 2048, 3, :o7, 59259455, 24
          tz.transition 2048, 11, :o6, 29632583, 12
          tz.transition 2049, 3, :o7, 59268359, 24
          tz.transition 2049, 11, :o6, 29637035, 12
          tz.transition 2050, 3, :o7, 59277095, 24
          tz.transition 2050, 11, :o6, 29641403, 12
        end
      end
    end
  end
end
