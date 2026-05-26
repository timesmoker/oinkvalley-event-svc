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
@Table(name = "event_participants")
@IdClass(EventParticipantId.class)
public class EventParticipant {

	@Id
	@NotNull
	@Column(name = "event_id", nullable = false)
	private Long eventId;

	@Id
	@NotNull
	@Column(name = "user_id", nullable = false)
	private Long userId;

	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	@PrePersist
	void prePersist() {
		this.createdAt = Instant.now();
	}
}
