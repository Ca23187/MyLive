package com.mylive.infra.elasticsearch;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.SortOrder;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.json.JsonData;
import co.elastic.clients.transport.endpoints.BooleanResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mylive.constants.Constants;
import com.mylive.enums.SearchOrderTypeEnum;
import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.dto.es.VideoInfoEsDto;
import com.mylive.infra.jpa.entity.po.UserInfo;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.vo.PaginationResultVo;
import com.mylive.infra.jpa.entity.vo.VideoSearchVo;
import com.mylive.infra.jpa.repository.UserInfoRepository;
import com.mylive.infra.mapstruct.VideoInfoMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ElasticSearchComponent {

    private final ElasticsearchClient elasticsearchClient;
    private final ElasticSearchProperties esProperties;
    private final VideoInfoMapper videoInfoMapper;
    private final ObjectMapper objectMapper;
    private final UserInfoRepository userInfoRepository;

    /**
     * 判断索引是否存在
     */
    private Boolean isExistIndex() throws IOException {

        BooleanResponse response = elasticsearchClient.indices()
                .exists(e -> e.index(esProperties.getIndexName()));

        return response.value();
    }

    /**
     * 创建索引
     */
    @PostConstruct
    public void createIndex() {
        try {
            if (isExistIndex()) {
                return;
            }

            CreateIndexResponse response = elasticsearchClient.indices()
                    .create(c -> c
                            .index(esProperties.getIndexName())
                            .mappings(m -> m
                                    .properties("videoId", p -> p
                                            .text(t -> t.index(false))
                                    )
                                    .properties("userId", p -> p
                                            .text(t -> t.index(false))
                                    )
                                    .properties("videoCover", p -> p
                                            .text(t -> t.index(false))
                                    )
                                    .properties("videoTitle", p -> p
                                            .text(t -> t
                                                    .analyzer("ik_max_word")
                                            )
                                    )
                                    .properties("tags", p -> p
                                            .keyword(k -> k)
                                    )
                                    .properties("playCount", p -> p
                                            .integer(i -> i.index(false))
                                    )
                                    .properties("danmakuCount", p -> p
                                            .integer(i -> i.index(false))
                                    )
                                    .properties("saveCount", p -> p
                                            .integer(i -> i.index(false))
                                    )
                                    .properties("createdAt", p -> p
                                            .date(d -> d
                                                    .format("strict_date_optional_time||epoch_millis")
                                                    .index(false)
                                            )
                                    )
                            )
                    );

            if (!response.acknowledged()) {
                throw new IllegalStateException("Failed to initialize es");
            }
        } catch (Exception e) {
            log.error("Failed to initialize es", e);
            throw new IllegalStateException("Failed to initialize es");
        }
    }

    /**
     * 文档是否存在
     */
    private Boolean docExist(String id) throws IOException {

        BooleanResponse response = elasticsearchClient.exists(e -> e
                .index(esProperties.getIndexName())
                .id(id));

        return response.value();
    }

    /**
     * 保存文档
     */
    public void saveDoc(VideoInfo videoInfo) {
        try {
            if (docExist(videoInfo.getVideoId())) {
                updateDoc(videoInfoMapper.toEsDto(videoInfo));
            } else {
                VideoInfoEsDto dto = videoInfoMapper.toEsDto(videoInfo);
                dto.setSaveCount(0);
                dto.setPlayCount(0);
                dto.setDanmakuCount(0);
                elasticsearchClient.index(i -> i
                        .index(esProperties.getIndexName())
                        .id(videoInfo.getVideoId())
                        .document(dto));
            }
        } catch (Exception e) {
            log.error("Failed to add video to es, videoId = {}", videoInfo.getVideoId(), e);
            throw new BusinessException("Failed to save");
        }
    }

    /**
     * 更新文档
     */
    private void updateDoc(VideoInfoEsDto esDto) {
        try {
            Map<String, Object> dataMap =
                    objectMapper.convertValue(
                            esDto,
                            new TypeReference<>() {}
                    );

            dataMap.values().removeIf(Objects::isNull);

            dataMap.remove("createdAt");

            if (dataMap.isEmpty()) {
                return;
            }

            elasticsearchClient.update(u -> u
                            .index(esProperties.getIndexName())
                            .id(esDto.getVideoId())
                            .doc(dataMap),
                    Object.class);

        } catch (Exception e) {
            log.error("Failed to update video to es, videoId = {}", esDto.getVideoId(), e);
            throw new BusinessException("Failed to save");
        }
    }

    /**
     * 更新计数
     */
    public void updateDocCount(
            String videoId,
            String fieldName,
            Integer count) {

        try {
            elasticsearchClient.update(u -> u
                            .index(esProperties.getIndexName())
                            .id(videoId)
                            .script(s -> s
                                    .source(
                                            "ctx._source[params.field] += params.count"
                                    )
                                    .lang("painless")
                                    .params(
                                            "field",
                                            JsonData.of(fieldName)
                                    )
                                    .params(
                                            "count",
                                            JsonData.of(count)
                                    )
                            ),
                    Object.class);

        } catch (Exception e) {
            log.error("Failed to update count to es, videoId = {}", videoId, e);
            throw new BusinessException("Failed to save");
        }
    }

    /**
     * 删除文档
     */
    public void delDoc(String videoId) {
        try {
            elasticsearchClient.delete(d -> d
                    .index(esProperties.getIndexName())
                    .id(videoId));
        } catch (Exception e) {
            log.error("Failed to delete video from es, videoId = {}", videoId, e);
            throw new BusinessException("Failed to delete");
        }
    }

    /**
     * 搜索
     */
    public PaginationResultVo<VideoSearchVo> search(
            Boolean highlight,
            String keyword,
            Integer orderType,
            Integer pageNo,
            Integer pageSize) {
        try {
            SearchOrderTypeEnum orderEnum = SearchOrderTypeEnum.getByType(orderType);
            if (pageNo == null || pageNo < 1) pageNo = 1;
            if (pageSize == null || pageSize < 1) pageSize = Constants.PAGE_SIZE;
            Integer finalPageNo = pageNo;
            Integer finalPageSize = pageSize;
            SearchResponse<VideoSearchVo> response =
                    elasticsearchClient.search(s -> {
                                s.index(esProperties.getIndexName());
                                s.query(q -> q
                                        .multiMatch(m -> m
                                                .query(keyword)
                                                .fields("videoTitle^3", "tags")
                                                .fuzziness("AUTO")
                                        )
                                );
                                if (highlight) {
                                    s.highlight(h -> h
                                            .fields("videoTitle",
                                                    f -> f)
                                            .preTags("<span class='highlight'>")
                                            .postTags("</span>")
                                    );
                                }
                                if (orderType != null) {
                                    s.sort(sort -> sort
                                            .field(f -> f
                                                    .field(orderEnum.getField())
                                                    .order(SortOrder.Desc)
                                            )
                                    );
                                } else {
                                    s.sort(sort -> sort
                                            .score(sc -> sc.order(SortOrder.Desc)));
                                }
                                s.from((finalPageNo - 1) * finalPageSize);
                                s.size(finalPageSize);
                                return s;
                            },
                            VideoSearchVo.class
                    );

            List<VideoSearchVo> list = new ArrayList<>();
            List<Long> userIdList = new ArrayList<>();

            for (Hit<VideoSearchVo> hit : response.hits().hits()) {
                VideoSearchVo vo = hit.source();
                if (vo == null) {
                    continue;
                }
                if (highlight &&
                        hit.highlight() != null &&
                        hit.highlight().get("videoTitle") != null) {

                    vo.setVideoTitle(
                            hit.highlight()
                                    .get("videoTitle")
                                    .get(0)
                    );
                }
                list.add(vo);
                userIdList.add(vo.getUserId());
            }
            Integer total = (int) response.hits().total().value();

            List<UserInfo> userInfoList = userInfoRepository.findAllById(userIdList);

            Map<Long, UserInfo> userInfoMap =
                    userInfoList.stream()
                            .collect(Collectors.toMap(
                                    UserInfo::getUserId,
                                    Function.identity(),
                                    (a, b) -> b
                            ));

            list.forEach(item -> {
                UserInfo userInfo = userInfoMap.get(item.getUserId());
                if (userInfo != null) {
                    item.setNickname(userInfo.getNickname());
                }
            });
            return new PaginationResultVo<>(
                    total,
                    pageSize,
                    pageNo,
                    (int) Math.ceil((double) total / pageSize),
                    list
            );
        } catch (Exception e) {
            log.error("Failed to query videos, key world = {}", keyword, e);
            throw new BusinessException("Failed to query");
        }
    }
}