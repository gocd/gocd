# encoding: UTF-8

module TZInfo
  module Definitions
    module Africa
      module Accra
        include TimezoneDefinition
        
        timezone 'Africa/Accra' do |tz|
          tz.offset :o0, -52, 0, :LMT
          tz.offset :o1, 0, 0, :GMT
          tz.offset :o2, 0, 1200, :GHST
          
          tz.transition 1918, 1, :o1, 52306441213, 21600
          tz.transition 1920, 9, :o2, 4845137, 2
          tz.transition 1920, 12, :o1, 174433643, 72
          tz.transition 1921, 9, :o2, 4845867, 2
          tz.transition 1921, 12, :o1, 174459923, 72
          tz.transition 1922, 9, :o2, 4846597, 2
          tz.transition 1922, 12, :o1, 174486203, 72
          tz.transition 1923, 9, :o2, 4847327, 2
          tz.transition 1923, 12, :o1, 174512483, 72
          tz.transition 1924, 9, :o2, 4848059, 2
          tz.transition 1924, 12, :o1, 174538835, 72
          tz.transition 1925, 9, :o2, 4848789, 2
          tz.transition 1925, 12, :o1, 174565115, 72
          tz.transition 1926, 9, :o2, 4849519, 2
          tz.transition 1926, 12, :o1, 174591395, 72
          tz.transition 1927, 9, :o2, 4850249, 2
          tz.transition 1927, 12, :o1, 174617675, 72
          tz.transition 1928, 9, :o2, 4850981, 2
          tz.transition 1928, 12, :o1, 174644027, 72
          tz.transition 1929, 9, :o2, 4851711, 2
          tz.transition 1929, 12, :o1, 174670307, 72
          tz.transition 1930, 9, :o2, 4852441, 2
          tz.transition 1930, 12, :o1, 174696587, 72
          tz.transition 1931, 9, :o2, 4853171, 2
          tz.transition 1931, 12, :o1, 174722867, 72
          tz.transition 1932, 9, :o2, 4853903, 2
          tz.transition 1932, 12, :o1, 174749219, 72
          tz.transition 1933, 9, :o2, 4854633, 2
          tz.transition 1933, 12, :o1, 174775499, 72
          tz.transition 1934, 9, :o2, 4855363, 2
          tz.transition 1934, 12, :o1, 174801779, 72
          tz.transition 1935, 9, :o2, 4856093, 2
          tz.transition 1935, 12, :o1, 174828059, 72
          tz.transition 1936, 9, :o2, 4856825, 2
          tz.transition 1936, 12, :o1, 174854411, 72
          tz.transition 1937, 9, :o2, 4857555, 2
          tz.transition 1937, 12, :o1, 174880691, 72
          tz.transition 1938, 9, :o2, 4858285, 2
          tz.transition 1938, 12, :o1, 174906971, 72
          tz.transition 1939, 9, :o2, 4859015, 2
          tz.transition 1939, 12, :o1, 174933251, 72
          tz.transition 1940, 9, :o2, 4859747, 2
          tz.transition 1940, 12, :o1, 174959603, 72
          tz.transition 1941, 9, :o2, 4860477, 2
          tz.transition 1941, 12, :o1, 174985883, 72
          tz.transition 1942, 9, :o2, 4861207, 2
          tz.transition 1942, 12, :o1, 175012163, 72
        end
      end
    end
  end
end
