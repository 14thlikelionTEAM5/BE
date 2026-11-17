package com.likelion14th._Team.domain.user.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.likelion14th._Team.domain.auth.dto.SignupRequest.LanguageScoreRequest;
import com.likelion14th._Team.domain.user.dto.MyInfoResponse;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateRequest;
import com.likelion14th._Team.domain.user.dto.MyInfoUpdateResponse;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.entity.UserLanguageScore;
import com.likelion14th._Team.domain.user.entity.UserProfile;
import com.likelion14th._Team.domain.user.repository.ConsentRepository;
import com.likelion14th._Team.domain.user.repository.UserLanguageScoreRepository;
import com.likelion14th._Team.domain.user.repository.UserProfileRepository;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.enums.ConsentType;
import com.likelion14th._Team.global.enums.LanguageTestType;
import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.dao.DataIntegrityViolationException;

class MyInfoServiceTests {

    private static final Long USER_ID = 1L;

    private final UserRepository userRepository = mock(UserRepository.class);
    private final UserProfileRepository userProfileRepository = mock(UserProfileRepository.class);
    private final UserLanguageScoreRepository scoreRepository = mock(UserLanguageScoreRepository.class);
    private final ConsentRepository consentRepository = mock(ConsentRepository.class);
    private final MyInfoService service =
            new MyInfoService(userRepository, userProfileRepository, scoreRepository, consentRepository);

    private final User user = mock(User.class);
    private final UserProfile profile = mock(UserProfile.class);

    @BeforeEach
    void setUp() {
        when(user.getId()).thenReturn(USER_ID);
        when(user.getSchoolEmail()).thenReturn("chaewon@hufs.ac.kr");
        when(user.getNickname()).thenReturn("채원");
        when(user.getMajorType()).thenReturn(MajorType.PRIMARY);
        when(profile.getGpa()).thenReturn(new BigDecimal("4.12"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.of(user));
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.of(profile));
        when(scoreRepository.findByUserIdOrderByIdAsc(USER_ID)).thenReturn(List.of());
    }

    private UserLanguageScore score(LanguageTestType type, String value) {
        return UserLanguageScore.builder().user(user).testType(type).score(new BigDecimal(value)).build();
    }

    private static MyInfoUpdateRequest request(String nickname, MajorType majorType, String gpa,
                                               List<LanguageScoreRequest> scores) {
        return new MyInfoUpdateRequest(nickname, majorType, gpa == null ? null : new BigDecimal(gpa), scores);
    }

    private static CustomException errorOf(org.junit.jupiter.api.function.Executable executable) {
        return assertThrows(CustomException.class, executable);
    }

    // ===== 조회 =====

    @Test
    void getReturnsProfileScoresInEnumOrderAndConsent() {
        when(scoreRepository.findByUserIdOrderByIdAsc(USER_ID))
                .thenReturn(List.of(score(LanguageTestType.IELTS, "7.0"), score(LanguageTestType.TOEFL_IBT, "100.0")));
        when(consentRepository.existsByUserIdAndConsentType(USER_ID, ConsentType.EXTERNAL_AI)).thenReturn(true);

        MyInfoResponse response = service.getMyInfo(USER_ID);

        assertEquals(USER_ID, response.userId());
        assertEquals("chaewon@hufs.ac.kr", response.schoolEmail());
        assertEquals("채원", response.nickname());
        assertEquals(MajorType.PRIMARY, response.majorType());
        assertEquals(new BigDecimal("4.12"), response.gpa());
        assertEquals(LanguageTestType.TOEFL_IBT, response.languageScores().get(0).testType());
        assertEquals(LanguageTestType.IELTS, response.languageScores().get(1).testType());
        assertTrue(response.externalAiConsented());
    }

    @Test
    void getWithoutScoresReturnsEmptyListAndNotConsented() {
        MyInfoResponse response = service.getMyInfo(USER_ID);

        assertTrue(response.languageScores().isEmpty());
        assertEquals(false, response.externalAiConsented());
    }

    @Test
    void getWithDeletedUserIs401() {
        when(userRepository.findById(USER_ID)).thenReturn(Optional.empty());

        assertEquals(ErrorCode.UNAUTHORIZED, errorOf(() -> service.getMyInfo(USER_ID)).getErrorCode());
    }

    // ===== 수정 =====

