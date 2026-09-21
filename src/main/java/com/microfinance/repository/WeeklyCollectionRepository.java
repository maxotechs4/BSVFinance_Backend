package com.microfinance.repository;

import com.microfinance.entity.WeeklyCollection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WeeklyCollectionRepository extends JpaRepository<WeeklyCollection, Long> {

    Optional<WeeklyCollection> findByWeekNumberAndCollectionYear(Integer weekNumber, Integer collectionYear);

    List<WeeklyCollection> findByCollectionYearOrderByWeekNumberAsc(Integer collectionYear);

    List<WeeklyCollection> findTop8ByOrderByCollectionYearDescWeekNumberDesc();
}
