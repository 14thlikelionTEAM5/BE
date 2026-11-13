package com.likelion14th._Team.domain.agreement.service;

import com.likelion14th._Team.domain.agreement.dto.AgreementChangesResponse;
import com.likelion14th._Team.domain.agreement.repository.AgreementChangesRepository;
import com.likelion14th._Team.domain.university.entity.Agreement;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AgreementChangesService {
    private final AgreementChangesRepository repository;

    @Transactional(readOnly = true)
    public AgreementChangesResponse changes() {
        // YYYY-1 / YYYY-2 형식이므로 문자열 내림차순과 학기 최신순이 같다.
        List<String> semesters = repository.latestSemesters(PageRequest.of(0, 2));
        if (semesters.isEmpty()) throw new CustomException(ErrorCode.AGREEMENT_NOT_FOUND);
        String currentSemester = semesters.getFirst();
        String previousSemester = semesters.size() > 1 ? semesters.get(1) : null;
        Map<Long, Agreement> current = new HashMap<>();
        Map<Long, Agreement> previous = new HashMap<>();
        LocalDate standardDate = null;
        for (Agreement agreement : repository.forSemesters(semesters)) {
            if (currentSemester.equals(agreement.getSemester())) {
                current.put(agreement.getUniversity().getId(), agreement);
                LocalDate date = agreement.getAnnouncedAt();
                if (date != null && (standardDate == null || date.isAfter(standardDate))) standardDate = date;
            } else {
                previous.put(agreement.getUniversity().getId(), agreement);
            }
        }
        if (previousSemester == null) {
            return new AgreementChangesResponse(currentSemester, null, standardDate, null, null, null);
        }
        long added = 0, changed = 0, closed = 0;
        for (var entry : current.entrySet()) {
            Agreement now = entry.getValue();
            Agreement before = previous.get(entry.getKey());
            if (!now.isOpen()) continue;
            if (before == null) added++;
            else if (before.isOpen() && before.getQuota() != null && now.getQuota() != null
                    && !before.getQuota().equals(now.getQuota())) changed++;
        }
        for (var entry : previous.entrySet()) {
            Agreement now = current.get(entry.getKey());
            if (entry.getValue().isOpen() && (now == null || !now.isOpen())) closed++;
        }
        return new AgreementChangesResponse(currentSemester, previousSemester, standardDate, added, changed, closed);
    }
}
