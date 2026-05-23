import { createRouter, createWebHistory } from 'vue-router'
import { useLoginStore } from "@/stores/loginStore"
import axios from "axios"
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      name: '框架页',
      redirect: "/",
      component: () => import('@/views/layout/Layout.vue'),
      children: [
        {
          path: '/',
          name: 'index',
          component: () => import('@/views/index/Index.vue'),
        },
        {
          path: '/video/:videoId',
          name: 'videoDetail',
          component: () => import('@/views/videoDetail/VideoDetail.vue'),
        },
        {
          path: '/v/:pCategoryCode',
          name: 'categoryVideo',
          component: () => import('@/views/videoList/CategoryVideo.vue'),
        },
        {
          path: '/v/:pCategoryCode/:categoryCode',
          name: 'subCategoryVideo',
          component: () => import('@/views/videoList/CategoryVideo.vue'),
        },
        {
          path: '/history',
          name: 'history',
          component: () => import('@/views/history/History.vue'),
          meta: {
            requiresLogin: true,
          },
        }, {
          path: '/message',
          name: 'messagehome',
          component: () => import('@/views/message/UserMessage.vue'),
          meta: {
            requiresLogin: true,
          },
        }, {
          path: '/message/:messageType',
          name: 'message',
          component: () => import('@/views/message/UserMessage.vue'),
          meta: {
            requiresLogin: true,
          },
        }, {
          path: '/search',
          name: 'search',
          component: () => import('@/views/search/Search.vue'),
        }, {
          path: '/hot',
          name: 'hot',
          component: () => import('@/views/hot/Hot.vue'),
        }
      ]
    },
    {
      path: '/ucenter',
      name: 'ucenter',
      redirect: "/ucenter/home",
      component: () => import('@/views/ucenter/UcLayout.vue'),
      meta: {
        requiresLogin: true,
      },
      children: [{
        path: '/ucenter/home',
        name: '用户中心首页',
        component: () => import('@/views/ucenter/Home.vue'),
      }, {
        path: '/ucenter/postVideo',
        name: '上传视频',
        component: () => import('@/views/ucenter/postvideo/Post.vue'),
      }, {
        path: '/ucenter/editVideo',
        name: '编辑视频',
        component: () => import('@/views/ucenter/postvideo/Post.vue'),
      }, {
        path: '/ucenter/video',
        name: '视频列表',
        component: () => import('@/views/ucenter/VideoList.vue'),
      }, {
        path: '/ucenter/comment',
        name: '评论管理',
        component: () => import('@/views/ucenter/CommentList.vue'),
      }, {
        path: '/ucenter/danmaku',
        name: '弹幕管理',
        component: () => import('@/views/ucenter/DanmakuList.vue'),
      }]
    },
    {
      path: '/user/:userId',
      name: 'userhome',
      redirect: "/user/:userId",
      component: () => import('@/views/userhome/UserHomeLayout.vue'),
      children: [
        {
          path: '/user/:userId',
          name: 'uhome',
          component: () => import('@/views/userhome/Home.vue'),
        }, {
          path: '/user/:userId/video',
          name: 'uhomeMyVideo',
          component: () => import('@/views/userhome/VideoList.vue'),
        }, {
          path: '/user/:userId/series',
          name: 'uhomeSeries',
          component: () => import('@/views/userhome/VideoSeries.vue'),
        }, {
          path: '/user/:userId/series/:seriesId',
          name: 'uhomeSeriesDetail',
          component: () => import('@/views/userhome/VideoSeriesDetail.vue'),
        }, {
          path: '/user/:userId/saved',
          name: 'saved',
          component: () => import('@/views/userhome/Saved.vue'),
        }, {
          path: '/user/:userId/follow',
          name: 'uhomeFollow',
          component: () => import('@/views/userhome/FollowFansList.vue'),
          meta: {
            requiresLogin: true,
          },
        }, {
          path: '/user/:userId/fans',
          name: 'uhomeFans',
          component: () => import('@/views/userhome/FollowFansList.vue'),
          meta: {
            requiresLogin: true,
          },
        }]
    },
    {
      path: '/404',
      name: '错误页404',
      component: () => import('@/views/error/404.vue'),
    },
    {
      path: '/:pathMatch(.*)*',
      beforeEnter: (to, from, next) => {
        next({
          path: '/404',
          replace: true
        })
      }
    }
  ]
})
const tryAutoLogin = async () => {
  const loginStore = useLoginStore();

  if (loginStore.isLogin) {
    return true;
  }

  try {
    const result = await axios.post(
      "/api/account/autoLogin",
      new URLSearchParams(),
      {
        withCredentials: true,
        timeout: 10 * 1000,
        headers: {
          "X-Requested-With": "XMLHttpRequest",
          "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
        },
      }
    );

    const responseData = result.data;

    if (responseData.code === 200 && responseData.data?.userId) {
      loginStore.saveUserInfo(responseData.data);
      return true;
    }
  } catch (e) {
    // 自动登录失败，下面统一当未登录处理
  }

  loginStore.logout();
  return false;
};

router.beforeEach(async (to, from, next) => {
  if (to.meta.requiresLogin) {
    const isLogin = await tryAutoLogin();

    if (!isLogin) {
      const loginStore = useLoginStore();
      loginStore.setLogin(true);
      next("/");
      return;
    }
  }

  next();
});
export default router