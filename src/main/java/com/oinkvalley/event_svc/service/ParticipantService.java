package com.oinkvalley.event_svc.service;

import com.oinkvalley.event_svc.db.domain.CalendarUser;
import com.oinkvalley.event_svc.db.domain.EventParticipant;
import com.oinkvalley.event_svc.db.repository.CalendarUserRepository;
import com.oinkvalley.event_svc.db.repository.EventParticipantRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 참여자 이메일 ↔ userId 및 event_participants 동기화. */
@Service
@RequiredArgsConstructor
@Transactional
public class ParticipantService {

    private final CalendarUserRepository calendarUserRepository;
    private final EventParticipantRepository eventParticipantRepository;

    public List<Long> resolveParticipantIds(List<String> rawEmails, long eventOwnerId) {
        if (rawEmails == null || rawEmails.isEmpty()) {
            return List.of();
        }
        Set<String> normalized = new LinkedHashSet<>();
        for (String raw : rawEmails) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            normalized.add(raw.trim().toLowerCase());
        }
        if (normalized.isEmpty()) {
            return List.of();
        }

        List<CalendarUser> users = calendarUserRepository.findByEmailInIgnoreCase(normalized);
        List<Long> ids = new ArrayList<>();
        for (CalendarUser user : users) {
            if (user.getId() != eventOwnerId) {
                ids.add(user.getId());
            }
        }
        return ids.stream().distinct().toList();
    }

    public void replaceParticipants(long eventId, List<Long> participantIds) {
        eventParticipantRepository.deleteByEventId(eventId);
        if (participantIds == null || participantIds.isEmpty()) {
            return;
        }
        for (Long uid : participantIds) {
            if (uid == null || uid <= 0) {
                continue;
            }
            eventParticipantRepository.save(EventParticipant.builder()
                    .eventId(eventId)
                    .userId(uid)
                    .build());
        }
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public List<String> emailsForUserIds(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return List.of();
        }
        return calendarUserRepository.findAllById(userIds).stream()
                .map(CalendarUser::getEmail)
                .toList();
    }

    @Transactional(Transactional.TxType.SUPPORTS)
    public Map<Long, String> emailByUserId(List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        return calendarUserRepository.findAllById(userIds).stream()
                .collect(Collectors.toMap(CalendarUser::getId, CalendarUser::getEmail));
    }
}
