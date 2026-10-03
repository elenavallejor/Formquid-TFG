package com.formquid.participation.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class AnonymousRespondentServiceTest {

    private AnonymousRespondentService service;

    @BeforeEach
    void setUp() {
        service = new AnonymousRespondentService();
        ReflectionTestUtils.setField(service, "secure", true);
        ReflectionTestUtils.setField(service, "sameSite", "Lax");
    }

    @Test
    void resolve_reusesValidTokenAndScopesHashToSurveyCode() {
        String token = "a".repeat(43);

        var firstSurvey = service.resolve(token, "abc123");
        var sameSurvey = service.resolve(token, " ABC123 ");
        var otherSurvey = service.resolve(token, "xyz987");

        assertThat(firstSurvey.newCookie()).isNull();
        assertThat(firstSurvey.tokenHash()).hasSize(64).isEqualTo(sameSurvey.tokenHash());
        assertThat(otherSurvey.tokenHash()).isNotEqualTo(firstSurvey.tokenHash());
    }

    @Test
    void resolve_createsSecureHttpOnlyCookieForMissingToken() {
        var identity = service.resolve(null, "ABC123");

        assertThat(identity.tokenHash()).hasSize(64);
        assertThat(identity.newCookie()).isNotNull();
        assertThat(identity.newCookie().isHttpOnly()).isTrue();
        assertThat(identity.newCookie().isSecure()).isTrue();
        assertThat(identity.newCookie().getPath()).isEqualTo("/");
        assertThat(identity.newCookie().getMaxAge().toDays()).isEqualTo(365);
        assertThat(identity.newCookie().getValue()).hasSize(43);
    }
}
