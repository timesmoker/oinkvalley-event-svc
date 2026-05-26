package com.oinkvalley.event_svc.dto.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record UpdateEventRequest(
		@NotBlank @Size(max = 200) String title,
		List<String> tags,
		List<String> participantEmails,
		@NotBlank String startDate,
		@NotBlank String endDate,
		boolean allDay,
		String startTime,
		String endTime,
		String note,
		String link,
		String visibility
) {
}
