package com.likelion14th._Team.global.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("14th LikeLion Team5 API")
						.description("멋쟁이사자처럼 14기 5팀 백엔드 API 문서")
						.version("v1"));
	}
}
