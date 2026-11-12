package com.likelion14th._Team.domain.user;

import com.likelion14th._Team.domain.user.controller.NicknameCheckController;
import com.likelion14th._Team.domain.user.service.*;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.global.security.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.system.*;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.Optional;
import java.util.UUID;
import io.jsonwebtoken.JwtException;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers=NicknameCheckController.class, properties="app.cors.allowed-origins=http://localhost:3000")
@Import({SecurityConfig.class, NicknameCheckService.class, NicknameCheckRateLimiter.class})
@ExtendWith(OutputCaptureExtension.class)
class NicknameCheckTests {
    @Autowired MockMvc mvc;
    @MockitoBean UserRepository users;
    @MockitoBean JwtProvider jwt;
    String ip;
    @BeforeEach void setup() { ip=UUID.randomUUID().toString(); }
    org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request(String nickname) {
        return get("/api/v1/users/nickname/check").param("nickname",nickname)
                .with(r -> {r.setRemoteAddr(ip);return r;});
    }
    @Test void availableAndNfc() throws Exception {
        mvc.perform(request("채원")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(true))
            .andExpect(jsonPath("$.data.isCurrent").value(false))
            .andExpect(jsonPath("$.timestamp").value(org.hamcrest.Matchers.endsWith("+09:00")));
        verify(users).findByNicknameIgnoreCase("채원");
    }
    @Test void duplicateAndCurrentAndInvalidToken() throws Exception {
        User user=mock(User.class); when(user.getId()).thenReturn(7L);
        when(users.findByNicknameIgnoreCase("Chaewon")).thenReturn(Optional.of(user));
        mvc.perform(request("Chaewon")).andExpect(status().isOk())
            .andExpect(jsonPath("$.data.available").value(false));
        when(jwt.getUserId("valid")).thenReturn(7L);
        mvc.perform(request("Chaewon").header("Authorization","Bearer valid"))
            .andExpect(jsonPath("$.data.available").value(true)).andExpect(jsonPath("$.data.isCurrent").value(true));
        when(jwt.getUserId("invalid")).thenThrow(new JwtException("invalid"));
        mvc.perform(request("Chaewon").header("Authorization","Bearer invalid"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.data.isCurrent").value(false));
    }
    @Test void invalidInputsHaveFieldErrors() throws Exception {
        for(String value:new String[]{"", "a", "abcdefghijklm", "ㅋㅋ", "채 원", "name!", "😀😀"}) {
            mvc.perform(request(value)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("E400"))
                .andExpect(jsonPath("$.errors[0].field").value("nickname"));
        }
        mvc.perform(get("/api/v1/users/nickname/check").with(r->{r.setRemoteAddr(ip);return r;}))
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors[0].field").value("nickname"));
    }
    @Test void thirtyRequestsThen429EvenWithForgedForwardedIp() throws Exception {
        for(int i=0;i<30;i++) mvc.perform(request("사용가능")).andExpect(status().isOk());
        mvc.perform(request("사용가능").header("X-Forwarded-For","new-ip"))
            .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("E429"));
        ip=UUID.randomUUID().toString();
        mvc.perform(request("사용가능")).andExpect(status().isOk());
    }
    @Test void serverFailureDoesNotLogSecrets(CapturedOutput output) throws Exception {
        String secret=UUID.randomUUID().toString();
        when(jwt.getUserId(secret)).thenReturn(7L);
        when(users.findByNicknameIgnoreCase("오류확인")).thenThrow(new IllegalStateException(secret));
        mvc.perform(request("오류확인").header("Authorization","Bearer "+secret)
            .param("password",secret).param("authCode",secret))
            .andExpect(status().isInternalServerError()).andExpect(jsonPath("$.message").value("닉네임 확인 중 오류가 발생했습니다"));
        assertFalse(output.getAll().contains(secret));
    }
}
