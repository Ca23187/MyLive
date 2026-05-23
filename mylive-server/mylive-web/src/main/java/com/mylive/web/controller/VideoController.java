package com.mylive.web.controller;

import com.mylive.constants.Constants;
import com.mylive.enums.SearchOrderTypeEnum;
import com.mylive.infra.elasticsearch.ElasticSearchComponent;
import com.mylive.infra.jpa.entity.dto.TokenInfo;
import com.mylive.infra.jpa.entity.vo.*;
import com.mylive.infra.redis.RedisComponent;
import com.mylive.response.ResponseVo;
import com.mylive.service.video.VideoInfoService;
import com.mylive.service.video.VideoPlayHistoryService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/video")
@Slf4j
@RequiredArgsConstructor
public class VideoController {

    private final VideoInfoService videoInfoService;
    private final RedisComponent redisComponent;
    private final ElasticSearchComponent elasticSearchComponent;
    private final VideoPlayHistoryService videoPlayHistoryService;

    @PostMapping("/loadRecommendVideoList")
    public ResponseVo<List<BasicVideoInfoVo>> loadRecommendVideo() {
        return ResponseVo.ok(videoInfoService.getRecommendVideoList());
    }

    @PostMapping("/loadVideoList")
    public ResponseVo<PaginationResultVo<PortalVideoInfoVo>> loadVideo(Integer pCategoryId, Integer categoryId, Integer pageNo) {
        return ResponseVo.ok(videoInfoService.getPortalVideoPage(pCategoryId, categoryId, pageNo));
    }

    @PostMapping("/loadVideoPartList")
    public ResponseVo<List<VideoPartVo>> loadVideoPartList(@NotBlank String videoId) {
        return ResponseVo.ok(videoInfoService.getVideoPartList(videoId));
    }

    @PostMapping("/getVideoDetail")
    public ResponseVo<VideoDetailVo> getVideoDetail(@NotBlank String videoId,
                                                    @CookieValue(name = Constants.TOKEN_WEB, required = false) String token) {
        return ResponseVo.ok(videoInfoService.getVideoDetail(videoId, token));
    }

    @RequestMapping("/getVideoRecommend")
    public ResponseVo<List<VideoSearchVo>> getVideoRecommend(@NotBlank String keyword, @NotBlank String videoId) {
        keyword = keyword.trim().replaceAll("\\s+"," ");
        List<VideoSearchVo> voList = elasticSearchComponent.search(
                false,
                keyword,
                SearchOrderTypeEnum.VIDEO_PLAY.getType(),
                1, Constants.PAGE_SIZE_RECOMMEND_VIDEO).getList();
        return ResponseVo.ok(voList.stream()
                .filter(item -> !item.getVideoId().equals(videoId))
                .collect(Collectors.toList()));
    }

    @PostMapping("/reportVideoPlayOnline")
    public ResponseVo<Long> reportVideoPlayOnline(@NotBlank String fileId, String deviceId) {
        return ResponseVo.ok(redisComponent.reportVideoPlayOnline(fileId, deviceId));
    }

    @PostMapping("/reportPlayProgress")
    public ResponseVo<Void> reportPlayProgress(@NotBlank String videoId,
                                               @NotBlank String fileId,
                                               @NotNull Integer progress,
                                               @NotNull Integer finished,
                                               @CookieValue(name = Constants.TOKEN_WEB, required = false) String token) {
        TokenInfo tokenInfo = redisComponent.getTokenInfo(token);
        if (tokenInfo == null) {
            return ResponseVo.ok();
        }
        videoPlayHistoryService.reportPlayProgress(tokenInfo.getUserId(), videoId, fileId, progress, finished);
        return ResponseVo.ok();
    }

    @PostMapping("/search")
    public ResponseVo<PaginationResultVo<VideoSearchVo>> search(@NotBlank String keyword, Integer orderType, Integer pageNo) {
        keyword = keyword.trim().replaceAll("\\s+"," ");
        redisComponent.addKeywordCount(keyword);
        return ResponseVo.ok(elasticSearchComponent.search(true, keyword, orderType, pageNo, Constants.PAGE_SIZE_SEARCH));
    }

    @PostMapping("/getSearchKeywordTop")
    public ResponseVo<List<String>> getSearchKeywordTop() {
        return ResponseVo.ok(redisComponent.getHotKeywords(Constants.HOT_KEYWORDS_DAYS, Constants.HOT_KEYWORDS_NUM));
    }

    @PostMapping("/loadHotVideoList")
    public ResponseVo<PaginationResultVo<PortalVideoInfoVo>> loadHotVideoList(Integer pageNo) {
        return ResponseVo.ok(videoInfoService.getHotVideoList(pageNo));
    }
}
