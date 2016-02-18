# encoding: UTF-8

module TZInfo
  module Definitions
    module Asia
      module Hovd
        include TimezoneDefinition
        
        timezone 'Asia/Hovd' do |tz|
          tz.offset :o0, 21996, 0, :LMT
          tz.offset :o1, 21600, 0, :HOVT
          tz.offset :o2, 25200, 0, :HOVT
          tz.offset :o3, 25200, 3600, :HOVST
          
          tz.transition 1905, 7, :o1, 5800939789, 2400
          tz.transition 1977, 12, :o2, 252439200
          tz.transition 1983, 3, :o3, 417978000
          tz.transition 1983, 9, :o2, 433785600
          tz.transition 1984, 3, :o3, 449600400
          tz.transition 1984, 9, :o2, 465321600
          tz.transition 1985, 3, :o3, 481050000
          tz.transition 1985, 9, :o2, 496771200
          tz.transition 1986, 3, :o3, 512499600
          tz.transition 1986, 9, :o2, 528220800
          tz.transition 1987, 3, :o3, 543949200
          tz.transition 1987, 9, :o2, 559670400
          tz.transition 1988, 3, :o3, 575398800
          tz.transition 1988, 9, :o2, 591120000
          tz.transition 1989, 3, :o3, 606848400
          tz.transition 1989, 9, :o2, 622569600
          tz.transition 1990, 3, :o3, 638298000
          tz.transition 1990, 9, :o2, 654624000
          tz.transition 1991, 3, :o3, 670352400
          tz.transition 1991, 9, :o2, 686073600
          tz.transition 1992, 3, :o3, 701802000
          tz.transition 1992, 9, :o2, 717523200
          tz.transition 1993, 3, :o3, 733251600
          tz.transition 1993, 9, :o2, 748972800
          tz.transition 1994, 3, :o3, 764701200
          tz.transition 1994, 9, :o2, 780422400
          tz.transition 1995, 3, :o3, 796150800
          tz.transition 1995, 9, :o2, 811872000
          tz.transition 1996, 3, :o3, 828205200
          tz.transition 1996, 9, :o2, 843926400
          tz.transition 1997, 3, :o3, 859654800
          tz.transition 1997, 9, :o2, 875376000
          tz.transition 1998, 3, :o3, 891104400
          tz.transition 1998, 9, :o2, 906825600
          tz.transition 2001, 4, :o3, 988398000
          tz.transition 2001, 9, :o2, 1001700000
          tz.transition 2002, 3, :o3, 1017428400
          tz.transition 2002, 9, :o2, 1033149600
          tz.transition 2003, 3, :o3, 1048878000
          tz.transition 2003, 9, :o2, 1064599200
          tz.transition 2004, 3, :o3, 1080327600
          tz.transition 2004, 9, :o2, 1096048800
          tz.transition 2005, 3, :o3, 1111777200
          tz.transition 2005, 9, :o2, 1127498400
          tz.transition 2006, 3, :o3, 1143226800
          tz.transition 2006, 9, :o2, 1159552800
          tz.transition 2015, 3, :o3, 1427482800
          tz.transition 2015, 9, :o2, 1443196800
          tz.transition 2016, 3, :o3, 1458932400
          tz.transition 2016, 9, :o2, 1474646400
          tz.transition 2017, 3, :o3, 1490382000
          tz.transition 2017, 9, :o2, 1506700800
          tz.transition 2018, 3, :o3, 1522436400
          tz.transition 2018, 9, :o2, 1538150400
          tz.transition 2019, 3, :o3, 1553886000
          tz.transition 2019, 9, :o2, 1569600000
          tz.transition 2020, 3, :o3, 1585335600
          tz.transition 2020, 9, :o2, 1601049600
          tz.transition 2021, 3, :o3, 1616785200
          tz.transition 2021, 9, :o2, 1632499200
          tz.transition 2022, 3, :o3, 1648234800
          tz.transition 2022, 9, :o2, 1663948800
          tz.transition 2023, 3, :o3, 1679684400
          tz.transition 2023, 9, :o2, 1696003200
          tz.transition 2024, 3, :o3, 1711738800
          tz.transition 2024, 9, :o2, 1727452800
          tz.transition 2025, 3, :o3, 1743188400
          tz.transition 2025, 9, :o2, 1758902400
          tz.transition 2026, 3, :o3, 1774638000
          tz.transition 2026, 9, :o2, 1790352000
          tz.transition 2027, 3, :o3, 1806087600
          tz.transition 2027, 9, :o2, 1821801600
          tz.transition 2028, 3, :o3, 1837537200
          tz.transition 2028, 9, :o2, 1853856000
          tz.transition 2029, 3, :o3, 1869591600
          tz.transition 2029, 9, :o2, 1885305600
          tz.transition 2030, 3, :o3, 1901041200
          tz.transition 2030, 9, :o2, 1916755200
          tz.transition 2031, 3, :o3, 1932490800
          tz.transition 2031, 9, :o2, 1948204800
          tz.transition 2032, 3, :o3, 1963940400
          tz.transition 2032, 9, :o2, 1979654400
          tz.transition 2033, 3, :o3, 1995390000
          tz.transition 2033, 9, :o2, 2011104000
          tz.transition 2034, 3, :o3, 2026839600
          tz.transition 2034, 9, :o2, 2043158400
          tz.transition 2035, 3, :o3, 2058894000
          tz.transition 2035, 9, :o2, 2074608000
          tz.transition 2036, 3, :o3, 2090343600
          tz.transition 2036, 9, :o2, 2106057600
          tz.transition 2037, 3, :o3, 2121793200
          tz.transition 2037, 9, :o2, 2137507200
          tz.transition 2038, 3, :o3, 59172223, 24
          tz.transition 2038, 9, :o2, 14794147, 6
          tz.transition 2039, 3, :o3, 59180959, 24
          tz.transition 2039, 9, :o2, 14796331, 6
          tz.transition 2040, 3, :o3, 59189863, 24
          tz.transition 2040, 9, :o2, 14798557, 6
          tz.transition 2041, 3, :o3, 59198599, 24
          tz.transition 2041, 9, :o2, 14800741, 6
          tz.transition 2042, 3, :o3, 59207335, 24
          tz.transition 2042, 9, :o2, 14802925, 6
          tz.transition 2043, 3, :o3, 59216071, 24
          tz.transition 2043, 9, :o2, 14805109, 6
          tz.transition 2044, 3, :o3, 59224807, 24
          tz.transition 2044, 9, :o2, 14807293, 6
          tz.transition 2045, 3, :o3, 59233543, 24
          tz.transition 2045, 9, :o2, 14809519, 6
          tz.transition 2046, 3, :o3, 59242447, 24
          tz.transition 2046, 9, :o2, 14811703, 6
          tz.transition 2047, 3, :o3, 59251183, 24
          tz.transition 2047, 9, :o2, 14813887, 6
          tz.transition 2048, 3, :o3, 59259919, 24
          tz.transition 2048, 9, :o2, 14816071, 6
          tz.transition 2049, 3, :o3, 59268655, 24
          tz.transition 2049, 9, :o2, 14818255, 6
          tz.transition 2050, 3, :o3, 59277391, 24
          tz.transition 2050, 9, :o2, 14820439, 6
        end
      end
    end
  end
end
