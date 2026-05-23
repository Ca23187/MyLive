package com.mylive.service.video.impl;

import com.mylive.exception.BusinessException;
import com.mylive.infra.jpa.entity.po.UserVideoSeries;
import com.mylive.infra.jpa.entity.po.UserVideoSeriesVideo;
import com.mylive.infra.jpa.entity.po.VideoInfo;
import com.mylive.infra.jpa.entity.vo.UHomeVideoInfoVo;
import com.mylive.infra.jpa.entity.vo.UserVideoSeriesDetailVo;
import com.mylive.infra.jpa.entity.vo.VideoSeriesWithVideoVo;
import com.mylive.infra.jpa.repository.UserVideoSeriesRepository;
import com.mylive.infra.jpa.repository.UserVideoSeriesVideoRepository;
import com.mylive.infra.jpa.repository.VideoInfoRepository;
import com.mylive.response.ResponseCodeEnum;
import com.mylive.service.video.UserVideoSeriesService;
import com.mylive.utils.StringTools;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserVideoSeriesServiceImpl implements UserVideoSeriesService {


    private final UserVideoSeriesRepository userVideoSeriesRepository;
    private final VideoInfoRepository videoInfoRepository;
    private final UserVideoSeriesVideoRepository userVideoSeriesVideoRepository;

    @Override
    public List<UserVideoSeries> getAllSeries(Long userId) {
        return userVideoSeriesRepository.findAllByUserId(userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveUserVideoSeries(UserVideoSeries bean, String videoIds) {
        if (bean.getSeriesId() == null) {
            if (!StringUtils.hasText(videoIds)) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }
            List<String> videoIdList = StringTools.parseDelimitedDistinctList(videoIds, ",");
            if (videoIdList.isEmpty()) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }
            Map<String, VideoInfo> videoMap = checkVideoIds(bean.getUserId(), videoIdList);
            // 最新加入合集的视频封面（冗余字段换查询效率）
            String lastVideoId = videoIdList.get(videoIdList.size() - 1);
            VideoInfo last = videoMap.get(lastVideoId);
            bean.setCover(last.getVideoCover());
            bean.setOrderNum(userVideoSeriesRepository.findMaxOrder(bean.getUserId()) + 1);
            userVideoSeriesRepository.save(bean);

            Integer order = userVideoSeriesVideoRepository.findMaxOrder(bean.getSeriesId());
            List<UserVideoSeriesVideo> seriesVideoList = new ArrayList<>();
            for (String videoId : videoIdList) {
                // 用前端传来的 videoIds 顺序决定 Order 顺序，使用 Map 映射保序（因为数据库输出可能乱序）
                VideoInfo video = videoMap.get(videoId);
                UserVideoSeriesVideo seriesVideo = new UserVideoSeriesVideo();
                seriesVideo.setUserId(bean.getUserId());
                seriesVideo.setSeriesId(bean.getSeriesId());
                seriesVideo.setVideoId(video.getVideoId());
                seriesVideo.setOrderNum(++order);
                seriesVideoList.add(seriesVideo);
            }
            userVideoSeriesVideoRepository.saveAll(seriesVideoList);
        } else {
            int count = userVideoSeriesRepository.updateSeriesNameAndSeriesDescriptionByUserIdAndSeriesId(
                    bean.getSeriesName(),
                    bean.getSeriesDescription(),
                    bean.getUserId(),
                    bean.getSeriesId()
            );
            if (count == 0) {
                throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
            }
        }
    }

    private Map<String, VideoInfo> checkVideoIds(Long userId, List<String> videoIdList) {
        List<VideoInfo> videos = videoInfoRepository.findByUserIdAndVideoIdIn(userId, videoIdList);
        if (videos.size() != videoIdList.size()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        return videos.stream().collect(
                Collectors.toMap(
                        VideoInfo::getVideoId,
                        v -> v
                ));
    }

    @Override
    public UserVideoSeriesDetailVo getVideoSeriesDetail(Long seriesId) {
        UserVideoSeries videoSeries = userVideoSeriesRepository.findById(seriesId)
                .orElseThrow(() -> new BusinessException(ResponseCodeEnum.NOT_FOUND));
        List<UserVideoSeriesDetailVo.UserVideoSeriesVideoVo> seriesVideoList = userVideoSeriesVideoRepository.findVideoSeriesVideoVoList(seriesId);
        return new UserVideoSeriesDetailVo(videoSeries, seriesVideoList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void addSeriesVideo(Long userId, Long seriesId, String videoIds) {
        UserVideoSeries userVideoSeries = userVideoSeriesRepository.findByUserIdAndSeriesId(userId, seriesId);
        if (userVideoSeries == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        List<String> videoIdList = StringTools.parseDelimitedDistinctList(videoIds, ",");
        if (videoIdList.isEmpty()) {
            return;
        }
        // 思路与 saveUserVideoSeries 相同，cover存最新加入合集的视频封面
        Map<String, VideoInfo> videoMap = checkVideoIds(userId, videoIdList);
        String lastVideoId = videoIdList.get(videoIdList.size() - 1);
        VideoInfo last = videoMap.get(lastVideoId);
        userVideoSeriesRepository.updateCoverBySeriesId(last.getVideoCover(), seriesId);

        Integer order = userVideoSeriesVideoRepository.findMaxOrder(seriesId);
        List<UserVideoSeriesVideo> seriesVideoList = new ArrayList<>();
        for (String videoId : videoIdList) {
            VideoInfo video = videoMap.get(videoId);
            UserVideoSeriesVideo seriesVideo = new UserVideoSeriesVideo();
            seriesVideo.setUserId(userId);
            seriesVideo.setSeriesId(seriesId);
            seriesVideo.setVideoId(video.getVideoId());
            seriesVideo.setOrderNum(++order);
            seriesVideoList.add(seriesVideo);
        }
        userVideoSeriesVideoRepository.saveAll(seriesVideoList);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderSeriesVideo(Long userId, Long seriesId, String videoIds) {
        UserVideoSeries userVideoSeries = userVideoSeriesRepository.findByUserIdAndSeriesId(userId, seriesId);
        if (userVideoSeries == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        List<String> videoIdList = StringTools.parseDelimitedDistinctList(videoIds, ",");
        if (videoIdList.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        // 校验 video 是否属于该用户（防注入）
        List<UserVideoSeriesVideo> dbList =
                userVideoSeriesVideoRepository.findByUserIdAndSeriesId(userId, seriesId);

        // 转成 set 用来校验一致性
        Set<String> dbVideoIds = dbList.stream()
                .map(UserVideoSeriesVideo::getVideoId)
                .collect(Collectors.toSet());

        // 不跟数据库一模一样就拒绝重排
        if (!dbVideoIds.equals(new HashSet<>(videoIdList))) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        // 开始重排 Order
        int order = 0;
        for (String videoId : videoIdList) {
            userVideoSeriesVideoRepository.updateOrderNum(
                    userId,
                    seriesId,
                    videoId,
                    ++order
            );
        }

        // 更新封面（最新 = 最后一个）
        String lastVideoId = videoIdList.get(videoIdList.size() - 1);
        userVideoSeriesRepository.updateCoverByVideoId(seriesId, lastVideoId, userId);
    }

    @Override
    public List<VideoSeriesWithVideoVo> getVideoSeriesWithVideo(Long userId) {
        List<VideoSeriesWithVideoVo.FlatDto> rows =
                userVideoSeriesRepository.findSeriesWithVideosFlat(userId);

        Map<Long, VideoSeriesWithVideoVo> map = new LinkedHashMap<>();

        for (VideoSeriesWithVideoVo.FlatDto row : rows) {

            VideoSeriesWithVideoVo vo = map.computeIfAbsent(
                    row.getSeriesId(),
                    id -> {
                        VideoSeriesWithVideoVo tmp = new VideoSeriesWithVideoVo();
                        tmp.setSeriesId(row.getSeriesId());
                        tmp.setSeriesName(row.getSeriesName());
                        tmp.setVideoInfoList(new ArrayList<>());
                        return tmp;
                    }
            );

            if (row.getVideoId() != null) {
                UHomeVideoInfoVo video = new UHomeVideoInfoVo();
                video.setVideoId(row.getVideoId());
                video.setVideoCover(row.getVideoCover());
                video.setVideoTitle(row.getVideoTitle());
                video.setPlayCount(row.getPlayCount());
                video.setCreatedAt(row.getCreatedAt());

                vo.getVideoInfoList().add(video);
            }
        }

        return new ArrayList<>(map.values());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delSeriesVideo(Long userId, Long seriesId, String videoId) {

        UserVideoSeries dbSeries = userVideoSeriesRepository
                .findByUserIdAndSeriesId(userId, seriesId);
        if (dbSeries == null) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        if (0 == userVideoSeriesVideoRepository.deleteByUserIdAndSeriesIdAndVideoId(userId, seriesId, videoId)) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        userVideoSeriesVideoRepository.flush(); // 后面的读操作的结果依赖于前面的写操作，因此手动刷一下

        // findLatestCover负责
        // 1. 若即将从合集里删除的 Video 是 Series 的封面，就找目前 Series 里的 Order 最大的视频当作新封面（前提是合集里还有视频）
        // 2. 若即将从合集里删除的 Video 是 Series 的封面，且合集里只剩该 Video，返回 null（合集被删空了，自然没封面）
        // 3. 若即将从合集里删除的 Video 不是 Series 的封面，返回原封面
        String newCover = userVideoSeriesVideoRepository.findLatestCover(seriesId, userId);
        dbSeries.setCover(newCover);
        userVideoSeriesRepository.save(dbSeries);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delVideoSeries(Long userId, Long seriesId) {
        int count = userVideoSeriesRepository.deleteByUserIdAndSeriesId(userId, seriesId);
        if (count == 0) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        userVideoSeriesVideoRepository.deleteByUserIdAndSeriesId(userId, seriesId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderVideoSeries(Long userId, String seriesIds) {
        List<Long> seriesIdList = StringTools.parseDelimitedDistinctList(seriesIds, ",")
                .stream()
                .map(Long::valueOf)
                .toList();
        if (seriesIdList.isEmpty()) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }
        // 查 DB 当前合集并判断是否完全一致
        List<UserVideoSeries> dbList = userVideoSeriesRepository.findByUserId(userId);
        Set<Long> dbSet = dbList.stream()
                .map(UserVideoSeries::getSeriesId)
                .collect(Collectors.toSet());
        if (!dbSet.equals(new HashSet<>(seriesIdList))) {
            throw new BusinessException(ResponseCodeEnum.BAD_REQUEST);
        }

        int order = 0;
        for (Long seriesId : seriesIdList) {
            userVideoSeriesRepository.updateOrderNumBySeriesIdAndUserId(
                    ++order,
                    seriesId,
                    userId
            );
        }
    }
}
