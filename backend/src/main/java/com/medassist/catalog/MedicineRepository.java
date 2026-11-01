package com.medassist.catalog;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface MedicineRepository extends JpaRepository<Medicine, UUID> {

    /**
     * Fuzzy search on brand name or salt, ranked by trigram similarity (pg_trgm), so
     * "paracetmol" still finds Paracetamol and "dolo" finds Dolo 650.
     */
    @Query(value = """
            select * from medicines m
            where lower(m.name) like lower(concat('%', :q, '%'))
               or lower(m.salt_composition) like lower(concat('%', :q, '%'))
               or similarity(lower(m.name), lower(:q)) > 0.3
               or similarity(lower(m.salt_composition), lower(:q)) > 0.3
            order by greatest(similarity(lower(m.name), lower(:q)), similarity(lower(m.salt_composition), lower(:q))) desc,
                     m.name
            """,
            countQuery = """
            select count(*) from medicines m
            where lower(m.name) like lower(concat('%', :q, '%'))
               or lower(m.salt_composition) like lower(concat('%', :q, '%'))
               or similarity(lower(m.name), lower(:q)) > 0.3
               or similarity(lower(m.salt_composition), lower(:q)) > 0.3
            """,
            nativeQuery = true)
    Page<Medicine> search(@Param("q") String q, Pageable pageable);

    /** Same salt composition, different brand: the generic-substitute finder. */
    @Query("select m from Medicine m where lower(m.saltComposition) = lower(:salt) and m.id <> :excludeId order by m.mrp asc nulls last")
    List<Medicine> findSubstitutes(String salt, UUID excludeId);
}
