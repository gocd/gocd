/*************************GO-LICENSE-START*********************************
 * Copyright 2014 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *************************GO-LICENSE-END**********************************/

describe("time_converter", function () {
    it("testShouldConvertTimeCorrectly", function () {
        assertEquals('00:00:00', cruiseTimeConverter.fromSecondsToHHMMSS(0));
        assertEquals('00:00:01', cruiseTimeConverter.fromSecondsToHHMMSS(1));
        assertEquals('00:00:09', cruiseTimeConverter.fromSecondsToHHMMSS(9));
        assertEquals('00:00:10', cruiseTimeConverter.fromSecondsToHHMMSS(10));
        assertEquals('00:00:59', cruiseTimeConverter.fromSecondsToHHMMSS(59));
        assertEquals('00:01:00', cruiseTimeConverter.fromSecondsToHHMMSS(60));
        assertEquals('00:01:01', cruiseTimeConverter.fromSecondsToHHMMSS(61));
        assertEquals('00:02:00', cruiseTimeConverter.fromSecondsToHHMMSS(120));
        assertEquals('00:59:59', cruiseTimeConverter.fromSecondsToHHMMSS(3599));
        assertEquals('01:00:00', cruiseTimeConverter.fromSecondsToHHMMSS(3600));
        assertEquals('10:00:00', cruiseTimeConverter.fromSecondsToHHMMSS(36000));
        assertEquals('11:00:00', cruiseTimeConverter.fromSecondsToHHMMSS(39600));
        assertEquals('100:00:00', cruiseTimeConverter.fromSecondsToHHMMSS(360000));
    });
});

