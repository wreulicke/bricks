/**
 * MIT License
 *
 * Copyright (c) 2017 Wreulicke
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

import java.net.URI;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.resource.BearerTokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.BearerTokenError;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class CheckTokenReactiveAuthenticationManager implements ReactiveAuthenticationManager {

  private static final ParameterizedTypeReference<Map<String, Object>> TYPE_REFERENCE = new ParameterizedTypeReference<Map<String, Object>>() {};

  private final WebClient webClient;

  private final URI checkTokenUri;

  public CheckTokenReactiveAuthenticationManager(WebClient webClient, URI checkTokenUri) {
    this.webClient = webClient;
    this.checkTokenUri = checkTokenUri;
  }

  @Override
  public Mono<Authentication> authenticate(@Nullable Authentication authentication) {
    return Mono.justOrEmpty(authentication)
      .flatMap(authentication1 -> {
        if (authentication instanceof BearerTokenAuthenticationToken) {
          return Mono.just((BearerTokenAuthenticationToken) authentication);
        }
        return Mono.empty();
      })
      .map(BearerTokenAuthenticationToken::getToken)
      .flatMap(this::authenticate)
      .onErrorMap(throwable -> !(throwable instanceof OAuth2AuthenticationException), this::onError);
  }

  public Mono<Authentication> authenticate(String token) {
    return checkToken(token).flatMap(clientResponse -> clientResponse.bodyToMono(TYPE_REFERENCE))
      .map(map -> {
        if (map.containsKey("error")) {
          throw new OAuth2AuthenticationException(invalidToken("contains error: " + map.get("error")));
        }
        if (!map.containsKey("active")) {
          throw new OAuth2AuthenticationException(invalidToken("This token is not active"));
        }
        else {
          Object active = map.get("active");
          if (active instanceof Boolean && !((Boolean) active)) {
            throw new OAuth2AuthenticationException(invalidToken("This token is not active"));
          }
        }

        Instant expiresAt = null;
        if (map.containsKey("exp")) {
          Object exp = map.get("exp");
          if (exp instanceof Long) {
            expiresAt = Instant.ofEpochMilli((Long) exp);
          }
          else if (exp instanceof Integer) {
            expiresAt = Instant.ofEpochMilli(((Integer) exp).longValue());
          }
        }

        List<GrantedAuthority> authorities = new ArrayList<>();
        if (map.containsKey("authorities")) {
          Object list = map.get("authorities");
          if (list instanceof Collection) {
            authorities = AuthorityUtils.createAuthorityList(((Collection<String>) list).toArray(new String[0]));
          }
        }
        Set<String> scopes = new HashSet<>();
        if (map.containsKey("scope")) {
          Collection<String> scope = (Collection<String>) map.get("scope");
          scopes.addAll(scope);
        }

        OAuth2AccessToken accessToken = new OAuth2AccessToken(OAuth2AccessToken.TokenType.BEARER, token, null, expiresAt, scopes);
        String client_id = (String) map.get("client_id");
        String userName = (String) map.get("user_name");
        CheckTokenAuthenticationToken checkTokenAuthenticationToken = new CheckTokenAuthenticationToken(accessToken, map, authorities, client_id);
        checkTokenAuthenticationToken.setUserName(userName);
        return checkTokenAuthenticationToken;
      });
  }

  public Mono<ClientResponse> checkToken(String token) {
    return webClient.post()
      .uri(checkTokenUri)
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .body(BodyInserters.fromFormData("token", token))
      .exchange();
  }

  private BearerTokenError invalidToken(String message) {
    return new BearerTokenError("invalid_token", HttpStatus.UNAUTHORIZED, message, "https://tools.ietf.org/html/rfc7662#section-2.2");
  }

  private Throwable onError(Throwable throwable) {
    BearerTokenError invalidToken = invalidToken(throwable.getMessage());
    return new OAuth2AuthenticationException(invalidToken, throwable.getMessage());
  }
}
