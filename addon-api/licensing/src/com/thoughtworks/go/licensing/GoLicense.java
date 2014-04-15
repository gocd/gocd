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
 *************************GO-LICENSE-END***********************************/

package com.thoughtworks.go.licensing;

import java.sql.Date;
import java.util.Map;

import com.thoughtworks.go.util.Clock;
import com.thoughtworks.go.util.GoConstants;
import com.thoughtworks.go.util.SystemTimeClock;
import org.joda.time.DateTime;

import static com.thoughtworks.go.util.ExceptionUtils.bombIfNull;

public class GoLicense {
    private final Date expirationDate;
    private final int maxAgents;
    private Edition edition = Edition.Free;
    private int maxUsers;

    public static final Date DEFAULT_EXPIRY_DATE = new Date(SystemTimeClock.ETERNITY.getTime());
    public static final GoLicense OSS_LICENSE = createLicense(DEFAULT_EXPIRY_DATE, Integer.MAX_VALUE, Edition.OpenSource, Integer.MAX_VALUE);
    private final Clock clock;


    public static GoLicense createLicense(Date expiryDate, int maxAgents) {
        return createLicense(expiryDate, maxAgents, Edition.Free, 0);
    }

    public static GoLicense createLicense(Date expiryDate, int maxAgents, Edition edition, int maxUsers, Clock clock) {
        return new GoLicense(expiryDate, maxAgents, edition, maxUsers, clock);
    }

    public static GoLicense createLicense(Date expiryDate, int maxAgents, Edition edition, int maxUsers) {
        return createLicense(expiryDate, maxAgents, edition, maxUsers, new SystemTimeClock());
    }

    private GoLicense(Date expiryDate, int maxAgents, Edition edition, int maxUsers, Clock clock) {
        this.clock = clock;
        bombIfNull(expiryDate, "Must have expiration date on GoLicense");
        this.expirationDate = expiryDate;
        this.maxAgents = maxAgents;
        this.maxUsers = edition == Edition.Free ? 10: maxUsers;
        this.edition = edition;
    }

    public int numberOfAgents() {
        return maxAgents;
    }

    public String toString() {
        return String.format("GoLicense[expirationDate=%s(%s), maxAgents=%s, edition=%s, maxUsers=%s]", expirationDate,
                expirationDate.getTime(), maxAgents, edition, maxUsers);
    }

    public int numberOfUsers() {
        return maxUsers;
    }

    public void fill(Map<String, Object> model) {
        model.put(GoConstants.EXPIRY_DATE, expirationDate);
        model.put(GoConstants.MAX_AGENTS, maxAgents);
        model.put(GoConstants.MAX_USERS, maxUsers);
        model.put(GoConstants.EDITION, edition);
    }

    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        GoLicense that = (GoLicense) o;

        if (maxAgents != that.maxAgents) {
            return false;
        }
        if (maxUsers != that.maxUsers) {
            return false;
        }
        if (edition != that.edition) {
            return false;
        }
        if (expirationDate != null ? !expirationDate.equals(that.expirationDate) : that.expirationDate != null) {
            return false;
        }

        return true;
    }

    public int hashCode() {
        int result;
        result = (expirationDate != null ? expirationDate.hashCode() : 0);
        result = 31 * result + maxAgents;
        result = 31 * result + (edition != null ? edition.hashCode() : 0);
        return result;
    }

    public Edition edition() {
        return edition;
    }

    public LicenseValidity validity() {
        return LicenseValidity.VALID_LICENSE;
    }

    public boolean isExpired() {
        Date oneDayAfterExpireDate = new Date(new DateTime(expirationDate).plusDays(1).toDate().getTime());
        return clock.currentTime().getTime() >= oneDayAfterExpireDate.getTime();
    }

    public java.util.Date getExpirationDate() {
        return expirationDate;
    }
}
