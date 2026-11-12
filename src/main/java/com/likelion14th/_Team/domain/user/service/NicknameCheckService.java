package com.likelion14th._Team.domain.user.service;

import com.likelion14th._Team.domain.user.dto.NicknameCheckResponse;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.text.Normalizer;
import java.util.List;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import com.likelion14th._Team.global.response.FieldErrorDetail;

@Service
@RequiredArgsConstructor
public class NicknameCheckService {
    private final UserRepository users;

    @Transactional(readOnly = true)
    public NicknameCheckResponse check(String input, Long userId) {
        String nickname = input == null ? "" : Normalizer.normalize(input, Normalizer.Form.NFC);
        if (!nickname.matches("[가-힣a-zA-Z0-9]{2,12}")) {
            throw new CustomException(ErrorCode.INVALID_INPUT, ErrorCode.INVALID_INPUT.getMessage(),
                    List.of(new FieldErrorDetail("nickname", "닉네임은 2~12자의 한글, 영문, 숫자만 사용할 수 있습니다")));
        }
        var owner = users.findByNicknameIgnoreCase(nickname);
        boolean current = owner.isPresent() && owner.get().getId().equals(userId);
        // 응답은 입력한 표기를 보존하고, 검증과 조회에는 NFC 정규화한 값을 사용한다.
        return new NicknameCheckResponse(input, owner.isEmpty() || current, current);
    }
}
