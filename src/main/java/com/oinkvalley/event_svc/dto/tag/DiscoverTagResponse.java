package com.oinkvalley.event_svc.dto.tag;

public record DiscoverTagResponse(
		long id,
		String name,
		String visibility,
		long ownerId,
		String ownerEmail,
		String ownerNickname
) {
}
