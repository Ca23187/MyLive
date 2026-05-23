import { ElMessageBox } from 'element-plus'

const confirm = ({ message, okfun, showCancelBtn = true, okText = 'Confirm' }) => {
    ElMessageBox.confirm(message, 'Confirmation', {
        "close-on-click-modal": false,
        confirmButtonText: okText,
        cancelButtonText: 'Cancel',
        showCancelButton: showCancelBtn,
        type: 'info',
    }).then(async () => {
        if (okfun) {
            okfun();
        }
    }).catch(() => {
    });
};
export default confirm;