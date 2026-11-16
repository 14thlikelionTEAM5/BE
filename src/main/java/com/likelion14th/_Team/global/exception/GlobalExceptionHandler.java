package com.likelion14th._Team.global.exception;

import com.likelion14th._Team.global.response.ApiResponse;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import jakarta.validation.ConstraintViolationException;
import jakarta.servlet.http.HttpServletRequest;
import com.likelion14th._Team.global.logging.ExceptionLogSanitizer;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerMapping;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 서비스에서 직접 던진 에러 */
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ApiResponse<Void>> handleCustom(CustomException e) {
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), e.getMessage(), e.getErrors()));
    }

    /** @Valid 검증 실패 (RequestBody) -> errors에 필드별 사유를 담는다 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        List<FieldErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return invalidInput(errors);
    }

    /** @Validated 검증 실패 (RequestParam, PathVariable) */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiResponse<Void>> handleConstraintViolation(ConstraintViolationException e) {
        List<FieldErrorDetail> errors = e.getConstraintViolations().stream()
                .map(v -> new FieldErrorDetail(v.getPropertyPath().toString(), v.getMessage()))
                .toList();
        return invalidInput(errors);
    }

    /** JSON 형식 오류, enum 값 오류 등 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotReadable(HttpMessageNotReadableException e) {
        return invalidInput(null);
    }

    @ExceptionHandler({MissingServletRequestParameterException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ApiResponse<Void>> handleBadParameter(Exception e) {
        return invalidInput(null);
    }

    /** 그 외 모든 예외 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleException(Exception e, HttpServletRequest request) {
        // 존재하지 않는 URL(404), 허용되지 않는 메서드(405) 등 스프링이 상태 코드를 이미 정해둔 예외는 500으로 바꾸지 않는다
        if (e instanceof ErrorResponse errorResponse) {
            int status = errorResponse.getStatusCode().value();
            return ResponseEntity.status(errorResponse.getStatusCode())
                    .body(ApiResponse.error("E" + status, "요청을 처리할 수 없습니다", null));
        }
        Object handler = request.getAttribute(HandlerMapping.BEST_MATCHING_HANDLER_ATTRIBUTE);
        if (e instanceof RuntimeException && handler instanceof HandlerMethod method) {
            ApiFailure failure = method.getMethodAnnotation(ApiFailure.class);
            if (failure != null) {
                LoggerFactory.getLogger(method.getBeanType())
                        .error(failure.logMessage(), ExceptionLogSanitizer.withoutMessages(e));
                ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
                return ResponseEntity.status(errorCode.getStatus())
                        .body(ApiResponse.error(errorCode.getCode(), failure.message(), null));
            }
        }
        log.error("처리되지 않은 예외", ExceptionLogSanitizer.withoutMessages(e));
        ErrorCode errorCode = ErrorCode.INTERNAL_ERROR;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), null));
    }

    private ResponseEntity<ApiResponse<Void>> invalidInput(List<FieldErrorDetail> errors) {
        ErrorCode errorCode = ErrorCode.INVALID_INPUT;
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), errorCode.getMessage(), errors));
    }
}