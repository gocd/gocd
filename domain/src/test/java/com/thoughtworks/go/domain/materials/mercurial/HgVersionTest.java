/*
 * Copyright 2021 ThoughtWorks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.thoughtworks.go.domain.materials.mercurial;

import de.skuzzle.semantic.Version;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

public class HgVersionTest {

    private static final String LINUX_HG_094 = "Mercurial Distributed SCM (version 0.9.4)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String LINUX_HG_101 = "Mercurial Distributed SCM (version 1.0.1)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String LINUX_HG_10 = "Mercurial Distributed SCM (version 1.0)\n"
            + "\n"
            + "Copyright (C) 2005-2007 Matt Mackall <mpm@selenic.com> and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String WINDOWS_HG_OFFICAL_102 = "Mercurial Distributed SCM (version 1.0.2+20080813)\n"
            + "\n"
            + "Copyright (C) 2005-2008 Matt Mackall <mpm@selenic.com>; and others\n"
            + "This is free software; see the source for copying conditions. There is NO\n"
            + "warranty; not even for MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.";
    private static final String WINDOWS_HG_TORTOISE = "Mercurial Distributed SCM (version 626cb86a6523+tortoisehg)";
    private static final String HG_TWO_DIGIT_VERSION = "Mercurial Distributed SCM (version 10.19.40)";
    private static final String HG_MULTIPLE_VERSIONS = "Mercurial Distributed SCM (version 2.6.2), 2.7-rc+5-ca2dfc2f63eb";

    @Test
    void shouldParseHgVersions() {
        assertThat(HgVersion.parse(LINUX_HG_094).getVersion()).isEqualTo(Version.create(0,9,4));
        assertThat(HgVersion.parse(LINUX_HG_101).getVersion()).isEqualTo(Version.create(1,0,1));
        assertThat(HgVersion.parse(LINUX_HG_10).getVersion()).isEqualTo(Version.create(1,0,0));
        assertThat(HgVersion.parse(WINDOWS_HG_OFFICAL_102).getVersion()).isEqualTo(Version.create(1,0,2));
        assertThat(HgVersion.parse(HG_TWO_DIGIT_VERSION).getVersion()).isEqualTo(Version.create(10,19,40));
        assertThat(HgVersion.parse(HG_MULTIPLE_VERSIONS).getVersion()).isEqualTo(Version.create(2,6,2));
    }

    @Test
    void shouldReturnTrueWhenHgVersionIsOlderThanOneDotZero() {
        assertThat(HgVersion.parse(LINUX_HG_094).isOlderThanOneDotZero()).isTrue();
    }

    @Test
    void shouldReturnFalseWhenHgVersionIsOneDotZeroOrNewer() {
        assertThat(HgVersion.parse(LINUX_HG_101).isOlderThanOneDotZero()).isFalse();
        assertThat(HgVersion.parse(LINUX_HG_10).isOlderThanOneDotZero()).isFalse();
        assertThat(HgVersion.parse(WINDOWS_HG_OFFICAL_102).isOlderThanOneDotZero()).isFalse();
    }

    @Test
    void shouldBombIfVersionCannotBeParsed() {
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(() -> HgVersion.parse(WINDOWS_HG_TORTOISE))
                .withMessage("cannot parse Hg version : " + WINDOWS_HG_TORTOISE);
    }
}
