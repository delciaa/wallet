package io.github.delciaa.wallet.user;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/me")
public class ProfileController {

	private final ProfileService profileService;

	public ProfileController(ProfileService profileService) {
		this.profileService = profileService;
	}

	/** The user always comes from the verified token, never from the request. */
	@GetMapping
	public ProfileResponse me(@AuthenticationPrincipal Jwt jwt) {
		return profileService.profileOf(UUID.fromString(jwt.getSubject()));
	}

}
