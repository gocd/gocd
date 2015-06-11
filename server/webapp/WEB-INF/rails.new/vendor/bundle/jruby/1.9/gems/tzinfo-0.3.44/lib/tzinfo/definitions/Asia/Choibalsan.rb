module TZInfo
  module Definitions
    module Asia
      module Choibalsan
        include TimezoneDefinition
        
        timezone 'Asia/Choibalsan' do |tz|
          tz.offset :o0, 27480, 0, :LMT
          tz.offset :o1, 25200, 0, :ULAT
          tz.offset :o2, 28800, 0, :ULAT
          tz.offset :o3, 32400, 3600, :CHOST
          tz.offset :o4, 32400, 0, :CHOT
          tz.offset :o5, 28800, 0, :CHOT
          tz.offset :o6, 28800, 3600, :CHOST
          
          tz.transition 1905, 7, :o1, 1740281891, 720
          tz.transition 1977, 12, :o2, 252435600
          tz.transition 1983, 3, :o3, 417974400
          tz.transition 1983, 9, :o4, 433778400
          tz.transition 1984, 3, :o3, 449593200
          tz.transition 1984, 9, :o4, 465314400
          tz.transition 1985, 3, :o3, 481042800
          tz.transition 1985, 9, :o4, 496764000
          tz.transition 1986, 3, :o3, 512492400
          tz.transition 1986, 9, :o4, 528213600
          tz.transition 1987, 3, :o3, 543942000
          tz.transition 1987, 9, :o4, 559663200
          tz.transition 1988, 3, :o3, 575391600
          tz.transition 1988, 9, :o4, 591112800
          tz.transition 1989, 3, :o3, 606841200
          tz.transition 1989, 9, :o4, 622562400
          tz.transition 1990, 3, :o3, 638290800
          tz.transition 1990, 9, :o4, 654616800
          tz.transition 1991, 3, :o3, 670345200
          tz.transition 1991, 9, :o4, 686066400
          tz.transition 1992, 3, :o3, 701794800
          tz.transition 1992, 9, :o4, 717516000
          tz.transition 1993, 3, :o3, 733244400
          tz.transition 1993, 9, :o4, 748965600
          tz.transition 1994, 3, :o3, 764694000
          tz.transition 1994, 9, :o4, 780415200
          tz.transition 1995, 3, :o3, 796143600
          tz.transition 1995, 9, :o4, 811864800
          tz.transition 1996, 3, :o3, 828198000
          tz.transition 1996, 9, :o4, 843919200
          tz.transition 1997, 3, :o3, 859647600
          tz.transition 1997, 9, :o4, 875368800
          tz.transition 1998, 3, :o3, 891097200
          tz.transition 1998, 9, :o4, 906818400
          tz.transition 2001, 4, :o3, 988390800
          tz.transition 2001, 9, :o4, 1001692800
          tz.transition 2002, 3, :o3, 1017421200
          tz.transition 2002, 9, :o4, 1033142400
          tz.transition 2003, 3, :o3, 1048870800
          tz.transition 2003, 9, :o4, 1064592000
          tz.transition 2004, 3, :o3, 1080320400
          tz.transition 2004, 9, :o4, 1096041600
          tz.transition 2005, 3, :o3, 1111770000
          tz.transition 2005, 9, :o4, 1127491200
          tz.transition 2006, 3, :o3, 1143219600
          tz.transition 2006, 9, :o4, 1159545600
          tz.transition 2008, 3, :o5, 1206889200
          tz.transition 2015, 3, :o6, 1427479200
          tz.transition 2015, 9, :o5, 1443193200
          tz.transition 2016, 3, :o6, 1458928800
          tz.transition 2016, 9, :o5, 1474642800
          tz.transition 2017, 3, :o6, 1490378400
          tz.transition 2017, 9, :o5, 1506697200
          tz.transition 2018, 3, :o6, 1522432800
          tz.transition 2018, 9, :o5, 1538146800
          tz.transition 2019, 3, :o6, 1553882400
          tz.transition 2019, 9, :o5, 1569596400
          tz.transition 2020, 3, :o6, 1585332000
          tz.transition 2020, 9, :o5, 1601046000
          tz.transition 2021, 3, :o6, 1616781600
          tz.transition 2021, 9, :o5, 1632495600
          tz.transition 2022, 3, :o6, 1648231200
          tz.transition 2022, 9, :o5, 1663945200
          tz.transition 2023, 3, :o6, 1679680800
          tz.transition 2023, 9, :o5, 1695999600
          tz.transition 2024, 3, :o6, 1711735200
          tz.transition 2024, 9, :o5, 1727449200
          tz.transition 2025, 3, :o6, 1743184800
          tz.transition 2025, 9, :o5, 1758898800
          tz.transition 2026, 3, :o6, 1774634400
          tz.transition 2026, 9, :o5, 1790348400
          tz.transition 2027, 3, :o6, 1806084000
          tz.transition 2027, 9, :o5, 1821798000
          tz.transition 2028, 3, :o6, 1837533600
          tz.transition 2028, 9, :o5, 1853852400
          tz.transition 2029, 3, :o6, 1869588000
          tz.transition 2029, 9, :o5, 1885302000
          tz.transition 2030, 3, :o6, 1901037600
          tz.transition 2030, 9, :o5, 1916751600
          tz.transition 2031, 3, :o6, 1932487200
          tz.transition 2031, 9, :o5, 1948201200
          tz.transition 2032, 3, :o6, 1963936800
          tz.transition 2032, 9, :o5, 1979650800
          tz.transition 2033, 3, :o6, 1995386400
          tz.transition 2033, 9, :o5, 2011100400
          tz.transition 2034, 3, :o6, 2026836000
          tz.transition 2034, 9, :o5, 2043154800
          tz.transition 2035, 3, :o6, 2058890400
          tz.transition 2035, 9, :o5, 2074604400
          tz.transition 2036, 3, :o6, 2090340000
          tz.transition 2036, 9, :o5, 2106054000
          tz.transition 2037, 3, :o6, 2121789600
          tz.transition 2037, 9, :o5, 2137503600
          tz.transition 2038, 3, :o6, 9862037, 4
          tz.transition 2038, 9, :o5, 19725529, 8
          tz.transition 2039, 3, :o6, 9863493, 4
          tz.transition 2039, 9, :o5, 19728441, 8
          tz.transition 2040, 3, :o6, 9864977, 4
          tz.transition 2040, 9, :o5, 19731409, 8
          tz.transition 2041, 3, :o6, 9866433, 4
          tz.transition 2041, 9, :o5, 19734321, 8
          tz.transition 2042, 3, :o6, 9867889, 4
          tz.transition 2042, 9, :o5, 19737233, 8
          tz.transition 2043, 3, :o6, 9869345, 4
          tz.transition 2043, 9, :o5, 19740145, 8
          tz.transition 2044, 3, :o6, 9870801, 4
          tz.transition 2044, 9, :o5, 19743057, 8
          tz.transition 2045, 3, :o6, 9872257, 4
          tz.transition 2045, 9, :o5, 19746025, 8
          tz.transition 2046, 3, :o6, 9873741, 4
          tz.transition 2046, 9, :o5, 19748937, 8
          tz.transition 2047, 3, :o6, 9875197, 4
          tz.transition 2047, 9, :o5, 19751849, 8
          tz.transition 2048, 3, :o6, 9876653, 4
          tz.transition 2048, 9, :o5, 19754761, 8
          tz.transition 2049, 3, :o6, 9878109, 4
          tz.transition 2049, 9, :o5, 19757673, 8
          tz.transition 2050, 3, :o6, 9879565, 4
          tz.transition 2050, 9, :o5, 19760585, 8
        end
      end
    end
  end
end
