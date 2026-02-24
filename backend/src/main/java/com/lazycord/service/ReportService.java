package com.lazycord.service;

import com.lazycord.model.*;
import com.lazycord.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    private final ReportRepository reportRepository;

    @Transactional
    public Report createReport(User reporter, User reportedUser, Channel channel, Message message,
                             Report.ReportReason reason, String details) {
        Report report = new Report();
        report.setReporter(reporter);
        report.setReportedUser(reportedUser);
        report.setChannel(channel);
        report.setMessage(message);
        report.setReason(reason);
        report.setDetails(details);
        report.setStatus(Report.ReportStatus.PENDING);

        Report saved = reportRepository.save(report);
        log.info("Report created by {} against {}", reporter.getUsername(), reportedUser.getUsername());
        return saved;
    }

    @Transactional(readOnly = true)
    public Page<Report> getPendingReports(Pageable pageable) {
        return reportRepository.findByStatusOrderByCreatedAtDesc(Report.ReportStatus.PENDING, pageable);
    }

    @Transactional(readOnly = true)
    public List<Report> getUserReports(User reporter) {
        return reportRepository.findByReporterOrderByCreatedAtDesc(reporter);
    }

    @Transactional(readOnly = true)
    public long getPendingReportCount() {
        return reportRepository.countByStatus(Report.ReportStatus.PENDING);
    }
}
