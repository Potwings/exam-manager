package com.exammanager.service;

import com.exammanager.entity.Examinee;
import com.exammanager.repository.ExamineeRepository;
import com.exammanager.repository.SubmissionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExamineeServiceTest {

    @Mock
    private ExamineeRepository examineeRepository;

    @Mock
    private SubmissionRepository submissionRepository;

    @InjectMocks
    private ExamineeService examineeService;

    private static final String NAME = "홍길동";
    private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);

    /**
     * 기존 수험자가 활성 시험에 제출 기록이 없으면 정상 로그인되어야 한다.
     */
    @Test
    void loginOrCreate_existingExamineeNoSubmission_returnsExaminee() {
        // given
        Examinee existing = Examinee.builder().id(1L).name(NAME).birthDate(BIRTH_DATE).build();
        when(examineeRepository.findByNameAndBirthDate(NAME, BIRTH_DATE))
                .thenReturn(Optional.of(existing));
        when(submissionRepository.countByExamineeIdAndActiveExam(1L))
                .thenReturn(0L);

        // when
        Examinee result = examineeService.loginOrCreate(NAME, BIRTH_DATE);

        // then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo(NAME);
        verify(examineeRepository, never()).save(any());
    }

    /**
     * 신규 수험자는 새로 생성되어야 한다.
     * 제출 기록 검증(submissionRepository)은 호출되지 않아야 한다.
     */
    @Test
    void loginOrCreate_newExaminee_createsAndReturns() {
        // given
        when(examineeRepository.findByNameAndBirthDate(NAME, BIRTH_DATE))
                .thenReturn(Optional.empty());
        Examinee saved = Examinee.builder().id(2L).name(NAME).birthDate(BIRTH_DATE).build();
        when(examineeRepository.save(any(Examinee.class))).thenReturn(saved);

        // when
        Examinee result = examineeService.loginOrCreate(NAME, BIRTH_DATE);

        // then
        assertThat(result.getId()).isEqualTo(2L);
        verify(examineeRepository).save(any(Examinee.class));
        // 신규 수험자는 제출 기록 검증을 하지 않아야 한다
        verify(submissionRepository, never())
                .countByExamineeIdAndActiveExam(anyLong());
    }

    /**
     * 동시 요청으로 save가 DataIntegrityViolationException을 던지면
     * 재조회하여 기존 레코드를 반환해야 한다.
     */
    @Test
    void loginOrCreate_concurrentInsert_retriesAndReturns() {
        // given
        when(examineeRepository.findByNameAndBirthDate(NAME, BIRTH_DATE))
                .thenReturn(Optional.empty())   // 첫 조회: 없음
                .thenReturn(Optional.of(        // 재조회: 다른 스레드가 생성한 레코드
                        Examinee.builder().id(3L).name(NAME).birthDate(BIRTH_DATE).build()));
        when(examineeRepository.save(any(Examinee.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // when
        Examinee result = examineeService.loginOrCreate(NAME, BIRTH_DATE);

        // then
        assertThat(result.getId()).isEqualTo(3L);
        verify(examineeRepository).save(any(Examinee.class));
        verify(examineeRepository, times(2)).findByNameAndBirthDate(NAME, BIRTH_DATE);
    }

    /**
     * 기존 수험자가 활성 시험에 제출 기록이 있으면 409 CONFLICT가 발생해야 한다.
     */
    @Test
    void loginOrCreate_existingExamineeWithSubmission_throwsConflict() {
        // given
        Examinee existing = Examinee.builder().id(1L).name(NAME).birthDate(BIRTH_DATE).build();
        when(examineeRepository.findByNameAndBirthDate(NAME, BIRTH_DATE))
                .thenReturn(Optional.of(existing));
        when(submissionRepository.countByExamineeIdAndActiveExam(1L))
                .thenReturn(1L);

        // when & then
        assertThatThrownBy(() -> examineeService.loginOrCreate(NAME, BIRTH_DATE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(409);
                });
        verify(examineeRepository, never()).save(any());
    }

    /**
     * 동시성 충돌 후 재조회에서도 찾지 못하면 500 INTERNAL_SERVER_ERROR가 발생해야 한다.
     */
    @Test
    void loginOrCreate_concurrentInsertAndRetryFails_throwsInternalError() {
        // given
        when(examineeRepository.findByNameAndBirthDate(NAME, BIRTH_DATE))
                .thenReturn(Optional.empty())   // 첫 조회: 없음
                .thenReturn(Optional.empty());  // 재조회: 여전히 없음 (비정상)
        when(examineeRepository.save(any(Examinee.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate entry"));

        // when & then
        assertThatThrownBy(() -> examineeService.loginOrCreate(NAME, BIRTH_DATE))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode().value()).isEqualTo(500);
                });
    }
}
