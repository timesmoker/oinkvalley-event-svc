package com.oinkvalley.event_svc.service;

import com.oinkvalley.event_svc.db.domain.Event;
import com.oinkvalley.event_svc.db.domain.Tag;
import com.oinkvalley.event_svc.db.domain.Visibility;
import com.oinkvalley.event_svc.db.repository.EventParticipantRepository;
import com.oinkvalley.event_svc.db.repository.EventTagRepository;
import com.oinkvalley.event_svc.db.repository.TagMemberRepository;
import com.oinkvalley.event_svc.db.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** 일정·태그 visibility 및 공유 멤버 기준 조회 권한. */
@Service
@RequiredArgsConstructor
public class EventAccessService {

	private final EventTagRepository eventTagRepository;
	private final EventParticipantRepository eventParticipantRepository;
	private final TagRepository tagRepository;
	private final TagMemberRepository tagMemberRepository;

	public boolean isMine(Event event, long userId) {
		if (event.getOwnerId() == userId) {
			return true;
		}
		return eventParticipantRepository.existsByEventIdAndUserId(event.getId(), userId);
	}

	public boolean canView(Event event, Long userId, Collection<Long> defaultVisibleTagIds) {
		if (userId == null) {
			return event.getVisibility() == Visibility.PUBLIC;
		}
		if (isMine(event, userId)) {
			return true;
		}
		if (event.getVisibility() == Visibility.PUBLIC) {
			return true;
		}

		List<Long> linkedTagIds = eventTagRepository.findTagIdsByEventId(event.getId());
		if (linkedTagIds.isEmpty()) {
			return false;
		}

		Set<Long> defaultTags = defaultVisibleTagIds == null
				? Set.of()
				: new HashSet<>(defaultVisibleTagIds);

		boolean onDefaultTag = linkedTagIds.stream().anyMatch(defaultTags::contains);
		if (onDefaultTag && (event.getVisibility() == Visibility.PUBLIC || event.getVisibility() == Visibility.SHARED)) {
			return true;
		}

		if (event.getVisibility() != Visibility.SHARED) {
			return false;
		}

		for (Long tagId : linkedTagIds) {
			if (canAccessTag(tagId, userId, defaultTags)) {
				return true;
			}
		}
		return false;
	}

	public boolean canAccessTag(long tagId, Long userId, Collection<Long> defaultVisibleTagIds) {
		if (userId == null) {
			return tagRepository.findById(tagId)
					.map(t -> t.getVisibility() == Visibility.PUBLIC)
					.orElse(false);
		}
		Set<Long> defaultTags = defaultVisibleTagIds == null
				? Set.of()
				: new HashSet<>(defaultVisibleTagIds);
		if (defaultTags.contains(tagId)) {
			return true;
		}
		return tagRepository.findById(tagId)
				.map(tag -> canAccessTagEntity(tag, userId))
				.orElse(false);
	}

	private boolean canAccessTagEntity(Tag tag, long userId) {
		if (tag.getOwnerId() == userId) {
			return true;
		}
		if (tag.getVisibility() == Visibility.PUBLIC) {
			return true;
		}
		if (tag.getVisibility() == Visibility.SHARED) {
			return tagMemberRepository.existsByTagIdAndUserId(tag.getId(), userId);
		}
		return false;
	}
}
