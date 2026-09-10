package com.igarciamen.projects.repository;

import com.igarciamen.projects.enums.ProjectStatus;
import com.igarciamen.projects.model.Project;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;

// Combines the optional public-listing filters (sector, country, amount
// range) with a Specification, same pattern as ecommerce/products'
// ProductRepository. Every filter is optional: a null parameter simply
// contributes no predicate.
public final class ProjectSpecifications {

    private ProjectSpecifications() {}

    public static Specification<Project> publicFilters(Long sectorId, String country,
                                                        BigDecimal minAmount, BigDecimal maxAmount) {
        return (root, query, cb) -> {
            var predicates = cb.conjunction();

            predicates = cb.and(predicates, cb.equal(root.get("status"), ProjectStatus.APPROVED));

            if (sectorId != null) {
                predicates = cb.and(predicates, cb.equal(root.get("sectorId"), sectorId));
            }
            if (country != null && !country.isBlank()) {
                predicates = cb.and(predicates, cb.equal(cb.lower(root.get("country")), country.toLowerCase()));
            }
            if (minAmount != null) {
                predicates = cb.and(predicates, cb.greaterThanOrEqualTo(root.get("requestedAmount"), minAmount));
            }
            if (maxAmount != null) {
                predicates = cb.and(predicates, cb.lessThanOrEqualTo(root.get("requestedAmount"), maxAmount));
            }

            return predicates;
        };
    }
}
