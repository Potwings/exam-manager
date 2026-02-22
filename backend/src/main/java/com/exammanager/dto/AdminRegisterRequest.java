package com.exammanager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class AdminRegisterRequest {
    @NotBlank(message = "아이디는 필수입니다")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 4, message = "비밀번호는 4자 이상이어야 합니다")
    @Size(max = 72, message = "비밀번호는 72자 이하여야 합니다")
    private String password;
}
