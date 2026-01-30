package com.booking.platform.repository;

import com.booking.platform.entity.system.Feature;
import com.booking.platform.enums.FeatureCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 功能定義 Repository
 *
 * @author Developer
 * @since 1.0.0
 */
@Repository
public interface FeatureRepository extends JpaRepository<Feature, String> {

    Optional<Feature> findByCode(FeatureCode code);

    List<Feature> findByIsActiveTrueOrderBySortOrderAsc();

    @Query("""
            SELECT f FROM Feature f
            WHERE f.isActive = true
            AND (:category IS NULL OR f.category = :category)
            ORDER BY f.sortOrder ASC
            """)
    List<Feature> findByCategory(@Param("category") String category);

    @Query("""
            SELECT f FROM Feature f
            WHERE f.isActive = true
            AND f.isFree = :isFree
            ORDER BY f.sortOrder ASC
            """)
    List<Feature> findByIsFree(@Param("isFree") boolean isFree);

    boolean existsByCode(FeatureCode code);
}
