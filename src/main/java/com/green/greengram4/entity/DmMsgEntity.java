package com.green.greengram4.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_dm_msg")
public class DmMsgEntity extends CreatedAtEntity {
    @EmbeddedId
    private DmMsgIds dmMsgIds;

    @ManyToOne
    @MapsId
    @JoinColumn(columnDefinition = "BIGINT UNSIGNED", name = "idm2")
    private DmEntity dmEntity;

    @ManyToOne
    @JoinColumn(name = "iuser", nullable = false)
    private UserEntity userEntity;

    @Column(length = 2000, nullable = false)
    private String msg;
}
