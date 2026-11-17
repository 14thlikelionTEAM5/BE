package com.likelion14th._Team.support;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.MajorType;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@ActiveProfiles("test")
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:mysql://invalid-host:1/never_connect",
        "spring.datasource.username=never_connect",
        "spring.datasource.hikari.jdbc-url=jdbc:mysql://invalid-host:1/never_connect",
        "spring.datasource.password=never_connect",
        "spring.jpa.hibernate.ddl-auto=none"
})
class DatabaseIsolationTests {
    @Autowired DataSource dataSource;
    @Autowired UserRepository users;
    @Autowired PlatformTransactionManager transactions;

    @Test
    void overridesExternalConnectionAndDdlSettingsWithContainerDatabase() throws Exception {
        try (var connection = dataSource.getConnection()) {
            assertEquals("passhufs_test", connection.getCatalog());
            assertFalse(connection.getMetaData().getURL().contains("invalid-host"));
            assertEquals("MySQL", connection.getMetaData().getDatabaseProductName());
        }
        // 외부 ddl-auto=none 설정과 무관하게 테스트 테이블이 생성되어야 한다.
        users.count();
    }

    @Test
    void committedFixtureIsRemovedByItsOwnIdAndRolledBackFixtureDoesNotRemain() {
        var tx = new TransactionTemplate(transactions);
        String key = UUID.randomUUID().toString().replace("-", "");
        Long committedId = tx.execute(status -> users.saveAndFlush(user(key)).getId());
        try {
            assertTrue(users.existsById(committedId));
            String rolledBackKey = UUID.randomUUID().toString().replace("-", "");
            Long rolledBackId = tx.execute(status -> {
                Long id = users.saveAndFlush(user(rolledBackKey)).getId();
                status.setRollbackOnly();
                return id;
            });
            assertFalse(users.existsById(rolledBackId));
        } finally {
            tx.executeWithoutResult(status -> users.deleteById(committedId));
        }
        assertFalse(users.existsById(committedId));
    }

    private User user(String key) {
        return User.builder().schoolEmail(key + "@example.hufs.ac.kr")
                .nickname(key.substring(0, 12)).password(UUID.randomUUID().toString())
                .majorType(MajorType.PRIMARY).build();
    }
}
