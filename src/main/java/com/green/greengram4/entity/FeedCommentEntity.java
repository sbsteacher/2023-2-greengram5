package com.green.greengram4.entity;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "t_feed_comment")
public class FeedCommentEntity extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(columnDefinition = "BIGINT UNSIGNED", name="ifeed_comment")
    private Long ifeedComment;

    @ManyToOne
    @JoinColumn(name = "iuser", nullable = false)
    private UserEntity userEntity;

    @ManyToOne
    @JoinColumn(name = "ifeed", nullable = false)
    private FeedEntity feedEntity;

    @Column(length = 500, nullable = false)
    private String comment;
}
