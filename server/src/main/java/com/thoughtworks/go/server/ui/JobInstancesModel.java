/*
 * Copyright 2020 ThoughtWorks, Inc.
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
package com.thoughtworks.go.server.ui;

import com.thoughtworks.go.domain.JobInstance;
import com.thoughtworks.go.domain.JobInstances;
import com.thoughtworks.go.server.util.Pagination;

import java.util.Iterator;

/**
 * @understands how to represent a paginated list of jobs on the UI
 */
public class JobInstancesModel implements Iterable<JobInstance> {

    private final JobInstances jobInstances;
    private final Pagination pagination;

    public JobInstancesModel(JobInstances jobInstances, Pagination pagination) {
        this.jobInstances = jobInstances;
        this.pagination = pagination;
    }

    @Override
    public Iterator<JobInstance> iterator() {
        return jobInstances.iterator();
    }

    public boolean isEmpty() {
        return jobInstances.isEmpty();
    }

	public JobInstances getJobInstances() {
		return jobInstances;
	}

	public Pagination getPagination() {
        return pagination;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JobInstancesModel that = (JobInstancesModel) o;

        if (jobInstances != null ? !jobInstances.equals(that.jobInstances) : that.jobInstances != null) {
            return false;
        }
        if (pagination != null ? !pagination.equals(that.pagination) : that.pagination != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = jobInstances != null ? jobInstances.hashCode() : 0;
        result = 31 * result + (pagination != null ? pagination.hashCode() : 0);
        return result;
    }
}
