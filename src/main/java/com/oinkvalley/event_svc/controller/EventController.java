package com.oinkvalley.event_svc.controller;

import com.oinkvalley.event_svc.dto.event.CreateEventRequest;
import com.oinkvalley.event_svc.dto.event.EventResponse;
import com.oinkvalley.event_svc.dto.event.UpdateEventRequest;
import com.oinkvalley.event_svc.service.EventService;
import jakarta.validation.Valid;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequiredArgsConstructor
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;

    /**
     * @param scope {@code visible}(기본) — 볼 수 있는 일정, {@code mine} — 소유·참여 일정 (로그인 필요)
     */
    @GetMapping
    public ResponseEntity<List<EventResponse>> list(
            @AuthenticationPrincipal Long userId,
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam(required = false) String tagIds,
            @RequestParam(defaultValue = "visible") String scope
    ) {
        if ("mine".equalsIgnoreCase(scope) && userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required for scope=mine");
        }
        return ResponseEntity.ok(eventService.listVisible(userId, from, to, parseTagIds(tagIds), scope));
    }

    @PostMapping
    public ResponseEntity<EventResponse> create(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateEventRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(eventService.create(userId, request));
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventResponse> update(
            @PathVariable long eventId,
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody UpdateEventRequest request
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        return ResponseEntity.ok(eventService.update(userId, eventId, request));
    }

    @DeleteMapping("/{eventId}")
    public ResponseEntity<Void> delete(
            @PathVariable long eventId,
            @AuthenticationPrincipal Long userId
    ) {
        if (userId == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        }
        eventService.delete(userId, eventId);
        return ResponseEntity.noContent().build();
    }

    private static List<Long> parseTagIds(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Long> ids = new ArrayList<>();
        for (String part : raw.split(",")) {
            String trimmed = part.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            try {
                ids.add(Long.parseLong(trimmed));
            } catch (NumberFormatException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid tagIds: " + raw);
            }
        }
        return List.copyOf(ids);
    }
}
