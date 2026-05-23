import Request from "./Request";
//单服务版本
const Api = {
    checkCode: "/account/checkCode",
    sendEmailCode: "/account/sendEmailCode",
    login: "/account/login",
    logout: "/account/logout",
    register: "/account/register",
    autoLogin: "/account/autoLogin",
    getUserCountInfo: "/account/getUserCountInfo",
    sourcePath: "/api/file/getResource?sourceName=",
    loadAllCategory: "/category/loadAllCategory",
    getSysSetting: "/sysSetting/getSetting",
    //发布视频
    preUploadVideo: "/file/preUploadVideo",
    uploadVideo: "/file/uploadVideo",
    resumeUploadVideo: "/file/resumeUploadVideo",
    delUploadVideo: "/file/delUploadVideo",
    postVideo: "/ucenter/postVideo",
    saveVideoInteraction: "/ucenter/saveVideoInteraction",
    getVideoByVideoId: "/ucenter/getVideoByVideoId",
    loadUcenterVideoPostList: "/ucenter/loadVideoPostList",
    getUcenterVideoCountInfo: "/ucenter/getVideoCountInfo",
    uploadImage: "/file/uploadImage",
    //个人中心
    ucLoadAllVideo: "/ucenter/loadAllVideo",
    ucLoadComment: "/ucenter/loadComment",
    ucDelComment: "/ucenter/delComment",
    ucLoadDanmaku: "/ucenter/loadDanmaku",
    ucDelDanmaku: "/ucenter/delDanmaku",
    ucGetRealTimeStatisticInfo: "/ucenter/getRealTimeStatisticInfo",
    getWeekStatisticInfo: "/ucenter/getWeekStatisticInfo",
    ucDeleteVideo: "/ucenter/deleteVideo",
    //获取视频列表
    loadRecommendVideoList: "/video/loadRecommendVideoList",
    loadVideoList: "/video/loadVideoList",
    loadVideoPartList: "/video/loadVideoPartList",
    getVideoResource: "/api/file/videoResource",
    getVideoDetail: "/video/getVideoDetail",
    //评论
    loadComment: "/comment/loadComment",
    loadReply: "/comment/loadReply",
    postComment: "/comment/postComment",
    userDelComment: "/comment/userDelComment",
    userTopComment: "/comment/topComment",
    userCancelTopComment: "/comment/cancelTopComment",
    //弹幕
    loadDanmaku: "/danmaku/loadDanmaku",
    postDanmaku: "/danmaku/postDanmaku",
    //上报在线人数
    reportVideoPlayOnline: "/video/reportVideoPlayOnline",
    reportPlayProgress: "/video/reportPlayProgress",
    //点赞，评论，投币，评论，收藏
    userAction: "/userAction/doAction",
    //播放历史
    playHistory: "/history/loadHistory",
    delHistory: "/history/delHistory",
    cleanHistory: "/history/cleanHistory",
    //消息
    getNoReadCount: "/message/getNoReadCount",
    loadUserMessage: "/message/loadMessage",
    delMessage: "/message/delMessage",
    getNoReadCountGroup: "/message/getNoReadCountGroup",
    readAll: "/message/readAll",
    searchMentionUser: "/comment/searchMentionUser",
    //个人主页
    uHomeUpdateUserInfo: "/uhome/updateUserInfo",
    uHomeLoadVideo: "/uhome/loadVideoList",
    uHomeGetUsesrInfo: "/uhome/getUserInfo",
    //关注
    uHomeFollow: "/uhome/follow",
    //取消关注
    uHomeUnFollow: "/uhome/unFollow",
    //关注列表
    uHomeFollowList: "/uhome/loadFollowList",
    //粉丝列表
    uHomeFanList: "/uhome/loadFanList",
    //视频系列
    uHomeSeriesLoadVideoSeries: "/uhome/series/loadVideoSeries",
    //获取系列视频
    uHomeSeriesLoadAllVideo: "/uhome/series/loadAllVideo",
    //保存系列
    uHomeSeriesSaveVideoSeries: "/uhome/series/saveVideoSeries",
    //修改系列顺序
    uHomeSeriesReorderVideoSeries: "/uhome/series/reorderVideoSeries",
    //获取系列详情
    uHomeSeriesGetVideoSeriesDetail: "/uhome/series/getVideoSeriesDetail",
    //删除系列
    uHomeSeriesDelVideoSeries: "/uhome/series/delVideoSeries",
    //保存系列视频
    uHomeSeriesAddSeriesVideo: "/uhome/series/addSeriesVideo",
    //重排系列视频
    uHomeSeriesReorderSeriesVideo: "/uhome/series/reorderSeriesVideo",
    //删除系列视频
    uHomeSeriesDelSeriesVideo: "/uhome/series/delSeriesVideo",
    //获取所有列表
    uHomeSeriesLoadVideoSeriesWithVideo: "/uhome/series/loadVideoSeriesWithVideo",
    //收藏列表
    uHomeLoadSave: "/uhome/loadUserSave",
    //设置主题
    saveTheme: "/uhome/saveTheme",
    //搜索
    search: "/video/search",
    getSearchKeywordTop: "/video/getSearchKeywordTop",
    //推荐视频
    getVideoRecommend: "/video/getVideoRecommend",
    //热门视频
    hotVideoList: "/video/loadHotVideoList"
}

//上传封面
const uploadImage = async (file, createThumbnail = false) => {
    let result = await Request({
        url: Api.uploadImage,
        params: {
            file,
            createThumbnail
        },
    })
    if (!result) {
        return;
    }
    return result.data;
}

const doUserAction = async (config, callback) => {
    let result = await Request({
        url: Api.userAction,
        params: config,
        showLoading: true,
    })
    if (!result) {
        return;
    }
    callback()
}

export {
    Api,
    uploadImage,
    doUserAction
}