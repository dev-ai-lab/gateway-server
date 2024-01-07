package com.bank.gatewayserver.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import reactor.core.publisher.Mono;

import static com.bank.gatewayserver.filters.FilterUtility.CORRELATION_ID;

@Configuration
public class ResponseTraceFilter {

	private static final Logger logger = LoggerFactory.getLogger(ResponseTraceFilter.class);

	@Autowired
	FilterUtility filterUtility;
	
	@Bean
	public GlobalFilter postGlobalFilter() {
		return (exchange, chain) -> chain.filter(exchange).then(Mono.fromRunnable(() -> {
			HttpHeaders requestHeaders = exchange.getRequest().getHeaders();
			String correlationId = filterUtility.getCorrelationId(requestHeaders);

			if (exchange.getResponse().getHeaders().containsKey(CORRELATION_ID)) {
				logger.debug("Updated the correlation id to the outbound headers. {}", correlationId);
				exchange.getResponse().getHeaders().add(CORRELATION_ID, correlationId);
			}
		}));
	}
}
