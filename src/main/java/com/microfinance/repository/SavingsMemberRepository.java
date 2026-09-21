package com.microfinance.repository;

import com.microfinance.entity.SavingsMember;
import com.microfinance.entity.enums.MemberStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SavingsMemberRepository extends JpaRepository<SavingsMember, Long> {

    Optional<SavingsMember> findByMemberCode(String memberCode);

    boolean existsByMemberCode(String memberCode);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    long count();

    /**
     * Search by name, phone, member code, or place (case-insensitive, partial
     * match) and optionally filter by status. Pass null for status to ignore.
     */
    @Query("""
            SELECT m FROM SavingsMember m
            WHERE (:status IS NULL OR m.status = :status)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR m.phoneNumber LIKE CONCAT('%', :keyword, '%')
                OR LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.place) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<SavingsMember> search(@Param("keyword") String keyword,
                                @Param("status") MemberStatus status,
                                Pageable pageable);
}
