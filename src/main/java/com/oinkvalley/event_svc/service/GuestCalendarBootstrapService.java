package com.oinkvalley.event_svc.service;

import com.oinkvalley.event_svc.config.GuestCalendarBootstrapCatalog;
import com.oinkvalley.event_svc.config.GuestCalendarProperties;
import com.oinkvalley.event_svc.dto.guest.GuestCalendarSubscribeSpec;
import com.oinkvalley.event_svc.db.domain.Event;
import com.oinkvalley.event_svc.db.domain.Visibility;
import com.oinkvalley.event_svc.db.repository.EventRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** 게스트 계정용 샘플 일정. {@code POST /events/guest-bootstrap} 전용. 샘플 SOT: Helm chart. */
@Service
@RequiredArgsConstructor
public class GuestCalendarBootstrapService {

    private static final ZoneId ZONE = CalendarTimeUtil.ZONE;

    private final GuestCalendarProperties guestCalendarProperties;
    private final GuestCalendarBootstrapCatalog guestCalendarBootstrapCatalog;
    private final EventRepository eventRepository;
    private final TagService tagService;
    private final EventTagLinkService eventTagLinkService;

    /** 비활성·샘플 없음·이미 일정 있으면 no-op. */
    @Transactional
    public void bootstrap(Long userId) {
        if (userId == null || !guestCalendarProperties.isEnabled()) {
            return;
        }
        List<GuestCalendarBootstrapCatalog.ResolvedGuestCalendarSample> samples =
                guestCalendarBootstrapCatalog.resolveSamples(LocalDate.now(ZONE));
        if (samples.isEmpty()) {
            return;
        }
        if (eventRepository.existsByOwnerId(userId)) {
            return;
        }

        subscribeConfiguredTags(userId);

        for (GuestCalendarBootstrapCatalog.ResolvedGuestCalendarSample sample : samples) {
            Event event = eventRepository.save(Event.builder()
                    .ownerId(userId)
                    .title(sample.title())
                    .description(sample.description())
                    .visibility(Visibility.PRIVATE)
                    .startTime(sample.start())
                    .endTime(sample.end())
                    .build());
            eventTagLinkService.replaceEventTags(event.getId(), resolveOwnedTagIds(userId, sample.tagNames()));
        }
    }

    /** 공유 태그 구독 — Helm {@code subscribeTags} (ownerUserId + tagIds). */
    private void subscribeConfiguredTags(Long userId) {
        for (GuestCalendarSubscribeSpec spec : guestCalendarBootstrapCatalog.subscribeTags()) {
            if (spec.tagIds() == null) {
                continue;
            }
            for (Long tagId : spec.tagIds()) {
                if (tagId == null || tagId <= 0) {
                    continue;
                }
                tagService.followOwnerTagIfAllowed(userId, spec.ownerUserId(), tagId);
            }
        }
    }

    /** 샘플 태그 이름 → 게스트 본인 소유 태그 생성/재사용. 비면 ETC. */
    private List<Long> resolveOwnedTagIds(Long userId, List<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return List.of(tagService.ensureEtcTag(userId).getId());
        }
        Set<Long> ids = new LinkedHashSet<>();
        for (String name : tagNames) {
            if (name == null || name.isBlank()) {
                continue;
            }
            ids.add(tagService.ensureOwnedUserTagByName(userId, name).getId());
        }
        if (ids.isEmpty()) {
            return List.of(tagService.ensureEtcTag(userId).getId());
        }
        return new ArrayList<>(ids);
    }
}
