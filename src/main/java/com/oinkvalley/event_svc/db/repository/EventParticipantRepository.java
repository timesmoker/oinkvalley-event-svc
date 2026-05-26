package com.oinkvalley.event_svc.db.repository;

import com.oinkvalley.event_svc.db.domain.EventParticipant;
import com.oinkvalley.event_svc.db.domain.EventParticipantId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface EventParticipantRepository extends JpaRepository<EventParticipant, EventParticipantId> {

	boolean existsByEventIdAndUserId(Long eventId, Long userId);

	@Query("SELECT ep.userId FROM EventParticipant ep WHERE ep.eventId = :eventId")
	List<Long> findUserIdsByEventId(@Param("eventId") Long eventId);

	@Query("SELECT ep FROM EventParticipant ep WHERE ep.eventId IN :eventIds")
	List<EventParticipant> findByEventIdIn(@Param("eventIds") Collection<Long> eventIds);

	void deleteByEventId(Long eventId);
}
