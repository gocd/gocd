# encoding: UTF-8

module TZInfo
  module Definitions
    module Atlantic
      module Reykjavik
        include TimezoneDefinition
        
        timezone 'Atlantic/Reykjavik' do |tz|
          tz.offset :o0, -5280, 0, :LMT
          tz.offset :o1, -3600, 0, :IST
          tz.offset :o2, -3600, 3600, :ISST
          tz.offset :o3, 0, 0, :GMT
          
          tz.transition 1908, 1, :o1, 435229481, 180
          tz.transition 1917, 2, :o2, 4842559, 2
          tz.transition 1917, 10, :o1, 58116541, 24
          tz.transition 1918, 2, :o2, 4843289, 2
          tz.transition 1918, 11, :o1, 58125925, 24
          tz.transition 1919, 2, :o2, 4844019, 2
          tz.transition 1919, 11, :o1, 58134685, 24
          tz.transition 1921, 3, :o2, 4845537, 2
          tz.transition 1921, 6, :o1, 58148725, 24
          tz.transition 1939, 4, :o2, 4858767, 2
          tz.transition 1939, 10, :o1, 29154787, 12
          tz.transition 1940, 2, :o2, 19437477, 8
          tz.transition 1940, 11, :o1, 29159239, 12
          tz.transition 1941, 3, :o2, 29160667, 12
          tz.transition 1941, 11, :o1, 29163607, 12
          tz.transition 1942, 3, :o2, 29165119, 12
          tz.transition 1942, 10, :o1, 29167891, 12
          tz.transition 1943, 3, :o2, 29169487, 12
          tz.transition 1943, 10, :o1, 29172259, 12
          tz.transition 1944, 3, :o2, 29173855, 12
          tz.transition 1944, 10, :o1, 29176627, 12
          tz.transition 1945, 3, :o2, 29178223, 12
          tz.transition 1945, 10, :o1, 29181079, 12
          tz.transition 1946, 3, :o2, 29182591, 12
          tz.transition 1946, 10, :o1, 29185447, 12
          tz.transition 1947, 4, :o2, 29187379, 12
          tz.transition 1947, 10, :o1, 29189815, 12
          tz.transition 1948, 4, :o2, 29191747, 12
          tz.transition 1948, 10, :o1, 29194183, 12
          tz.transition 1949, 4, :o2, 29196115, 12
          tz.transition 1949, 10, :o1, 29198635, 12
          tz.transition 1950, 4, :o2, 29200483, 12
          tz.transition 1950, 10, :o1, 29202919, 12
          tz.transition 1951, 4, :o2, 29204851, 12
          tz.transition 1951, 10, :o1, 29207371, 12
          tz.transition 1952, 4, :o2, 29209303, 12
          tz.transition 1952, 10, :o1, 29211739, 12
          tz.transition 1953, 4, :o2, 29213671, 12
          tz.transition 1953, 10, :o1, 29216107, 12
          tz.transition 1954, 4, :o2, 29218039, 12
          tz.transition 1954, 10, :o1, 29220475, 12
          tz.transition 1955, 4, :o2, 29222407, 12
          tz.transition 1955, 10, :o1, 29224843, 12
          tz.transition 1956, 4, :o2, 29226775, 12
          tz.transition 1956, 10, :o1, 29229295, 12
          tz.transition 1957, 4, :o2, 29231227, 12
          tz.transition 1957, 10, :o1, 29233663, 12
          tz.transition 1958, 4, :o2, 29235595, 12
          tz.transition 1958, 10, :o1, 29238031, 12
          tz.transition 1959, 4, :o2, 29239963, 12
          tz.transition 1959, 10, :o1, 29242399, 12
          tz.transition 1960, 4, :o2, 29244331, 12
          tz.transition 1960, 10, :o1, 29246767, 12
          tz.transition 1961, 4, :o2, 29248699, 12
          tz.transition 1961, 10, :o1, 29251135, 12
          tz.transition 1962, 4, :o2, 29253067, 12
          tz.transition 1962, 10, :o1, 29255587, 12
          tz.transition 1963, 4, :o2, 29257519, 12
          tz.transition 1963, 10, :o1, 29259955, 12
          tz.transition 1964, 4, :o2, 29261887, 12
          tz.transition 1964, 10, :o1, 29264323, 12
          tz.transition 1965, 4, :o2, 29266255, 12
          tz.transition 1965, 10, :o1, 29268691, 12
          tz.transition 1966, 4, :o2, 29270623, 12
          tz.transition 1966, 10, :o1, 29273059, 12
          tz.transition 1967, 4, :o2, 29274991, 12
          tz.transition 1967, 10, :o1, 29277511, 12
          tz.transition 1968, 4, :o3, 29279443, 12
        end
      end
    end
  end
end
