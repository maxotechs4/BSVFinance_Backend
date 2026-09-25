package com.microfinance.repository;

import com.microfinance.entity.Member;
import com.microfinance.entity.enums.MemberStatus;
import com.microfinance.entity.enums.Weekday;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findByMemberCode(String memberCode);

    boolean existsByMemberCode(String memberCode);

    Optional<Member> findByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    /**
     * All members (any status) that share this phone number. A phone number can
     * legitimately belong to more than one member profile over time — e.g. an
     * older profile whose loan is CLOSED plus a new profile for the same
     * person's next loan — so this returns a list rather than a single Optional.
     * Used by the Create Member form to decide whether a new profile can be
     * created for this phone number (only allowed when every existing match is
     * CLOSED).
     */
    List<Member> findAllByPhoneNumber(String phoneNumber);

    /**
     * Whether an existing member with this phone number still has an
     * open (non-CLOSED) loan. Creating a new profile for a phone number is only
     * blocked when this returns true — a phone number whose only matches are
     * CLOSED is free to be reused for a new loan profile.
     */
    boolean existsByPhoneNumberAndStatusNot(String phoneNumber, MemberStatus status);

    /** All members marked as a head member, for populating the "Head Member" dropdown. */
    List<Member> findByHeadMemberTrueOrderByNameAsc();

    /** Sub-members belonging to a given head, for the expandable accordion row. */
    List<Member> findByParentHead_IdOrderByNameAsc(Long parentHeadId);

    /** All members (head or sub-member) scheduled for the given weekday, for the Admin page filter. */
    List<Member> findByWeekdayOrderByNameAsc(Weekday weekday);

    long count();

    long countByStatus(MemberStatus status);

    @Query("SELECT COALESCE(SUM(m.weeklyAmount), 0) FROM Member m WHERE m.status = :status")
    BigDecimal sumWeeklyAmountByStatus(@Param("status") MemberStatus status);

    /** Members who have paid at least one of insurance or processing (i.e. a non-zero amount is recorded). */
    @Query("SELECT COUNT(m) FROM Member m WHERE m.insuranceAmount > 0 OR m.processingAmount > 0")
    long countMembersWithInsuranceOrProcessingPaid();

    @Query("SELECT COALESCE(SUM(m.insuranceAmount), 0) FROM Member m")
    BigDecimal sumInsuranceAmount();

    @Query("SELECT COALESCE(SUM(m.processingAmount), 0) FROM Member m")
    BigDecimal sumProcessingAmount();

  
    @Query("""
            SELECT m FROM Member m
            WHERE (:status IS NULL OR m.status = :status)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR m.phoneNumber LIKE CONCAT('%', :keyword, '%')
                OR LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.centerPlace) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Member> search(@Param("keyword") String keyword,
                         @Param("status") MemberStatus status,
                         Pageable pageable);

    
    @Query("""
            SELECT DISTINCT m FROM Member m
            LEFT JOIN m.subMembers sub
            WHERE m.headMember = true
            AND (:status IS NULL OR m.status = :status)
            AND (
                :keyword IS NULL OR :keyword = ''
                OR LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR m.phoneNumber LIKE CONCAT('%', :keyword, '%')
                OR LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.centerPlace) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.groupId) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.groupName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(sub.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR sub.phoneNumber LIKE CONCAT('%', :keyword, '%')
                OR LOWER(sub.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            """)
    Page<Member> searchHeadsIncludingSubMembers(@Param("keyword") String keyword,
                                                 @Param("status") MemberStatus status,
                                                 Pageable pageable);

    /** Any member (head or sub) whose own name/phone/member code matches — used by the Collection page search. */
    @Query("""
            SELECT m FROM Member m
            LEFT JOIN FETCH m.parentHead
            WHERE LOWER(m.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR m.phoneNumber LIKE CONCAT('%', :keyword, '%')
               OR LOWER(m.memberCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
            ORDER BY m.name ASC
            """)
    List<Member> searchIndividualMembers(@Param("keyword") String keyword, Pageable pageable);

    /** Head members whose center code matches — used by the Collection page search to surface the whole group. */
        /** Head members whose group ID or group name matches — used by the Collection page search to surface the whole group. */
    @Query("""
            SELECT DISTINCT m FROM Member m
            LEFT JOIN FETCH m.subMembers
            WHERE m.headMember = true
            AND (
                LOWER(m.groupId) LIKE LOWER(CONCAT('%', :keyword, '%'))
                OR LOWER(m.groupName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            )
            ORDER BY m.name ASC
            """)
    List<Member> searchGroupsByGroupId(@Param("keyword") String keyword, Pageable pageable);
    
}