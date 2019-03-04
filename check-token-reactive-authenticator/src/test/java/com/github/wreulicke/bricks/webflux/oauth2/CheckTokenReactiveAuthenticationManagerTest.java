/**
 * MIT License
 *
 * Copyright (c) 2019 Wreulicke
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.wreulicke.bricks.webflux.oauth2;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.reactive.function.client.ClientResponse;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class CheckTokenReactiveAuthenticationManagerTest {

  @Test
  void testAuthenticateWhenAuthenticationIsNull() {
    CheckTokenReactiveAuthenticationManager sut = new CheckTokenReactiveAuthenticationManager(null, null);
    Mono<Authentication> mono = sut.authenticate(null);

    StepVerifier.create(mono)
      .verifyComplete();
  }

  @Test
  void testAuthenticateToken() throws JsonProcessingException {
    Map<String, Object> map = new HashMap<>();
    map.put("client_id", "test_client");
    map.put("user_name", "test_user");

    ObjectMapper mapper = new ObjectMapper();
    String response = mapper.writeValueAsString(map);

    CheckTokenReactiveAuthenticationManager sut = new CheckTokenReactiveAuthenticationManager(null, null) {
      @Override
      Mono<ClientResponse> checkToken(String token) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
          .body(response)
          .headers(httpHeaders -> httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
          .build());
      }
    };

    Mono<Authentication> actual = sut.authenticateToken("token");

    StepVerifier.create(actual)
      .expectNextMatches(authentication -> authentication.isAuthenticated() && authentication instanceof CheckTokenAuthenticationToken)
      .expectComplete()
      .verify();
  }

  @Test
  void testAuthenticateTokenWhenTokenHasError() throws JsonProcessingException {
    Map<String, Object> map = new HashMap<>();
    map.put("error", "invalid_request");

    ObjectMapper mapper = new ObjectMapper();
    String response = mapper.writeValueAsString(map);

    CheckTokenReactiveAuthenticationManager sut = new CheckTokenReactiveAuthenticationManager(null, null) {
      @Override
      Mono<ClientResponse> checkToken(String token) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
          .body(response)
          .headers(httpHeaders -> httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
          .build());
      }
    };

    Mono<Authentication> actual = sut.authenticateToken("token");

    StepVerifier.create(actual)
      .expectErrorMessage("contains error: invalid_request")
      .verify();
  }

  @Test
  void testAuthenticateTokenWhenTokenIsNotActive() throws JsonProcessingException {
    Map<String, Object> map = new HashMap<>();
    map.put("active", false);

    ObjectMapper mapper = new ObjectMapper();
    String response = mapper.writeValueAsString(map);

    CheckTokenReactiveAuthenticationManager sut = new CheckTokenReactiveAuthenticationManager(null, null) {
      @Override
      Mono<ClientResponse> checkToken(String token) {
        return Mono.just(ClientResponse.create(HttpStatus.OK)
          .body(response)
          .headers(httpHeaders -> httpHeaders.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_UTF8_VALUE))
          .build());
      }
    };

    Mono<Authentication> actual = sut.authenticateToken("token");

    StepVerifier.create(actual)
      .expectErrorMessage("This token is not active")
      .verify();
  }

}
