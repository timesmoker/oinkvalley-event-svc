package com.oinkvalley.event_svc.db.domain;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class EventParticipantId implements Serializable {

	private Long eventId;
	private Long userId;
}
