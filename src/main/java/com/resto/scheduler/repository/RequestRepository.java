package com.resto.scheduler.repository;

import com.resto.scheduler.model.AppUser;
import com.resto.scheduler.model.Request;
import com.resto.scheduler.model.enums.RequestStatus;
import com.resto.scheduler.model.enums.RequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface RequestRepository extends JpaRepository<Request, Long> {

    List<Request> findByRequesterOrderByCreatedAtDesc(AppUser requester);

    List<Request> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    List<Request> findByTypeAndRequestDate(RequestType type, LocalDate date);

    boolean existsByRequesterAndRequestDateAndStatusIn(AppUser requester, LocalDate date, List<RequestStatus> statuses);

    // range helpers for badges and manager views
    List<Request> findByTypeAndStatusAndRequestDateBetween(
            RequestType type, RequestStatus status, LocalDate start, LocalDate end);

    List<Request> findByStatusAndRequestDateBetween(
            RequestStatus status, LocalDate start, LocalDate end);

    List<Request> findByReceiverOrderByCreatedAtDesc(AppUser receiver);
}
