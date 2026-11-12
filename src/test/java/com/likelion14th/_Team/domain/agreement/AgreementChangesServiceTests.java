package com.likelion14th._Team.domain.agreement;

import com.likelion14th._Team.domain.agreement.repository.AgreementChangesRepository;
import com.likelion14th._Team.domain.agreement.service.AgreementChangesService;
import com.likelion14th._Team.domain.university.entity.*;
import com.likelion14th._Team.global.exception.*;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AgreementChangesServiceTests {
    AgreementChangesRepository repo=mock(AgreementChangesRepository.class);
    AgreementChangesService service=new AgreementChangesService(repo);
    Agreement row(long id,String semester,boolean open,Integer quota,String date) {
        University university=mock(University.class);when(university.getId()).thenReturn(id);
        return Agreement.builder().university(university).semester(semester).isOpen(open).quota(quota)
            .announcedAt(date==null?null:LocalDate.parse(date)).build();
    }
    void data(List<String> semesters,List<Agreement> rows) {
        when(repo.latestSemesters(any())).thenReturn(semesters);
        when(repo.forSemesters(semesters)).thenReturn(rows);
    }
    @Test void noDataIs404() {
        data(List.of(),List.of());
        assertEquals(ErrorCode.AGREEMENT_NOT_FOUND,assertThrows(CustomException.class,service::changes).getErrorCode());
        verify(repo,never()).forSemesters(any());
    }
    @Test void firstSemesterCountsAreNullAndDateIncludesClosedAnnouncement() {
        data(List.of("2027-1"),List.of(row(1,"2027-1",true,2,"2026-07-01"),row(2,"2027-1",false,null,"2026-07-03")));
        var result=service.changes();
        assertNull(result.previousSemester());assertNull(result.newCount());assertNull(result.quotaChangedCount());assertNull(result.closedCount());
        assertEquals(LocalDate.parse("2026-07-03"),result.standardDate());
    }
    @Test void comparesBySchoolAndExcludesReopeningAndUnknownQuota() {
        var fresh=row(1,"2027-1",true,2,"2026-07-03");
        data(List.of("2027-1","2026-1"),List.of(
            fresh,fresh, // 같은 학교는 한 번만 집계
            row(2,"2026-1",true,2,null),row(2,"2027-1",true,3,null),
            row(3,"2026-1",true,2,null), // 최신 공고 없음: 종료
            row(4,"2026-1",true,2,null),row(4,"2027-1",false,0,null),
            row(5,"2026-1",false,0,null),row(5,"2027-1",true,3,null), // 재개는 신규 아님
            row(6,"2026-1",true,null,null),row(6,"2027-1",true,3,null),
            row(7,"2026-1",true,3,null),row(7,"2027-1",true,null,null),
            row(8,"2027-1",false,0,null), // 모집 중단 상태로 처음 등장: 신규 아님
            row(9,"2026-1",false,0,null)));
        var result=service.changes();
        assertEquals("2026-1",result.previousSemester());
        assertEquals(1L,result.newCount());assertEquals(1L,result.quotaChangedCount());assertEquals(2L,result.closedCount());
    }
    @Test void unchangedSchoolsHaveZeroCountsAndMissingDatesAreNull() {
        data(List.of("2027-1","2026-2"),List.of(row(1,"2027-1",true,0,null),row(1,"2026-2",true,0,null)));
        var result=service.changes();
        assertEquals(0L,result.newCount());assertEquals(0L,result.quotaChangedCount());assertEquals(0L,result.closedCount());assertNull(result.standardDate());
    }
}
