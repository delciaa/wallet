package io.github.delciaa.wallet.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

// No format rules beyond "not blank", so a failed login can't hint at what a valid account looks like
public record LoginRequest(@NotBlank @Size(max = 254) String email, @NotBlank @Size(max = 72) String password) {
}
