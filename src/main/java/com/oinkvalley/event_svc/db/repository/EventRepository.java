package com.oinkvalley.event_svc.db.repository;

import com.oinkvalley.event_svc.db.domain.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface EventRepository extends JpaRepository<Event, Long> {

	@Query("""
			SELECT e FROM Event e
			WHERE e.startTime < :rangeEnd AND e.endTime > :rangeStart
			""")
	List<Event> findOverlapping(
			@Param("rangeStart") Instant rangeStart,
			@Param("rangeEnd") Instant rangeEnd
	);
}
