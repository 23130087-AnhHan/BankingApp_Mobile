package org.training.user.service.model.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUser {

    @NotBlank(message = "Tên không được để trống")
    @Size(min = 2, message = "Họ tên không hợp lệ")
    @Pattern(regexp = "^\\p{L}(?:[\\p{L} .'-]*\\p{L})?$", message = "Họ tên không hợp lệ")
    private String firstName;

    @NotBlank(message = "Họ không được để trống")
    @Size(min = 2, message = "Họ tên không hợp lệ")
    @Pattern(regexp = "^\\p{L}(?:[\\p{L} .'-]*\\p{L})?$", message = "Họ tên không hợp lệ")
    private String lastName;

    @NotBlank(message = "Số điện thoại không được để trống")
    @Pattern(regexp = "^0[0-9]{9}$", message = "Số điện thoại phải bắt đầu bằng 0 và có đúng 10 chữ số")
    private String contactNumber;

    @NotBlank(message = "Email không được để trống")
    @Email(message = "Email không đúng định dạng")
    private String emailId;

    @NotBlank(message = "Số CCCD không được để trống")
    @Pattern(regexp = "^[0-9]{12}$", message = "Số CCCD phải có đúng 12 chữ số")
    private String identificationNumber;

    @NotBlank(message = "Mật khẩu không được để trống")
    @Size(min = 8, max = 72, message = "Mật khẩu phải có ít nhất 8 ký tự")
    @Pattern(regexp = "^\\S+$", message = "Mật khẩu không được chứa khoảng trắng")
    @Pattern(
            regexp = "^(?=.*[A-Z])(?=.*[a-z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s]).*$",
            message = "Mật khẩu phải gồm chữ hoa, chữ thường, số và ký tự đặc biệt")
    private String password;
}
