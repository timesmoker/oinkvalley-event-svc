package com.oinkvalley.event_svc.dto.event;

import java.util.List;

public record EventResponse(
		long id,
		String title,
		List<String> tags,
		long ownerId,
		List<Long> participantIds,
		List<String> participantEmails,
		String startDate,
		String endDate,
		boolean allDay,
		String startTime,
		String endTime,
		String note,
		String link,
		String visibility
) {
}
