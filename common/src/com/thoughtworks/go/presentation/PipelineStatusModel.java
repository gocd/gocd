package com.thoughtworks.go.presentation;

public class PipelineStatusModel {
	private boolean paused;
	private boolean locked;
	private boolean schedulable;

	public PipelineStatusModel(boolean paused, boolean locked, boolean schedulable) {
		this.paused = paused;
		this.locked = locked;
		this.schedulable = schedulable;
	}

	public boolean isPaused() {
		return paused;
	}

	public boolean isLocked() {
		return locked;
	}

	public boolean isSchedulable() {
		return schedulable;
	}
}
