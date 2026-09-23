package com.oinkvalley.event_svc.client;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/** profile-svc `GET /profiles?ids=` — `user_profiles` 는 profile-svc 만 조회한다. */
@Component
@RequiredArgsConstructor
public class UserProfileClient {

    private final RestClient userProfileRestClient;

    public String nicknameForUser(long userId) {
        return nicknamesByUserIds(List.of(userId)).get(userId);
    }

    public Map<Long, String> nicknamesByUserIds(Collection<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        String idsParam = userIds.stream()
                .filter(id -> id != null && id > 0)
                .distinct()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        if (idsParam.isBlank()) {
            return Map.of();
        }
        try {
            List<ProfileNicknameDto> body = userProfileRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/profiles")
                            .queryParam("ids", idsParam)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            if (body == null || body.isEmpty()) {
                return Map.of();
            }
            Map<Long, String> out = new HashMap<>();
            for (ProfileNicknameDto dto : body) {
                if (dto.userId() != null && dto.nickname() != null && !dto.nickname().isBlank()) {
                    out.put(dto.userId(), dto.nickname().trim());
                }
            }
            return out;
        } catch (RestClientException e) {
            return Map.of();
        }
    }

    public record ProfileNicknameDto(Long userId, String nickname) {
    }
}
