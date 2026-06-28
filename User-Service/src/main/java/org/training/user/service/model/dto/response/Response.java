package org.training.user.service.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.training.user.service.model.Status;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Response {

    private String responseCode;

    private String responseMessage;

    private Long userId;

    private String emailId;

    private String displayName;

    private Status status;
}
