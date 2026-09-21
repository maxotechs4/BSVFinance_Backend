package com.microfinance.repository;

import com.microfinance.entity.StaffMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StaffMemberRepository extends JpaRepository<StaffMember, Long> {

    java.util.List<StaffMember> findAllByOrderByNameAsc();
}
