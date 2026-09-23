package com.oinkvalley.event_svc.db.repository;

import com.oinkvalley.event_svc.db.domain.EventTag;
import com.oinkvalley.event_svc.db.domain.EventTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventTagRepository extends JpaRepository<EventTag, EventTagId> {

	List<EventTag> findByEventId(Long eventId);

	@Query("SELECT et.tagId FROM EventTag et WHERE et.eventId = :eventId")
	List<Long> findTagIdsByEventId(@Param("eventId") Long eventId);

	@Query("SELECT et FROM EventTag et WHERE et.eventId IN :eventIds")
	List<EventTag> findByEventIdIn(@Param("eventIds") Collection<Long> eventIds);

	List<EventTag> findByTagId(Long tagId);

	void deleteByEventIdAndTagId(Long eventId, Long tagId);

	void deleteByEventId(Long eventId);
}
