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
package com.github.wreulicke.bricks.spring;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.function.Supplier;

import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import reactor.core.publisher.Mono;

public class ClientCredentialTokenProvider implements Supplier<Mono<OAuth2AccessToken>> {

  private final WebClient client;

  private final URI tokenURI;

  private final ClientRegistration clientRegistration;

  public ClientCredentialTokenProvider(WebClient client, URI tokenURI, ClientRegistration clientRegistration) {
    this.client = client;
    this.tokenURI = tokenURI;
    this.clientRegistration = clientRegistration;
  }

  @Override
  public Mono<OAuth2AccessToken> get() {
    MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
    form.add("client_id", clientRegistration.getClientId());
    form.add("grant_type", "client_credentials");
    Set<String> scopes = clientRegistration.getScopes();
    if (!scopes.isEmpty()) {
      form.add("scope", String.join(" ", scopes));
    }

    return client.post()
      .uri(tokenURI)
      .accept(MediaType.APPLICATION_JSON_UTF8)
      .headers(httpHeaders -> httpHeaders.setBasicAuth(clientRegistration.getClientId(), clientRegistration.getClientSecret(),
        StandardCharsets.UTF_8))
      .body(BodyInserters.fromFormData(form))
      .retrieve()
      // TODO error handling
      // TODO
      .bodyToMono(OAuth2AccessToken.class);
  }
}
