package com.likelion14th._Team.domain.bookmark;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.likelion14th._Team.domain.bookmark.repository.BookmarkRepository;
import com.likelion14th._Team.domain.bookmark.service.BookmarkService;
import com.likelion14th._Team.domain.university.entity.University;
import com.likelion14th._Team.domain.university.repository.UniversityRepository;
import com.likelion14th._Team.domain.user.entity.User;
import com.likelion14th._Team.domain.user.repository.UserRepository;
import com.likelion14th._Team.global.exception.CustomException;
import com.likelion14th._Team.global.exception.ErrorCode;
import java.sql.SQLException;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class BookmarkServiceTests {
    @Test
    void duplicateConstraintIs409ButForeignKeyViolationIsNot() {
        BookmarkRepository bookmarks = mock(BookmarkRepository.class);
        UniversityRepository schools = mock(UniversityRepository.class);
        UserRepository users = mock(UserRepository.class);
        BookmarkService service = new BookmarkService(bookmarks, schools, users,
                new com.likelion14th._Team.domain.university.service.UniversityCardAssembler(
                        mock(com.likelion14th._Team.domain.university.repository.UniversityQueryRepository.class),
                        new com.likelion14th._Team.domain.university.service.UniversityEligibilityCalculator()));
        when(schools.findById(4L)).thenReturn(Optional.of(mock(University.class)));
        when(users.getReferenceById(1L)).thenReturn(mock(User.class));
        when(bookmarks.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException(
                "duplicate", new SQLException("duplicate", "23000", 1062)));
        CustomException duplicate = assertThrows(CustomException.class, () -> service.create(1L, 4L));
        assertEquals(ErrorCode.BOOKMARK_DUPLICATED, duplicate.getErrorCode());
        DataIntegrityViolationException foreignKey = new DataIntegrityViolationException(
                "foreign key", new SQLException("foreign key", "23000", 1452));
        doThrow(foreignKey).when(bookmarks).saveAndFlush(any());
        assertSame(foreignKey, assertThrows(DataIntegrityViolationException.class,
                () -> service.create(1L, 4L)));
    }
    @Test
    void emptyBookmarksReturnNullSemesterWhenThereAreNoAgreements() {
        var bookmarks = mock(BookmarkRepository.class);
        var queries = mock(com.likelion14th._Team.domain.university.repository.UniversityQueryRepository.class);
        var service = new BookmarkService(bookmarks, mock(UniversityRepository.class), mock(UserRepository.class),
                new com.likelion14th._Team.domain.university.service.UniversityCardAssembler(queries,
                        new com.likelion14th._Team.domain.university.service.UniversityEligibilityCalculator()));
        when(queries.semesters()).thenReturn(java.util.List.of());
        when(bookmarks.findAllForUser(1L)).thenReturn(java.util.List.of());
        var response = service.list(1L);
        assertNull(response.semester());
        assertEquals(0, response.totalCount());
        assertTrue(response.universities().isEmpty());
        verify(queries, never()).applicant(any());
        verify(queries, never()).agreementsForSchools(any(), any());
    }

}
