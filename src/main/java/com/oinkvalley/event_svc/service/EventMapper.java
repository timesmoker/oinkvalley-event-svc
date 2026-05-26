package com.oinkvalley.event_svc.service;

import com.oinkvalley.event_svc.db.domain.Event;
import com.oinkvalley.event_svc.dto.event.EventResponse;

import java.util.List;
import java.util.Map;

public final class EventMapper {

	private EventMapper() {
	}

	public static EventResponse toResponse(
			Event event,
			List<Long> tagIds,
			List<Long> participantIds,
			List<String> participantEmails
	) {
		boolean allDay = CalendarTimeUtil.isAllDaySpan(event.getStartTime(), event.getEndTime());
		String startDate = CalendarTimeUtil.formatDate(event.getStartTime());
		String endDate = allDay
				? CalendarTimeUtil.formatDate(event.getEndTime().minusSeconds(1))
				: CalendarTimeUtil.formatDate(event.getEndTime());
		if (endDate.compareTo(startDate) < 0) {
			endDate = startDate;
		}

		return new EventResponse(
				event.getId(),
				event.getTitle(),
				tagIds.stream().map(String::valueOf).toList(),
				event.getOwnerId(),
				participantIds,
				participantEmails == null ? List.of() : participantEmails,
				startDate,
				endDate,
				allDay,
				allDay ? null : CalendarTimeUtil.formatTime(event.getStartTime()),
				allDay ? null : CalendarTimeUtil.formatTime(event.getEndTime()),
				event.getDescription(),
				event.getLink(),
				event.getVisibility().name()
		);
	}

	public static Map<Long, List<Long>> tagIdsByEvent(List<com.oinkvalley.event_svc.db.domain.EventTag> links) {
		return links.stream().collect(
				java.util.stream.Collectors.groupingBy(
						com.oinkvalley.event_svc.db.domain.EventTag::getEventId,
						java.util.stream.Collectors.mapping(
								com.oinkvalley.event_svc.db.domain.EventTag::getTagId,
								java.util.stream.Collectors.toList()
						)
				)
		);
	}

	public static Map<Long, List<Long>> participantIdsByEvent(
			List<com.oinkvalley.event_svc.db.domain.EventParticipant> links
	) {
		return links.stream().collect(
				java.util.stream.Collectors.groupingBy(
						com.oinkvalley.event_svc.db.domain.EventParticipant::getEventId,
						java.util.stream.Collectors.mapping(
								com.oinkvalley.event_svc.db.domain.EventParticipant::getUserId,
								java.util.stream.Collectors.toList()
						)
				)
		);
	}
}
