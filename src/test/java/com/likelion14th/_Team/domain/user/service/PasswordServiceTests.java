package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;

import com.likelion14th._Team.domain.user.dto.PasswordChangeRequest;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;

import java.util.List;
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
    @Test
    void passwordUpTo72BytesIsAccepted() {
        // 영문 72자(72B), 한글 24자(72B)
        for (String pw : List.of("a".repeat(72), "가".repeat(24))) {
            service.changePassword(USER_ID, new PasswordChangeRequest(CURRENT, pw));
        }

        verify(user, times(2)).changePassword(any());
    }

    @Test
    void passwordOver72BytesIs400WithNewPasswordField() {
        // 영문 73자(73B), 한글 25자(75B)
        for (String pw : List.of("a".repeat(73), "가".repeat(25))) {
            CustomException e = assertThrows(CustomException.class,
                    () -> service.changePassword(USER_ID, new PasswordChangeRequest(CURRENT, pw)));

            assertEquals(ErrorCode.INVALID_INPUT, e.getErrorCode());
            assertEquals("newPassword", e.getErrors().get(0).field());
        }

        verify(userRepository, never()).findById(any());
        verify(user, never()).changePassword(any());
    }
}