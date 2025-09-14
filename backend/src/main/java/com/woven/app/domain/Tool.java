package com.woven.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tool")
public class Tool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tool_id")
    private int toolId;

    @NotBlank
    @Column(name = "tool_name", nullable = false, length = 255)
    private String toolName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sop_id", nullable = false)
    private Sop sop;


    /* Getters and Setters */
    public int getToolId() {
        return toolId;
    }

    public void setToolId(int tool_id) {
        this.toolId = tool_id;
    }

    public String getToolName() {
        return toolName;
    }

    public void setToolName(String tool_name) {
        this.toolName = tool_name;
    }

    public Sop getSop() {
        return sop;
    }
    public void setSop(Sop sop) {
        this.sop = sop;
    }
}
