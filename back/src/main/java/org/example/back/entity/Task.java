package org.example.back.entity;

import com.baomidou.mybatisplus.annotation.*;
import org.example.back.entity.enums.Priority;
import org.example.back.entity.enums.TaskStatus;
import org.example.back.entity.enums.TaskType;

import java.time.LocalDate;
import java.time.LocalDateTime;

@TableName("tasks")
public class Task {

    @TableId(type = IdType.AUTO)
    private Long id;
    private TaskType taskType;
    private String title;
    private String description;
    private Long equipmentId;
    private Long assignedTo;
    private Long assignedBy;
    private Priority priority;
    private TaskStatus status;
    private LocalDate dueDate;
    private LocalDateTime completedAt;
    private String report;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    @TableField(exist = false)
    private String equipmentName;
    @TableField(exist = false)
    private String assigneeName;
    @TableField(exist = false)
    private String assignerName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public TaskType getTaskType() { return taskType; }
    public void setTaskType(TaskType taskType) { this.taskType = taskType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Long getEquipmentId() { return equipmentId; }
    public void setEquipmentId(Long equipmentId) { this.equipmentId = equipmentId; }
    public Long getAssignedTo() { return assignedTo; }
    public void setAssignedTo(Long assignedTo) { this.assignedTo = assignedTo; }
    public Long getAssignedBy() { return assignedBy; }
    public void setAssignedBy(Long assignedBy) { this.assignedBy = assignedBy; }
    public Priority getPriority() { return priority; }
    public void setPriority(Priority priority) { this.priority = priority; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public LocalDate getDueDate() { return dueDate; }
    public void setDueDate(LocalDate dueDate) { this.dueDate = dueDate; }
    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }
    public String getReport() { return report; }
    public void setReport(String report) { this.report = report; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getEquipmentName() { return equipmentName; }
    public void setEquipmentName(String equipmentName) { this.equipmentName = equipmentName; }
    public String getAssigneeName() { return assigneeName; }
    public void setAssigneeName(String assigneeName) { this.assigneeName = assigneeName; }
    public String getAssignerName() { return assignerName; }
    public void setAssignerName(String assignerName) { this.assignerName = assignerName; }
}
