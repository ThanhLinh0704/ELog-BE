-- V42__decouple_history_events_from_fk_constraints.sql
-- Fix: PlanningHistoryServiceImpl.record()/TripOutcomeHistoryServiceImpl.record() dùng
-- @Transactional(propagation = REQUIRES_NEW) — cần thiết để không bị chặn bởi @Transactional(readOnly = true)
-- của RecommendationServiceImpl.recommendTop3() và để lỗi ghi audit log không làm rollback nghiệp vụ chính.
-- Nhưng REQUIRES_NEW mở transaction/connection độc lập, không nhìn thấy các row được tạo (nhưng chưa commit)
-- bởi transaction gọi nó -> Foreign Key tới trip_drafts/trips/trip_executions sẽ luôn vi phạm khi hook ghi log
-- ngay sau khi tạo entity đó trong cùng transaction (VD TRIP_DRAFT_CREATED, TRIP_EXECUTION_CREATED,
-- OPTION_SELECTED/DRIVER_ASSIGNED). Xác nhận lỗi thật qua TripDraftServiceImplIntegrationTest
-- (UnexpectedRollbackException tại PlanningHistoryServiceImpl.record() gọi từ consolidate()/revertToDraft()).
--
-- Bảng audit/log không nên ràng buộc FK cứng tới entity đang ghi log (pattern phổ biến) — vừa giải quyết đúng
-- lỗi transaction ở trên, vừa giữ được log kể cả khi entity gốc sau này bị xoá. Cột + index giữ nguyên, filter
-- theo trip_draft_id/trip_id/trip_execution_id vẫn nhanh như cũ.

ALTER TABLE trip_planning_events
    DROP FOREIGN KEY fk_tpe_trip_draft,
    DROP FOREIGN KEY fk_tpe_trip;

ALTER TABLE trip_outcome_events
    DROP FOREIGN KEY fk_toe_trip_execution,
    DROP FOREIGN KEY fk_toe_trip;
