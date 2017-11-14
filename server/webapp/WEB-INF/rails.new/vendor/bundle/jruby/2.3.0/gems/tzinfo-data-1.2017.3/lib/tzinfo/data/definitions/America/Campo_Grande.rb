# encoding: UTF-8

# This file contains data derived from the IANA Time Zone Database
# (http://www.iana.org/time-zones).

module TZInfo
  module Data
    module Definitions
      module America
        module Campo_Grande
          include TimezoneDefinition
          
          timezone 'America/Campo_Grande' do |tz|
            tz.offset :o0, -13108, 0, :LMT
            tz.offset :o1, -14400, 0, :'-04'
            tz.offset :o2, -14400, 3600, :'-03'
            
            tz.transition 1914, 1, :o1, -1767212492, 52274886877, 21600
            tz.transition 1931, 10, :o2, -1206954000, 19412945, 8
            tz.transition 1932, 4, :o1, -1191358800, 19414389, 8
            tz.transition 1932, 10, :o2, -1175371200, 7280951, 3
            tz.transition 1933, 4, :o1, -1159822800, 19417309, 8
            tz.transition 1949, 12, :o2, -633816000, 7299755, 3
            tz.transition 1950, 4, :o1, -622065600, 7300163, 3
            tz.transition 1950, 12, :o2, -602280000, 7300850, 3
            tz.transition 1951, 4, :o1, -591829200, 19469901, 8
            tz.transition 1951, 12, :o2, -570744000, 7301945, 3
            tz.transition 1952, 4, :o1, -560206800, 19472829, 8
            tz.transition 1952, 12, :o2, -539121600, 7303043, 3
            tz.transition 1953, 3, :o1, -531349200, 19475501, 8
            tz.transition 1963, 12, :o2, -191361600, 7315118, 3
            tz.transition 1964, 3, :o1, -184194000, 19507645, 8
            tz.transition 1965, 1, :o2, -155160000, 7316375, 3
            tz.transition 1965, 3, :o1, -150066000, 19510805, 8
            tz.transition 1965, 12, :o2, -128894400, 7317287, 3
            tz.transition 1966, 3, :o1, -121122000, 19513485, 8
            tz.transition 1966, 11, :o2, -99950400, 7318292, 3
            tz.transition 1967, 3, :o1, -89586000, 19516405, 8
            tz.transition 1967, 11, :o2, -68414400, 7319387, 3
            tz.transition 1968, 3, :o1, -57963600, 19519333, 8
            tz.transition 1985, 11, :o2, 499752000
            tz.transition 1986, 3, :o1, 511239600
            tz.transition 1986, 10, :o2, 530596800
            tz.transition 1987, 2, :o1, 540270000
            tz.transition 1987, 10, :o2, 562132800
            tz.transition 1988, 2, :o1, 571201200
            tz.transition 1988, 10, :o2, 592977600
            tz.transition 1989, 1, :o1, 602046000
            tz.transition 1989, 10, :o2, 624427200
            tz.transition 1990, 2, :o1, 634705200
            tz.transition 1990, 10, :o2, 656481600
            tz.transition 1991, 2, :o1, 666759600
            tz.transition 1991, 10, :o2, 687931200
            tz.transition 1992, 2, :o1, 697604400
            tz.transition 1992, 10, :o2, 719985600
            tz.transition 1993, 1, :o1, 728449200
            tz.transition 1993, 10, :o2, 750830400
            tz.transition 1994, 2, :o1, 761713200
            tz.transition 1994, 10, :o2, 782280000
            tz.transition 1995, 2, :o1, 793162800
            tz.transition 1995, 10, :o2, 813729600
            tz.transition 1996, 2, :o1, 824007600
            tz.transition 1996, 10, :o2, 844574400
            tz.transition 1997, 2, :o1, 856062000
            tz.transition 1997, 10, :o2, 876110400
            tz.transition 1998, 3, :o1, 888721200
            tz.transition 1998, 10, :o2, 908078400
            tz.transition 1999, 2, :o1, 919566000
            tz.transition 1999, 10, :o2, 938923200
            tz.transition 2000, 2, :o1, 951620400
            tz.transition 2000, 10, :o2, 970977600
            tz.transition 2001, 2, :o1, 982465200
            tz.transition 2001, 10, :o2, 1003032000
            tz.transition 2002, 2, :o1, 1013914800
            tz.transition 2002, 11, :o2, 1036296000
            tz.transition 2003, 2, :o1, 1045364400
            tz.transition 2003, 10, :o2, 1066536000
            tz.transition 2004, 2, :o1, 1076814000
            tz.transition 2004, 11, :o2, 1099368000
            tz.transition 2005, 2, :o1, 1108868400
            tz.transition 2005, 10, :o2, 1129435200
            tz.transition 2006, 2, :o1, 1140318000
            tz.transition 2006, 11, :o2, 1162699200
            tz.transition 2007, 2, :o1, 1172372400
            tz.transition 2007, 10, :o2, 1192334400
            tz.transition 2008, 2, :o1, 1203217200
            tz.transition 2008, 10, :o2, 1224388800
            tz.transition 2009, 2, :o1, 1234666800
            tz.transition 2009, 10, :o2, 1255838400
            tz.transition 2010, 2, :o1, 1266721200
            tz.transition 2010, 10, :o2, 1287288000
            tz.transition 2011, 2, :o1, 1298170800
            tz.transition 2011, 10, :o2, 1318737600
            tz.transition 2012, 2, :o1, 1330225200
            tz.transition 2012, 10, :o2, 1350792000
            tz.transition 2013, 2, :o1, 1361070000
            tz.transition 2013, 10, :o2, 1382241600
            tz.transition 2014, 2, :o1, 1392519600
            tz.transition 2014, 10, :o2, 1413691200
            tz.transition 2015, 2, :o1, 1424574000
            tz.transition 2015, 10, :o2, 1445140800
            tz.transition 2016, 2, :o1, 1456023600
            tz.transition 2016, 10, :o2, 1476590400
            tz.transition 2017, 2, :o1, 1487473200
            tz.transition 2017, 10, :o2, 1508040000
            tz.transition 2018, 2, :o1, 1518922800
            tz.transition 2018, 10, :o2, 1540094400
            tz.transition 2019, 2, :o1, 1550372400
            tz.transition 2019, 10, :o2, 1571544000
            tz.transition 2020, 2, :o1, 1581822000
            tz.transition 2020, 10, :o2, 1602993600
            tz.transition 2021, 2, :o1, 1613876400
            tz.transition 2021, 10, :o2, 1634443200
            tz.transition 2022, 2, :o1, 1645326000
            tz.transition 2022, 10, :o2, 1665892800
            tz.transition 2023, 2, :o1, 1677380400
            tz.transition 2023, 10, :o2, 1697342400
            tz.transition 2024, 2, :o1, 1708225200
            tz.transition 2024, 10, :o2, 1729396800
            tz.transition 2025, 2, :o1, 1739674800
            tz.transition 2025, 10, :o2, 1760846400
            tz.transition 2026, 2, :o1, 1771729200
            tz.transition 2026, 10, :o2, 1792296000
            tz.transition 2027, 2, :o1, 1803178800
            tz.transition 2027, 10, :o2, 1823745600
            tz.transition 2028, 2, :o1, 1834628400
            tz.transition 2028, 10, :o2, 1855195200
            tz.transition 2029, 2, :o1, 1866078000
            tz.transition 2029, 10, :o2, 1887249600
            tz.transition 2030, 2, :o1, 1897527600
            tz.transition 2030, 10, :o2, 1918699200
            tz.transition 2031, 2, :o1, 1928977200
            tz.transition 2031, 10, :o2, 1950148800
            tz.transition 2032, 2, :o1, 1960426800
            tz.transition 2032, 10, :o2, 1981598400
            tz.transition 2033, 2, :o1, 1992481200
            tz.transition 2033, 10, :o2, 2013048000
            tz.transition 2034, 2, :o1, 2024535600
            tz.transition 2034, 10, :o2, 2044497600
            tz.transition 2035, 2, :o1, 2055380400
            tz.transition 2035, 10, :o2, 2076552000
            tz.transition 2036, 2, :o1, 2086830000
            tz.transition 2036, 10, :o2, 2108001600
            tz.transition 2037, 2, :o1, 2118884400
            tz.transition 2037, 10, :o2, 2139451200
            tz.transition 2038, 2, :o1, 2150334000, 19723805, 8
            tz.transition 2038, 10, :o2, 2170900800, 7397141, 3
            tz.transition 2039, 2, :o1, 2181783600, 19726717, 8
            tz.transition 2039, 10, :o2, 2202350400, 7398233, 3
            tz.transition 2040, 2, :o1, 2213233200, 19729629, 8
            tz.transition 2040, 10, :o2, 2234404800, 7399346, 3
            tz.transition 2041, 2, :o1, 2244682800, 19732541, 8
            tz.transition 2041, 10, :o2, 2265854400, 7400438, 3
            tz.transition 2042, 2, :o1, 2276132400, 19735453, 8
            tz.transition 2042, 10, :o2, 2297304000, 7401530, 3
            tz.transition 2043, 2, :o1, 2307582000, 19738365, 8
            tz.transition 2043, 10, :o2, 2328753600, 7402622, 3
            tz.transition 2044, 2, :o1, 2339636400, 19741333, 8
            tz.transition 2044, 10, :o2, 2360203200, 7403714, 3
            tz.transition 2045, 2, :o1, 2371086000, 19744245, 8
            tz.transition 2045, 10, :o2, 2391652800, 7404806, 3
            tz.transition 2046, 2, :o1, 2402535600, 19747157, 8
            tz.transition 2046, 10, :o2, 2423707200, 7405919, 3
            tz.transition 2047, 2, :o1, 2433985200, 19750069, 8
            tz.transition 2047, 10, :o2, 2455156800, 7407011, 3
            tz.transition 2048, 2, :o1, 2465434800, 19752981, 8
            tz.transition 2048, 10, :o2, 2486606400, 7408103, 3
            tz.transition 2049, 2, :o1, 2497489200, 19755949, 8
            tz.transition 2049, 10, :o2, 2518056000, 7409195, 3
            tz.transition 2050, 2, :o1, 2528938800, 19758861, 8
            tz.transition 2050, 10, :o2, 2549505600, 7410287, 3
            tz.transition 2051, 2, :o1, 2560388400, 19761773, 8
            tz.transition 2051, 10, :o2, 2580955200, 7411379, 3
            tz.transition 2052, 2, :o1, 2591838000, 19764685, 8
            tz.transition 2052, 10, :o2, 2613009600, 7412492, 3
            tz.transition 2053, 2, :o1, 2623287600, 19767597, 8
            tz.transition 2053, 10, :o2, 2644459200, 7413584, 3
            tz.transition 2054, 2, :o1, 2654737200, 19770509, 8
            tz.transition 2054, 10, :o2, 2675908800, 7414676, 3
            tz.transition 2055, 2, :o1, 2686791600, 19773477, 8
            tz.transition 2055, 10, :o2, 2707358400, 7415768, 3
            tz.transition 2056, 2, :o1, 2718241200, 19776389, 8
            tz.transition 2056, 10, :o2, 2738808000, 7416860, 3
            tz.transition 2057, 2, :o1, 2749690800, 19779301, 8
            tz.transition 2057, 10, :o2, 2770862400, 7417973, 3
            tz.transition 2058, 2, :o1, 2781140400, 19782213, 8
            tz.transition 2058, 10, :o2, 2802312000, 7419065, 3
            tz.transition 2059, 2, :o1, 2812590000, 19785125, 8
            tz.transition 2059, 10, :o2, 2833761600, 7420157, 3
            tz.transition 2060, 2, :o1, 2844039600, 19788037, 8
            tz.transition 2060, 10, :o2, 2865211200, 7421249, 3
            tz.transition 2061, 2, :o1, 2876094000, 19791005, 8
            tz.transition 2061, 10, :o2, 2896660800, 7422341, 3
            tz.transition 2062, 2, :o1, 2907543600, 19793917, 8
            tz.transition 2062, 10, :o2, 2928110400, 7423433, 3
            tz.transition 2063, 2, :o1, 2938993200, 19796829, 8
            tz.transition 2063, 10, :o2, 2960164800, 7424546, 3
            tz.transition 2064, 2, :o1, 2970442800, 19799741, 8
            tz.transition 2064, 10, :o2, 2991614400, 7425638, 3
            tz.transition 2065, 2, :o1, 3001892400, 19802653, 8
            tz.transition 2065, 10, :o2, 3023064000, 7426730, 3
            tz.transition 2066, 2, :o1, 3033946800, 19805621, 8
            tz.transition 2066, 10, :o2, 3054513600, 7427822, 3
            tz.transition 2067, 2, :o1, 3065396400, 19808533, 8
          end
        end
      end
    end
  end
end
