package com.microfinance.repository;

import com.microfinance.dto.response.NomineeImageMetaResponse;
import com.microfinance.entity.NomineeImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NomineeImageRepository extends JpaRepository<NomineeImage, Long> {

    /** Member profile "View Image" gallery: newest first, metadata only (no image bytes travel here). */
    @Query("SELECT new com.microfinance.dto.response.NomineeImageMetaResponse(n.id, n.fileName, n.contentType, n.uploadedAt) " +
            "FROM NomineeImage n WHERE n.member.id = :memberId ORDER BY n.uploadedAt DESC")
    List<NomineeImageMetaResponse> findMetaByMemberId(@Param("memberId") Long memberId);

    /** Scoped to the member so an image can't be fetched/downloaded via a mismatched member id in the URL. */
    Optional<NomineeImage> findByIdAndMemberId(Long id, Long memberId);

    long countByMemberId(Long memberId);
}