    @Test
    void emptyRequestChangesNothingAndKeepsLanguageScores() {
        service.update(USER_ID, request(null, null, null, null));

        verify(user, never()).changeNickname(any());
        verify(user, never()).changeMajorType(any());
        verify(profile, never()).changeGpa(any());
        verify(scoreRepository, never()).deleteAllByUserId(any());
        verify(scoreRepository, never()).saveAll(any());
    }

    @Test
    void changesOnlySentFields() {
        MyInfoUpdateResponse response = service.update(USER_ID, request(null, MajorType.DOUBLE, "4.2", null));

        verify(user).changeMajorType(MajorType.DOUBLE);
        verify(profile).changeGpa(new BigDecimal("4.20"));
        verify(user, never()).changeNickname(any());
        verify(scoreRepository, never()).deleteAllByUserId(any());
        assertEquals(USER_ID, response.userId());
    }

    @Test
    void emptyLanguageScoresDeletesAll() {
        MyInfoUpdateResponse response = service.update(USER_ID, request(null, null, null, List.of()));

        verify(scoreRepository).deleteAllByUserId(USER_ID);
        verify(scoreRepository, never()).saveAll(any());
        assertTrue(response.languageScores().isEmpty());
    }

    @Test
    void languageScoresAreReplacedAfterDelete() {
        when(scoreRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MyInfoUpdateResponse response = service.update(USER_ID, request(null, null, null,
                List.of(new LanguageScoreRequest(LanguageTestType.TOEFL_IBT, new BigDecimal("105")))));

        InOrder order = inOrder(scoreRepository);
        order.verify(scoreRepository).deleteAllByUserId(USER_ID);
        order.verify(scoreRepository).saveAll(any());
        assertEquals(1, response.languageScores().size());
        assertEquals(LanguageTestType.TOEFL_IBT, response.languageScores().get(0).testType());
        assertEquals(new BigDecimal("105.0"), response.languageScores().get(0).score());
    }

    @Test
    void nicknameOwnedByAnotherUserIs409() {
        User other = mock(User.class);
        when(other.getId()).thenReturn(2L);
        when(userRepository.findByNicknameIgnoreCase("타인")).thenReturn(Optional.of(other));

        CustomException e = errorOf(() -> service.update(USER_ID, request("타인", null, "3.0", null)));

        assertEquals(ErrorCode.NICKNAME_DUPLICATED, e.getErrorCode());
        verify(user, never()).changeNickname(any());
        verify(profile, never()).changeGpa(any());
    }

    @Test
    void ownNicknameWithDifferentCaseIsAllowed() {
        when(user.getNickname()).thenReturn("Chaewon");
        when(userRepository.findByNicknameIgnoreCase("chaewon")).thenReturn(Optional.of(user));

        service.update(USER_ID, request("chaewon", null, null, null));

        verify(user).changeNickname("chaewon");
    }

    @Test
    void sameNicknameAsCurrentSkipsDuplicateCheck() {
        service.update(USER_ID, request("채원", null, null, null));

        verify(userRepository, never()).findByNicknameIgnoreCase(any());
        verify(user, never()).changeNickname(any());
    }

    @Test
    void uniqueViolationFromConcurrentRequestIs409() {
        when(userRepository.findByNicknameIgnoreCase("새닉네임")).thenReturn(Optional.empty());
        doThrow(new DataIntegrityViolationException("duplicate")).when(userRepository).flush();

        CustomException e = errorOf(() -> service.update(USER_ID, request("새닉네임", null, null, null)));

        assertEquals(ErrorCode.NICKNAME_DUPLICATED, e.getErrorCode());
    }

    @Test
    void updateWithDeletedUserIs401() {
        when(userRepository.findByIdForUpdate(USER_ID)).thenReturn(Optional.empty());

        assertEquals(ErrorCode.UNAUTHORIZED,
                errorOf(() -> service.update(USER_ID, request(null, null, null, null))).getErrorCode());
    }

    @Test
    void unexpectedFailureIs500WithApiMessage() {
        when(userProfileRepository.findByUserId(USER_ID)).thenReturn(Optional.empty());

        CustomException e = errorOf(() -> service.update(USER_ID, request(null, null, null, null)));

        assertEquals(ErrorCode.INTERNAL_ERROR, e.getErrorCode());
        assertEquals("내 정보 수정 중 오류가 발생했습니다", e.getMessage());
    }
}