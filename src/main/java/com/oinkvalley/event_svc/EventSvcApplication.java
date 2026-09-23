package com.oinkvalley.event_svc;

import com.oinkvalley.event_svc.config.CalendarProperties;
import com.oinkvalley.profile.v1.ProfileInternalServiceGrpc;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.grpc.client.ImportGrpcClients;

/** event-svc 진입점. */
@SpringBootApplication
@EnableConfigurationProperties(CalendarProperties.class)
@ImportGrpcClients(target = "profile", types = ProfileInternalServiceGrpc.ProfileInternalServiceBlockingStub.class)
public class EventSvcApplication {

	public static void main(String[] args) {
		SpringApplication.run(EventSvcApplication.class, args);
	}
}
