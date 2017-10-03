# encoding: UTF-8

module TZInfo
  module Definitions
    module Pacific
      module Tongatapu
        include TimezoneDefinition
        
        timezone 'Pacific/Tongatapu' do |tz|
          tz.offset :o0, 44360, 0, :LMT
          tz.offset :o1, 44400, 0, :'+1220'
          tz.offset :o2, 46800, 0, :'+13'
          tz.offset :o3, 46800, 3600, :'+14'
          
          tz.transition 1900, 12, :o1, 5217231571, 2160
          tz.transition 1940, 12, :o2, 174959639, 72
          tz.transition 1999, 10, :o3, 939214800
          tz.transition 2000, 3, :o2, 953384400
          tz.transition 2000, 11, :o3, 973342800
          tz.transition 2001, 1, :o2, 980596800
          tz.transition 2001, 11, :o3, 1004792400
          tz.transition 2002, 1, :o2, 1012046400
          tz.transition 2016, 11, :o3, 1478350800
          tz.transition 2017, 1, :o2, 1484398800
          tz.transition 2017, 11, :o3, 1509800400
          tz.transition 2018, 1, :o2, 1516453200
          tz.transition 2018, 11, :o3, 1541250000
          tz.transition 2019, 1, :o2, 1547902800
          tz.transition 2019, 11, :o3, 1572699600
          tz.transition 2020, 1, :o2, 1579352400
          tz.transition 2020, 10, :o3, 1604149200
          tz.transition 2021, 1, :o2, 1610802000
          tz.transition 2021, 11, :o3, 1636203600
          tz.transition 2022, 1, :o2, 1642251600
          tz.transition 2022, 11, :o3, 1667653200
          tz.transition 2023, 1, :o2, 1673701200
          tz.transition 2023, 11, :o3, 1699102800
          tz.transition 2024, 1, :o2, 1705755600
          tz.transition 2024, 11, :o3, 1730552400
          tz.transition 2025, 1, :o2, 1737205200
          tz.transition 2025, 11, :o3, 1762002000
          tz.transition 2026, 1, :o2, 1768654800
          tz.transition 2026, 10, :o3, 1793451600
          tz.transition 2027, 1, :o2, 1800104400
          tz.transition 2027, 11, :o3, 1825506000
          tz.transition 2028, 1, :o2, 1831554000
          tz.transition 2028, 11, :o3, 1856955600
          tz.transition 2029, 1, :o2, 1863608400
          tz.transition 2029, 11, :o3, 1888405200
          tz.transition 2030, 1, :o2, 1895058000
          tz.transition 2030, 11, :o3, 1919854800
          tz.transition 2031, 1, :o2, 1926507600
          tz.transition 2031, 11, :o3, 1951304400
          tz.transition 2032, 1, :o2, 1957957200
          tz.transition 2032, 11, :o3, 1983358800
          tz.transition 2033, 1, :o2, 1989406800
          tz.transition 2033, 11, :o3, 2014808400
          tz.transition 2034, 1, :o2, 2020856400
          tz.transition 2034, 11, :o3, 2046258000
          tz.transition 2035, 1, :o2, 2052910800
          tz.transition 2035, 11, :o3, 2077707600
          tz.transition 2036, 1, :o2, 2084360400
          tz.transition 2036, 11, :o3, 2109157200
          tz.transition 2037, 1, :o2, 2115810000
          tz.transition 2037, 10, :o3, 2140606800
          tz.transition 2038, 1, :o2, 59170561, 24
          tz.transition 2038, 11, :o3, 59177617, 24
          tz.transition 2039, 1, :o2, 59179297, 24
          tz.transition 2039, 11, :o3, 59186353, 24
          tz.transition 2040, 1, :o2, 59188033, 24
          tz.transition 2040, 11, :o3, 59195089, 24
          tz.transition 2041, 1, :o2, 59196937, 24
          tz.transition 2041, 11, :o3, 59203825, 24
          tz.transition 2042, 1, :o2, 59205673, 24
          tz.transition 2042, 11, :o3, 59212561, 24
          tz.transition 2043, 1, :o2, 59214409, 24
          tz.transition 2043, 10, :o3, 59221297, 24
          tz.transition 2044, 1, :o2, 59223145, 24
          tz.transition 2044, 11, :o3, 59230201, 24
          tz.transition 2045, 1, :o2, 59231881, 24
          tz.transition 2045, 11, :o3, 59238937, 24
          tz.transition 2046, 1, :o2, 59240785, 24
          tz.transition 2046, 11, :o3, 59247673, 24
          tz.transition 2047, 1, :o2, 59249521, 24
          tz.transition 2047, 11, :o3, 59256409, 24
          tz.transition 2048, 1, :o2, 59258257, 24
          tz.transition 2048, 10, :o3, 59265145, 24
          tz.transition 2049, 1, :o2, 59266993, 24
          tz.transition 2049, 11, :o3, 59274049, 24
          tz.transition 2050, 1, :o2, 59275729, 24
        end
      end
    end
  end
end
