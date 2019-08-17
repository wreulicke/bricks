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
	
	public ClientCredentialTokenProvider(WebClient client, URI tokenURI,
		ClientRegistration clientRegistration) {
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
		
		return client
			.post()
			.uri(tokenURI)
			.accept(MediaType.APPLICATION_JSON_UTF8)
			.headers(httpHeaders -> httpHeaders
				.setBasicAuth(clientRegistration.getClientId(), clientRegistration.getClientSecret(),
					StandardCharsets.UTF_8))
			.body(BodyInserters.fromFormData(form))
			.retrieve()
			// TODO error handling
			// TODO
			.bodyToMono(OAuth2AccessToken.class);
	}
}
