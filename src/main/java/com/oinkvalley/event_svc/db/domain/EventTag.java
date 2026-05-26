package com.oinkvalley.event_svc.db.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.Instant;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@Table(name = "event_tags")
@IdClass(EventTagId.class)
public class EventTag {

	@Id
	@NotNull
	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Id
	@NotNull
	@Column(name = "tag_id", nullable = false)
	private Long tagId;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}
}
