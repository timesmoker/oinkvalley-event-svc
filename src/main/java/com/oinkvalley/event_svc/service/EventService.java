package com.oinkvalley.event_svc.service;

import com.oinkvalley.event_svc.config.CalendarProperties;
import com.oinkvalley.event_svc.db.domain.Event;
import com.oinkvalley.event_svc.db.domain.Visibility;
import com.oinkvalley.event_svc.db.repository.EventParticipantRepository;
import com.oinkvalley.event_svc.db.repository.EventRepository;
import com.oinkvalley.event_svc.db.repository.EventTagRepository;
import com.oinkvalley.event_svc.dto.event.CreateEventRequest;
import com.oinkvalley.event_svc.dto.event.EventResponse;
import com.oinkvalley.event_svc.dto.event.UpdateEventRequest;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** 일정 조회·생성·수정·삭제. */
@Service
@RequiredArgsConstructor
@Transactional
public class EventService {

    private final EventRepository eventRepository;
    private final EventTagRepository eventTagRepository;
    private final EventParticipantRepository eventParticipantRepository;
    private final EventAccessService eventAccessService;
    private final EventTagLinkService eventTagLinkService;
    private final ParticipantService participantService;
    private final CalendarProperties calendarProperties;

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<EventResponse> listVisible(
            Long userId,
            String from,
            String to,
            List<Long> filterTagIds,
            String scope
    ) {
        Instant rangeStart = CalendarTimeUtil.rangeStart(from);
        Instant rangeEnd = CalendarTimeUtil.rangeEndExclusive(to);
        List<Long> defaultTagIds = calendarProperties.defaultVisibleTagIds();
        Set<Long> filter = filterTagIds == null ? Set.of() : new HashSet<>(filterTagIds);
        boolean mineOnly = "mine".equalsIgnoreCase(scope);

        if (mineOnly && userId == null) {
            return List.of();
        }

        List<Event> candidates = eventRepository.findOverlapping(rangeStart, rangeEnd);
        Set<Long> visibleIds = new LinkedHashSet<>();
        for (Event event : candidates) {
            if (!eventAccessService.canView(event, userId, defaultTagIds)) {
                continue;
            }
            if (mineOnly && userId != null && !eventAccessService.isMine(event, userId)) {
                continue;
            }
            if (!filter.isEmpty()) {
                List<Long> tagIds = eventTagRepository.findTagIdsByEventId(event.getId());
                boolean matches = tagIds.stream().anyMatch(filter::contains);
                if (!matches) {
                    continue;
                }
            }
            visibleIds.add(event.getId());
        }
        if (visibleIds.isEmpty()) {
            return List.of();
        }

        return buildResponses(List.copyOf(visibleIds));
    }

    public EventResponse create(Long userId, CreateEventRequest request) {
        Event event = saveNewEvent(userId, request);
        List<Long> tagIds = eventTagLinkService.resolveEventTagIds(
                userId, userId, request.tags());
        eventTagLinkService.replaceEventTags(event.getId(), tagIds);
        List<Long> linkedTagIds = eventTagRepository.findTagIdsByEventId(event.getId());

        List<Long> participantIds = participantService.resolveParticipantIds(
                request.participantEmails(), userId);
        participantService.replaceParticipants(event.getId(), participantIds);

        return toResponse(event, linkedTagIds, participantIds);
    }

    public EventResponse update(Long userId, long eventId, UpdateEventRequest request) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + eventId));
        if (!event.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event owner can update this event");
        }

        Instant start = CalendarTimeUtil.toStartInstant(
                request.startDate(), request.allDay(), request.startTime());
        Instant end = CalendarTimeUtil.toEndInstant(
                request.endDate(), request.allDay(), request.endTime());
        if (!end.isAfter(start)) {
            end = start.plusSeconds(request.allDay() ? 24 * 3600 : 3600);
        }

        event.updateDetails(
                request.title().trim(),
                blankToNull(request.note()),
                blankToNull(request.link()),
                parseEventVisibility(request.visibility()),
                start,
                end
        );

        List<Long> tagIds = eventTagLinkService.resolveEventTagIds(
                userId, event.getOwnerId(), request.tags());
        eventTagLinkService.replaceEventTags(event.getId(), tagIds);
        List<Long> linkedTagIds = eventTagRepository.findTagIdsByEventId(event.getId());

        List<Long> participantIds = participantService.resolveParticipantIds(
                request.participantEmails(), event.getOwnerId());
        participantService.replaceParticipants(event.getId(), participantIds);

        return toResponse(event, linkedTagIds, participantIds);
    }

    public void delete(Long userId, long eventId) {
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found: " + eventId));
        if (!event.getOwnerId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the event owner can delete this event");
        }
        eventRepository.delete(event);
    }

    private Event saveNewEvent(Long userId, CreateEventRequest request) {
        Instant start = CalendarTimeUtil.toStartInstant(
                request.startDate(), request.allDay(), request.startTime());
        Instant end = CalendarTimeUtil.toEndInstant(
                request.endDate(), request.allDay(), request.endTime());
        if (!end.isAfter(start)) {
            end = start.plusSeconds(request.allDay() ? 24 * 3600 : 3600);
        }

        Event event = Event.builder()
                .ownerId(userId)
                .title(request.title().trim())
                .description(blankToNull(request.note()))
                .link(blankToNull(request.link()))
                .visibility(parseEventVisibility(request.visibility()))
                .startTime(start)
                .endTime(end)
                .build();
        return eventRepository.save(event);
    }

    private List<EventResponse> buildResponses(List<Long> visibleIdList) {
        List<Event> events = eventRepository.findAllById(visibleIdList);
        var eventTags = eventTagRepository.findByEventIdIn(visibleIdList);
        var participants = eventParticipantRepository.findByEventIdIn(visibleIdList);
        Map<Long, List<Long>> tagsByEvent = EventMapper.tagIdsByEvent(eventTags);
        Map<Long, List<Long>> participantsByEvent = EventMapper.participantIdsByEvent(participants);

        Set<Long> allParticipantIds = new HashSet<>();
        for (List<Long> ids : participantsByEvent.values()) {
            allParticipantIds.addAll(ids);
        }
        Map<Long, String> emailByUserId = participantService.emailByUserId(new ArrayList<>(allParticipantIds));

        return events.stream()
                .map(e -> {
                    List<Long> pids = participantsByEvent.getOrDefault(e.getId(), List.of());
                    List<String> emails = pids.stream()
                            .map(id -> emailByUserId.getOrDefault(id, ""))
                            .filter(s -> !s.isBlank())
                            .toList();
                    return EventMapper.toResponse(
                            e,
                            tagsByEvent.getOrDefault(e.getId(), List.of()),
                            pids,
                            emails
                    );
                })
                .toList();
    }

    private EventResponse toResponse(Event event, List<Long> tagIds, List<Long> participantIds) {
        List<String> emails = participantService.emailsForUserIds(participantIds);
        return EventMapper.toResponse(event, tagIds, participantIds, emails);
    }

    private static Visibility parseEventVisibility(String raw) {
        if (raw == null || raw.isBlank()) {
            return Visibility.PRIVATE;
        }
        try {
            return Visibility.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid visibility: " + raw);
        }
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
