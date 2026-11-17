package com.likelion14th._Team.domain.admission.dto;

import com.likelion14th._Team.global.enums.MajorType;
import com.likelion14th._Team.global.exception.*;
import com.likelion14th._Team.global.response.FieldErrorDetail;
import java.util.List;

public record AdmissionReportFilter(long universityId, String semester, MajorType majorType) {
    public static AdmissionReportFilter from(String id, String semester, String majorType) {
        long parsed;
        try {
            if (id == null || !id.matches("[0-9]+")) throw new NumberFormatException();
            parsed = Long.parseLong(id);
            if (parsed <= 0) throw new NumberFormatException();
        } catch (NumberFormatException exception) {
            throw invalid("입력값이 올바르지 않습니다", "universityId", "양의 정수를 입력해 주세요.");
        }
        if (semester != null && !semester.matches("[0-9]{4}-[12]")) {
            throw invalid("학기 형식이 올바르지 않습니다 (예: 2026-1)", "semester", "YYYY-1 또는 YYYY-2 형식으로 입력해 주세요.");
        }
        MajorType parsedMajor = null;
        if (majorType != null) {
            try { parsedMajor = MajorType.valueOf(majorType); }
            catch (IllegalArgumentException exception) {
                throw invalid("전공 구분 값이 올바르지 않습니다", "majorType", "PRIMARY, DOUBLE, MINOR 중 하나를 입력해 주세요.");
            }
        }
        return new AdmissionReportFilter(parsed, semester, parsedMajor);
    }
    private static CustomException invalid(String message, String field, String reason) {
        return new CustomException(ErrorCode.INVALID_INPUT, message, List.of(new FieldErrorDetail(field, reason)));
    }
}
