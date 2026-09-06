package com.lingxi.platform.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.lingxi.kernel.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.http.MockHttpInputMessage;
import org.springframework.mock.web.MockHttpServletRequest;

class GlobalExceptionHandlerTest {
  private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

  @Test
  void mapsAuthenticationAuthorizationResourceAndConflictErrors() {
    assertStatus("AUTH_UNAUTHENTICATED", HttpStatus.UNAUTHORIZED);
    assertStatus("AUTH_ADMIN_REQUIRED", HttpStatus.FORBIDDEN);
    assertStatus("CONTENT_FILE_NOT_FOUND", HttpStatus.NOT_FOUND);
    assertStatus("REL_CONCURRENT_UPDATE", HttpStatus.CONFLICT);
  }

  @Test
  void mapsMalformedRequestToBadRequest() {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE, "test-request");

    var response =
        handler.handleMalformedRequest(
            new HttpMessageNotReadableException(
                "invalid json", new MockHttpInputMessage(new byte[0])),
            request);

    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().code()).isEqualTo("COMMON_INVALID_REQUEST");
  }

  private void assertStatus(String code, HttpStatus expected) {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.setAttribute(RequestTraceFilter.REQUEST_ID_ATTRIBUTE, "test-request");
    assertThat(
            handler
                .handleBusinessException(new BusinessException(code, "message"), request)
                .getStatusCode())
        .isEqualTo(expected);
  }
}
