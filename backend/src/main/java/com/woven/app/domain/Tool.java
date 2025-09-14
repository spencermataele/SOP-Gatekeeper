package com.woven.app.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

@Entity
@Table(name = "tool")
public class Tool {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private int tool_id;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String tool_name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sop_id", nullable = false)
    private Sop sop;


    /* Getters and Setters */
    public int getTool_id() {
        return tool_id;
    }

    public void setTool_id(int tool_id) {
        this.tool_id = tool_id;
    }

    public String getTool_name() {
        return tool_name;
    }

    public void setTool_name(String tool_name) {
        this.tool_name = tool_name;
    }

    public Sop getSop() {
        return sop;
    }
    public void setSop(Sop sop) {
        this.sop = sop;
    }
}
