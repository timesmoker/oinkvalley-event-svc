package com.oinkvalley.event_svc.service;

import com.oinkvalley.event_svc.config.CalendarProperties;
import com.oinkvalley.event_svc.db.domain.EventTag;
import com.oinkvalley.event_svc.db.domain.Tag;
import com.oinkvalley.event_svc.db.domain.TagType;
import com.oinkvalley.event_svc.db.repository.EventTagRepository;
import com.oinkvalley.event_svc.db.repository.TagRepository;
import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 일정–태그 연결 및 ETC 정규화(설계 시나리오 1–3). */
@Service
@RequiredArgsConstructor
@Transactional
public class EventTagLinkService {

    private final EventTagRepository eventTagRepository;
    private final TagRepository tagRepository;
    private final TagService tagService;
    private final EventAccessService eventAccessService;
    private final CalendarProperties calendarProperties;

    /**
     * 접근 가능한 태그만 파싱한 뒤 ETC 규칙 적용.
     * 일반 태그가 있으면 ETC 제거, 없으면 소유자 ETC 1개.
     */
    public List<Long> resolveEventTagIds(Long actorUserId, long eventOwnerId, List<String> rawTags) {
        List<Long> defaultIds = calendarProperties.defaultVisibleTagIds();
        long etcId = tagService.ensureEtcTag(eventOwnerId).getId();

        Set<Long> parsed = new HashSet<>();
        if (rawTags != null) {
            for (String raw : rawTags) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                try {
                    long id = Long.parseLong(raw.trim());
                    if (eventAccessService.canAccessTag(id, actorUserId, defaultIds)
                            || id == etcId) {
                        parsed.add(id);
                    }
                } catch (NumberFormatException ignored) {
                    // legacy slug — skip
                }
            }
        }

        if (parsed.isEmpty()) {
            return List.of(etcId);
        }

        boolean hasNonEtcUserTag = false;
        for (Long tagId : parsed) {
            if (tagId.equals(etcId)) {
                continue;
            }
            Tag tag = tagRepository.findById(tagId).orElse(null);
            if (tag != null && tag.getType() == TagType.USER) {
                hasNonEtcUserTag = true;
                break;
            }
        }

        if (!hasNonEtcUserTag) {
            return List.of(etcId);
        }

        List<Long> out = new ArrayList<>();
        for (Long tagId : parsed) {
            if (!tagId.equals(etcId)) {
                out.add(tagId);
            }
        }
        return out.stream().distinct().toList();
    }

    public void replaceEventTags(long eventId, List<Long> tagIds) {
        eventTagRepository.deleteByEventId(eventId);
        for (Long tagId : tagIds) {
            eventTagRepository.save(EventTag.builder()
                    .eventId(eventId)
                    .tagId(tagId)
                    .build());
        }
        tagService.markTagsUsed(tagIds);
    }
}
