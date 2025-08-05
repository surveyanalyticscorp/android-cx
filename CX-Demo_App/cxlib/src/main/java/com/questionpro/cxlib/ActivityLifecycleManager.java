/*
 * Copyright (c) 2013, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.questionpro.cxlib;

import android.app.Activity;
import android.content.Context;

import com.questionpro.cxlib.interfaces.PersistentSessionQueue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ActivityLifecycleManager {


	private static final int SESSION_TIMEOUT_SECONDS = 10;

	private static PersistentSessionQueue queue = null;
	private static void init(Context context) {
		if (queue == null) {
			queue = new SharedPreferencesPersistentSessionQueue(context.getApplicationContext());
		}
	}


	public static void activityStarted(Activity activity) {
		try {
			init(activity);
			SessionEvent start = new SessionEvent(new Date().getTime(), SessionEvent.Action.START, activity.toString());

			// Get last stop
			SessionEvent lastStop = getLastEvent(SessionEvent.Action.STOP);
			SessionEvent lastStart = getLastEvent(SessionEvent.Action.START);

			// Remove extra pairs
			removePairs(1);

			// Count Starts, pairs.
			int starts = countStarts();
			int pairs = countPairsInQueue();

			// Due to the nature of the Activity Lifecycle, onStart() of the next Activity may be called before or after
			// the onStart() of the current previous Activity. This complicated the detection. This is the distilled logic.
			if (pairs == 0 && starts == 0) {
				//Log.v("First start.");
				addEvents(start);
				//sendEvent(activity, start);
			} else if (pairs == 0 && starts == 1) {
				//Log.v("Continuation Start. (1)");
				addEvents(start);
			} else if (pairs == 0 && starts == 2) {
				//Log.i("Starting new session after crash. (1)");
				removeAllEvents();
				//sendEvent(activity, lastStart != null ? lastStart : start, true);
				addEvents(lastStart, start);
			} else if (pairs == 1 && starts == 1) {
				long expiration = lastStop.getTime() + (SESSION_TIMEOUT_SECONDS * 1000);
				boolean expired = expiration < start.getTime();
				addEvents(start);
				if (expired) {
				//	Log.d("Session expired. Starting new session.");
				//	sendEvent(activity, lastStop);
				//	sendEvent(activity, start);
				} else {
				//	Log.v("Continuation Start. (2)");
				}
			} else if (pairs == 1 && starts == 2) {
				//Log.v("Continuation start. (3)");
				addEvents(start);
			} else if (pairs == 1 && starts == 3) {
				//Log.i("Starting new session after crash. (2)");
				//sendEvent(activity, lastStart != null ? lastStart : start, true);
				// Reconstruct Queue.
				removeAllEvents();
				addEvents(lastStart, start);
			} else {
				//Log.w("ERROR: Unexpected state in LifecycleManager: " + getQueueAsString());
			}
		} catch (Exception e) {
			//Log.e("Error while handling activity start.", e);
		}
	}
	public static void activityStopped(Activity activity) {
		try {

			addEvents(new SessionEvent(new Date().getTime(), SessionEvent.Action.STOP, activity.toString()));
		} catch (Exception e) {
			//Log.e("Error while handling activity stop.", e);
		}
	}




	private static void addEvents(SessionEvent... events) {
		queue.addEvents(events);
	}

	private static int countStarts() {
		int starts = 0;
		List<SessionEvent> events = getAllEvents();
		for (SessionEvent event : events) {
			if (event.isStartEvent()) {
				starts++;
			}
		}
		return starts;
	}

	private static void removeEvent(SessionEvent... events) {
		queue.deleteEvents(events);
	}

	private static void removeAllEvents() {
		queue.deleteAllEvents();
	}

	private static void removePairs(int pairsToLeave) {
		List<SessionEvent> events = getAllEvents();
		List<SessionEvent> starts = new ArrayList<SessionEvent>();
		List<SessionEvent> eventsToDelete = new ArrayList<SessionEvent>();

		int pairsToDelete = Math.max(countPairsInQueue() - pairsToLeave, 0);
		if (pairsToDelete == 0) {
			return;
		}

		// Get all the start events.
		for (SessionEvent event : events) {
			if (event.isStartEvent()) {
				starts.add(event);
			}
		}
		// Mark pairs for deletion.
		outerLoop:
		for (SessionEvent start : starts) {
			for (SessionEvent event : events) {
				if (event.isStopEvent() && start.getActivityName().equals(event.getActivityName())) {
					pairsToDelete--;
					events.remove(start);
					events.remove(event);
					eventsToDelete.add(start);
					eventsToDelete.add(event);
					if (pairsToDelete == 0) {
						break outerLoop;
					}
					break;
				}
			}
		}
		// Do the actual deletion.
		removeEvent(eventsToDelete.toArray(new SessionEvent[eventsToDelete.size()]));
	}

	private static int countPairsInQueue() {
		List<SessionEvent> events = getAllEvents();
		List<SessionEvent> starts = new ArrayList<SessionEvent>();
		int pairs = 0;

		// Get all the start events.
		for (SessionEvent event : events) {
			if (event.isStartEvent()) {
				starts.add(event);
			}
		}
		// Then see if they have corresponding stop events.
		for (SessionEvent start : starts) {
			for (SessionEvent event : events) {
				if (event.isStopEvent() && start.getActivityName().equals(event.getActivityName())) {
					pairs++;
					events.remove(start);
					events.remove(event);
					break;
				}
			}
		}
		return pairs;
	}

	private static List<SessionEvent> getAllEvents() {
		return queue.getAllEvents();
	}

	private static SessionEvent getLastEvent(SessionEvent.Action action) {
		List<SessionEvent> events = getAllEvents();
		SessionEvent ret = null;
		for (SessionEvent event : events) {
			if (event.getAction() == action) {
				ret = event;
			}
		}
		return ret;
	}
}
