package com.green.greengram4.feed;

import com.green.greengram4.entity.FeedEntity;
import com.green.greengram4.entity.FeedFavEntity;
import com.green.greengram4.entity.FeedPicsEntity;
import com.green.greengram4.feed.model.FeedSelVo;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;

import com.green.greengram4.feed.model.FeedSelDto;
import java.util.List;
import java.util.stream.Collectors;

import static com.green.greengram4.entity.QFeedEntity.feedEntity;
import static com.green.greengram4.entity.QFeedFavEntity.feedFavEntity;
import static com.green.greengram4.entity.QFeedPicsEntity.feedPicsEntity;
import static com.green.greengram4.entity.QUserEntity.userEntity;

@Slf4j
@RequiredArgsConstructor
public class FeedQdslRepositoryImpl implements FeedQdslRepository {
    private final JPAQueryFactory jpaQueryFactory;

    @Override
    public List<FeedEntity> selFeedAll(FeedSelDto dto, Pageable pageable) {

        JPAQuery<FeedEntity> jpaQuery = jpaQueryFactory.select(feedEntity)
                                                        .from(feedEntity)
                                                        .join(feedEntity.userEntity).fetchJoin()
                                                        .orderBy(feedEntity.ifeed.desc())
                                                        .offset(pageable.getOffset())
                                                        .limit(pageable.getPageSize());

        if(dto.getIsFavList() == 1) {
            jpaQuery.join(feedFavEntity)
                    .on(feedEntity.ifeed.eq(feedFavEntity.feedEntity.ifeed)
                        , feedFavEntity.userEntity.iuser.eq(dto.getLoginedIuser()));
        } else {
            jpaQuery.where(whereTargetUser(dto.getTargetIuser()));
        }

        return jpaQuery.fetch();
    }

    @Override
    public List<FeedPicsEntity> selFeedPicsAll(List<FeedEntity> feedEntityList) {
        return jpaQueryFactory.select(Projections.fields(FeedPicsEntity.class
                , feedPicsEntity.feedEntity, feedPicsEntity.pic))
                .from(feedPicsEntity)
                .where(feedPicsEntity.feedEntity.in(feedEntityList))
                .fetch();
    }

    @Override
    public List<FeedFavEntity> selFeedFavAllByMe(List<FeedEntity> feedEntityList, long loginIuser) {
        return jpaQueryFactory.select(Projections.fields(FeedFavEntity.class
                , feedFavEntity.feedEntity))
                .from(feedFavEntity)
                .where(feedFavEntity.feedEntity.in(feedEntityList)
                        , feedFavEntity.userEntity.iuser.eq(loginIuser))
                .fetch();
    }

    private BooleanExpression whereTargetUser(long targetIuser) {
        return targetIuser == 0 ? null : feedEntity.userEntity.iuser.eq(targetIuser);
    }
}
