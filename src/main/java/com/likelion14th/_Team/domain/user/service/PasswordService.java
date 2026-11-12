package com.likelion14th._Team.domain.user.service;

import com.likelion14th._Team.domain.user.dto.PasswordChangeRequest;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void changePassword(Long userId, PasswordChangeRequest request) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new CustomException(ErrorCode.UNAUTHORIZED));

            if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
                throw new CustomException(ErrorCode.CURRENT_PASSWORD_MISMATCH);
            }
            user.changePassword(passwordEncoder.encode(request.newPassword()));
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("비밀번호 변경 중 예외", e);
            throw new CustomException(ErrorCode.INTERNAL_ERROR, "비밀번호 변경 중 오류가 발생했습니다");
        }
    }
}