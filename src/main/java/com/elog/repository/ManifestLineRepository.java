package com.elog.repository;

import com.elog.entity.ManifestLine;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ManifestLineRepository extends JpaRepository<ManifestLine, Long> {

    List<ManifestLine> findByManifestManifestIdOrderByLifoSequenceAsc(Long manifestId);

    List<ManifestLine> findByManifestManifestIdOrderByStopSequenceNoAscLifoSequenceAsc(Long manifestId);
}
