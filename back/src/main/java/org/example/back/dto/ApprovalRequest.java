package org.example.back.dto;

import java.time.LocalTime;

/**
 * 审批请求DTO
 */
public class ApprovalRequest {

    private String comment;

    private LocalTime adjustedStartTime;
    private LocalTime adjustedEndTime;

    public String getComment() { return comment; }
    public void setComment(String comment) { this.comment = comment; }

    public LocalTime getAdjustedStartTime() { return adjustedStartTime; }
    public void setAdjustedStartTime(LocalTime adjustedStartTime) { this.adjustedStartTime = adjustedStartTime; }

    public LocalTime getAdjustedEndTime() { return adjustedEndTime; }
    public void setAdjustedEndTime(LocalTime adjustedEndTime) { this.adjustedEndTime = adjustedEndTime; }
}
