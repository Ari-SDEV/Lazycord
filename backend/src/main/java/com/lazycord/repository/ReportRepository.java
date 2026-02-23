package com.lazycord.repository;

import com.lazycord.model.Report;
import com.lazycord.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ReportRepository extends JpaRepository<Report, UUID> {

    Page<Report> findByStatusOrderByCreatedAtDesc(Report.ReportStatus status, Pageable pageable);

    List<Report> findByReporterOrderByCreatedAtDesc(User reporter);

    List<Report> findByReportedUserOrderByCreatedAtDesc(User reportedUser);

    long countByStatus(Report.ReportStatus status);

    List<Report> findByStatusInOrderByCreatedAtDesc(List<Report.ReportStatus> statuses);
}
