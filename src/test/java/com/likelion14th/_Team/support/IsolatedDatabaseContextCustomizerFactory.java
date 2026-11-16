package com.likelion14th._Team.support;

import java.util.List;
import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.testcontainers.mysql.MySQLContainer;

/** 모든 전체 컨텍스트 테스트에 적용하여 DB 테스트의 개별 설정 누락을 방지한다. */
public class IsolatedDatabaseContextCustomizerFactory implements ContextCustomizerFactory {
    @Override
    public ContextCustomizer createContextCustomizer(
            Class<?> testClass, List<ContextConfigurationAttributes> configAttributes) {
        if (!AnnotatedElementUtils.hasAnnotation(testClass, SpringBootTest.class)) {
            return null;
        }
        return new IsolatedDatabaseCustomizer();
    }

    // JVM마다 별도 컨테이너를 사용한다. 재사용하지 않으며 종료 시 Ryuk이 제거한다.
    private static class Database {
        static final MySQLContainer MYSQL = start();

        private static MySQLContainer start() {
            MySQLContainer mysql = new MySQLContainer("mysql:8.4")
                    .withDatabaseName("passhufs_test")
                    .withUsername("test")
                    .withPassword(UUID.randomUUID().toString())
                    .withCommand("--character-set-server=utf8mb4", "--collation-server=utf8mb4_unicode_ci")
                    .withReuse(false);
            mysql.start();
            return mysql;
        }
    }

    // 값 기반 동등성으로 같은 설정의 Spring 컨텍스트 캐시를 유지한다.
    private record IsolatedDatabaseCustomizer() implements ContextCustomizer {
        @Override
        public void customizeContext(ConfigurableApplicationContext context,
                MergedContextConfiguration mergedConfig) {
            MySQLContainer mysql = Database.MYSQL;
            // 환경변수·테스트 인라인 설정보다 우선한다. Docker 실패 시 개발 DB로 대체하지 않는다.
            String jdbcUrl = mysql.getJdbcUrl() + "?serverTimezone=Asia/Seoul";
            TestPropertyValues.of(
                    "spring.datasource.url=" + jdbcUrl,
                    "spring.datasource.hikari.jdbc-url=" + jdbcUrl,
                    "spring.datasource.hikari.username=" + mysql.getUsername(),
                    "spring.datasource.hikari.password=" + mysql.getPassword(),
                    "spring.datasource.username=" + mysql.getUsername(),
                    "spring.datasource.password=" + mysql.getPassword(),
                    "spring.datasource.driver-class-name=com.mysql.cj.jdbc.Driver",
                    "spring.jpa.hibernate.ddl-auto=update",
                    "spring.sql.init.mode=never",
                    "ai.mode=mock")
                    .applyTo(context.getEnvironment());
        }
    }
}
