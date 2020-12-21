/*
 * Copyright (c) 2012, Apptentive, Inc. All Rights Reserved.
 * Please refer to the LICENSE file for the terms and conditions
 * under which redistribution and use of this file is permitted.
 */

package com.questionpro.cxlib;

import java.util.List;


public interface PersistentSessionQueue {
	public void addEvents(SessionEvent... events);
	public void deleteEvents(SessionEvent... events);
	public void deleteAllEvents();
	public List<SessionEvent> getAllEvents();
}
