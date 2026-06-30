package org.training.user.service.model.dto.beneficiary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BeneficiaryRequest {

    @NotBlank(message = "Vui lòng chọn ngân hàng")
    @Size(max = 120, message = "Tên ngân hàng quá dài")
    private String bankName;

    @NotBlank(message = "Vui lòng nhập số tài khoản")
    @Size(max = 40, message = "Số tài khoản quá dài")
    private String accountNumber;

    @NotBlank(message = "Vui lòng nhập tên chủ tài khoản")
    @Size(max = 160, message = "Tên chủ tài khoản quá dài")
    private String accountHolderName;

    @Size(max = 120, message = "Tên gợi nhớ quá dài")
    private String nickname;
}
