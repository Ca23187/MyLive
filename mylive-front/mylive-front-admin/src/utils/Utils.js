import moment from "moment";
moment.locale('en');

const isEmpty = (str) => {
    if (str == null || str == "" || str == undefined) {
        return true;
    }
    return false;
}

const formatDate = (timestamp) => {
    const time = moment(timestamp);
    const now = moment();

    const diffSeconds = now.diff(time, "seconds");
    const diffMinutes = now.diff(time, "minutes");
    const diffHours = now.diff(time, "hours");
    const diffDays = now.diff(time, "days");

    // 刚刚
    if (diffSeconds < 60) {
        return "Just now";
    }

    // X分钟前
    if (diffMinutes < 60) {
        return `${diffMinutes} min ago`;
    }

    // X小时前
    if (diffHours < 24) {
        return `${diffHours} hours ago`;
    }

    // 昨天
    if (diffDays === 1) {
        return "Yesterday";
    }

    // 一周内
    if (diffDays < 7) {
        return time.format("dddd");
    }

    // 当年
    if (now.isSame(time, "year")) {
        return time.format("MMM D");
    }

    // 超过一年
    return time.format("MMM D, YYYY");
};
const size2Str = (limit) => {
    var size = "";
    if (limit < 0.1 * 1024) {                            //小于0.1KB，则转化成B
        size = limit.toFixed(2) + "B"
    } else if (limit < 1024 * 1024) {            //小于0.1MB，则转化成KB
        size = (limit / 1024).toFixed(2) + "KB"
    } else if (limit < 1024 * 1024 * 1024) {        //小于1GB，则转化成MB
        size = (limit / (1024 * 1024)).toFixed(2) + "MB"
    } else {                                            //其他转化成GB
        size = (limit / (1024 * 1024 * 1024)).toFixed(2) + "GB"
    }
    var sizeStr = size + "";                        //转成字符串
    var index = sizeStr.indexOf(".");                    //获取小数点处的索引
    var dou = sizeStr.substring(index + 1, 2)            //获取小数点后两位的值
    if (dou == "00") {                                //判断后两位是否为00，如果是则删除00               
        return sizeStr.substring(0, index) + sizeStr.substr(index + 3, 2)
    }
    return size;
}
const convertSecondsToHMS = (seconds) => {
    var hours = Math.floor(seconds / 3600);
    var minutes = Math.floor((seconds % 3600) / 60);
    var remainingSeconds = seconds % 60;

    return (hours == 0 ? "" : hours.toString().padStart(2, '0') + ":") + minutes.toString().padStart(2, '0') + ":" + remainingSeconds.toString().padStart(2, '0');
}

const getFileName = (fileName) => {
    if (!fileName) {
        return fileName;
    }
    return fileName.lastIndexOf(".") == -1 ? fileName : fileName.substring(0, fileName.lastIndexOf("."));
}

const getLocalImage = (image) => {
    return new URL(`../assets/${image}`, import.meta.url).href;
}

export default {
    isEmpty,
    formatDate,
    size2Str,
    convertSecondsToHMS,
    getFileName,
    getLocalImage
}