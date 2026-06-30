package org.training.fundtransfer.external;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.training.fundtransfer.configuration.FeignClientConfiguration;
import org.training.fundtransfer.model.dto.NotificationRequest;

@FeignClient(name = "user-service", configuration = FeignClientConfiguration.class)
public interface UserService {

    @PostMapping("/api/users/notifications")
    ResponseEntity<Void> createNotification(@RequestBody NotificationRequest request);
}
