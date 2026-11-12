package com.likelion14th._Team.domain.returnreport.service;

import com.likelion14th._Team.domain.returnreport.ai.AiProperties;
import com.likelion14th._Team.domain.returnreport.ai.ExtractReturnReportPrompt;
import com.likelion14th._Team.domain.returnreport.ai.LlmClient;
import com.likelion14th._Team.domain.returnreport.ai.LlmException;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractRequest;
import com.likelion14th._Team.domain.returnreport.dto.ReturnReportExtractResponse;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.time.Duration;
import java.util.function.LongSupplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 귀국보고서 항목 추출. 결과만 반환하고 저장하지 않는다.
 * - 외부 AI 처리 동의 여부는 호출 전에 컨트롤러가 ConsentService로 확인한다
 * - AI 호출 중 DB 커넥션을 잡지 않도록 @Transactional을 쓰지 않는다
 * - 원문·AI 응답 원문은 DB·로그·예외 메시지에 남기지 않는다
 * - 실패는 종류와 관계없이 E500 하나로 응답하고, 실패 종류·상태코드는 로그에만 남긴다
 */
@Slf4j
@Service
public class ReturnReportExtractService {

    static final String FAILURE_MESSAGE = "항목 추출에 실패했습니다. 다시 시도하거나 직접 입력해 주세요.";

    private final LlmClient llmClient;
    private final ExtractReturnReportPrompt prompt;
    private final ExtractionPostProcessor postProcessor = new ExtractionPostProcessor();
    private final Duration attemptTimeout;
    private final Duration totalTimeout;
    private final LongSupplier nanoClock;

    @Autowired
    public ReturnReportExtractService(LlmClient llmClient, ExtractReturnReportPrompt prompt, AiProperties properties) {
        this(llmClient, prompt, Duration.ofSeconds(properties.gemini().timeoutSeconds()),
                Duration.ofSeconds(properties.gemini().totalTimeoutSeconds()), System::nanoTime);
    }

    ReturnReportExtractService(LlmClient llmClient, ExtractReturnReportPrompt prompt,
                               Duration attemptTimeout, Duration totalTimeout, LongSupplier nanoClock) {
        this.llmClient = llmClient;
        this.prompt = prompt;
        this.attemptTimeout = attemptTimeout;
        this.totalTimeout = totalTimeout;
        this.nanoClock = nanoClock;
    }

    public ReturnReportExtractResponse extract(ReturnReportExtractRequest request) {
        String text = ExtractReturnReportPrompt.neutralize(PhoneNumberMasker.mask(request.text()));
        String userMessage = ExtractReturnReportPrompt.userMessage(text);
        String source = llmClient.verifiesSource() ? text : null;

        long started = nanoClock.getAsLong();
        int attempt = 0;
        while (true) {
            attempt++;
            try {
                String json = llmClient.generateJson(prompt.systemInstruction(), userMessage,
                        ExtractReturnReportPrompt.responseSchema());
                ExtractionPostProcessor.Result result = postProcessor.process(json, source);
                log.info("귀국보고서 항목 추출 성공 client={} prompt={} attempts={} latencyMs={} droppedFields={}",
                        llmClient.describe(), prompt.version(), attempt, elapsedMillis(started),
                        result.droppedFields());
                return result.response();
            } catch (LlmException e) {
                if (e.getKind().isRetryable() && attempt == 1 && canRetry(started)) {
                    log.warn("귀국보고서 항목 추출 재시도 kind={} status={}", e.getKind(), e.getHttpStatus());
                    continue;
                }
                logFailure(e, attempt, started);
                throw new CustomException(ErrorCode.INTERNAL_ERROR, FAILURE_MESSAGE);
            } catch (RuntimeException e) {
                // 예외 메시지에 원문이 섞일 수 있으므로 클래스명만 남긴다
                log.error("귀국보고서 항목 추출 실패 kind=UNEXPECTED exception={} attempts={} latencyMs={}",
                        e.getClass().getName(), attempt, elapsedMillis(started));
                throw new CustomException(ErrorCode.INTERNAL_ERROR, FAILURE_MESSAGE);
            }
        }
    }

    /** 재시도해도 전체 상한(45초)을 넘지 않을 때만 재시도한다 (남은 시간 ≥ 1회 타임아웃 30초) */
    private boolean canRetry(long started) {
        Duration elapsed = Duration.ofNanos(nanoClock.getAsLong() - started);
        return elapsed.plus(attemptTimeout).compareTo(totalTimeout) <= 0;
    }

    private void logFailure(LlmException e, int attempt, long started) {
        String format = "귀국보고서 항목 추출 실패 client={} kind={} status={} attempts={} latencyMs={}";
        Object[] args = {llmClient.describe(), e.getKind(), e.getHttpStatus(), attempt, elapsedMillis(started)};
        if (e.getKind() == LlmException.Kind.MISSING_API_KEY || e.getKind() == LlmException.Kind.AUTH
                || e.getKind() == LlmException.Kind.CLIENT_ERROR) {
            log.error(format, args);
        } else {
            log.warn(format, args);
        }
    }

    private long elapsedMillis(long started) {
        return Duration.ofNanos(nanoClock.getAsLong() - started).toMillis();
    }
}
