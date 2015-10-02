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
package com.thoughtworks.go.presentation;
import com.thoughtworks.go.domain.PipelinePauseInfo;

public class PipelineStatusModel {
	private boolean paused;
	private String pausedCause;
	private String pausedBy;
	private boolean locked;
	private boolean schedulable;

	public PipelineStatusModel(boolean locked, boolean schedulable, PipelinePauseInfo pipelinePauseInfo) {
		this.paused = pipelinePauseInfo.isPaused();
		this.pausedCause = pipelinePauseInfo.getPauseCause();
		this.pausedBy = pipelinePauseInfo.getPauseBy();
		this.locked = locked;
		this.schedulable = schedulable;
	}

	public boolean isPaused() { return paused; }

    public String pausedCause() { return pausedCause; }

    public String pausedBy() { return pausedBy; }

	public boolean isLocked() {
		return locked;
	}

	public boolean isSchedulable() {
		return schedulable;
	}
}
