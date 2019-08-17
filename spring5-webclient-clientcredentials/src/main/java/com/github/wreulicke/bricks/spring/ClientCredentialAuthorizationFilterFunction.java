package com.github.wreulicke.bricks.spring;

import java.util.function.Supplier;

import org.springframework.security.oauth2.client.endpoint.AbstractOAuth2AuthorizationGrantRequest;
import org.springframework.security.oauth2.client.endpoint.ReactiveOAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ExchangeFunction;

import reactor.core.publisher.Mono;

/**
 *
 */
public class ClientCredentialAuthorizationFilterFunction<T extends AbstractOAuth2AuthorizationGrantRequest> implements ExchangeFilterFunction {
	
	private final ReactiveOAuth2AccessTokenResponseClient<T> client;
	
	private final Supplier<T> requestSupplier;
	
	public ClientCredentialAuthorizationFilterFunction(
		ReactiveOAuth2AccessTokenResponseClient<T> client,
		Supplier<T> requestSupplier) {
		this.client = client;
		this.requestSupplier = requestSupplier;
	}
	
	@Override
	public Mono<ClientResponse> filter(ClientRequest request, ExchangeFunction next) {
		return client.getTokenResponse(requestSupplier.get())
			.map(OAuth2AccessTokenResponse::getAccessToken)
			.flatMap(oAuth2AccessToken -> {
				request.headers().setBearerAuth(oAuth2AccessToken.getTokenValue());
				return next.exchange(request);
			});
	}
}
