package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.user.dto.PasswordChangeRequest;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PasswordServiceTests {

    private static final Long USER_ID = 1L;
    private static final String CURRENT = "Passhufs1234!";
    private static final String NEW = "Newpass5678!";

    private final UserRepository userRepository = mock(UserRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final PasswordService service = new PasswordService(userRepository, passwordEncoder);
    private final User user = mock(User.class);

    @BeforeEach
    void setUp() {
        when(user.getPassword()).thenReturn(passwordEncoder.encode(CURRENT));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
    }

    @Test
    void changesPasswordToBCryptHashOfNewPassword() {
        service.changePassword(USER_ID, new PasswordChangeRequest(CURRENT, NEW));

        org.mockito.ArgumentCaptor<String> saved = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(user).changePassword(saved.capture());
        assertEquals(true, passwordEncoder.matches(NEW, saved.getValue()));
        assertEquals(false, saved.getValue().equals(NEW));
    }

    @Test
    void wrongCurrentPasswordIs400AndKeepsPassword() {
        CustomException e = assertThrows(CustomException.class,
                () -> service.changePassword(USER_ID, new PasswordChangeRequest("wrong-password", NEW)));

        assertEquals(ErrorCode.CURRENT_PASSWORD_MISMATCH, e.getErrorCode());
        assertEquals("현재 비밀번호가 올바르지 않습니다", e.getMessage());
        verify(user, never()).changePassword(any());
    }

    @Test
    void deletedUserIs401() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        CustomException e = assertThrows(CustomException.class,
                () -> service.changePassword(USER_ID, new PasswordChangeRequest(CURRENT, NEW)));

        assertEquals(ErrorCode.UNAUTHORIZED, e.getErrorCode());
    }

    @Test
    void unexpectedFailureIs500WithApiMessage() {
        when(userRepository.findById(USER_ID)).thenThrow(new IllegalStateException("db down"));

        CustomException e = assertThrows(CustomException.class,
                () -> service.changePassword(USER_ID, new PasswordChangeRequest(CURRENT, NEW)));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("비밀번호 변경 중 오류가 발생했습니다", e.getMessage());
    }
}