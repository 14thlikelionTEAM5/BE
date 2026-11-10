package com.likelion14th._Team.domain.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** 인증 코드 메일 발송. 실패하면 MailException(런타임 예외)이 발생한다 */
@Component
public class VerificationMailSender {

    private final JavaMailSender mailSender;
    private final String from;

    public VerificationMailSender(JavaMailSender mailSender,
                                  @Value("${spring.mail.username}") String from) {
        this.mailSender = mailSender;
        this.from = from;
    }

    public void send(String to, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject("[PassHUFS] 학교 메일 인증 코드");
        message.setText("인증 코드: " + code + "\n\n"
                + "5분 안에 입력해 주세요.\n"
                + "본인이 요청하지 않았다면 이 메일을 무시해 주세요.");
        mailSender.send(message);
    }
}