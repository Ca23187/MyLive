<template>
  <div
    :class="[layoutType == 'grid' ? 'data-list-grad' : '']"
    :style="{ 'grid-template-columns': `repeat(${gridCount}, 1fr)` }"
  >
    <template v-for="item in dataSource.list">
      <slot :data="item"></slot>
    </template>
  </div>
  <div class="bottom-state">
    <div v-if="loading">
      <img :src="proxy.Utils.getLocalImage('playing.gif')" />
      Loading...
    </div>

    <div v-else-if="!dataSource.hasMore && dataSource.list.length > 0">
      {{ loadEndMsg }}
    </div>
  </div>
  <NoData v-if="dataSource.list && dataSource.list.length == 0"> </NoData>
</template>

<script setup>
import { mitter } from "@/eventbus/eventBus.js";
import {
  ref,
  reactive,
  getCurrentInstance,
  nextTick,
  onMounted,
  onUnmounted,
} from "vue";
import { useRouter } from "vue-router";
const { proxy } = getCurrentInstance();
const router = useRouter();

const props = defineProps({
  layoutType: {
    type: String,
    default: "grid",
  },
  dataSource: {
    type: Object,
  },
  loading: {
    type: Boolean,
  },
  loadEndMsg: {
    type: String,
    default: "No more items",
  },
  gridCount: {
    type: Number,
    default: 5,
  },
});

const emit = defineEmits(["loadData"]);
const scrollHandler = (curScrollTop) => {
  const bottomOffset = 200; // ✅ 提前100px触发

  if (
    window.innerHeight + curScrollTop <
    document.body.offsetHeight - bottomOffset
  ) {
    return;
  }

  if (props.loading || !props.dataSource.hasMore) return;

  emit("loadData");
};

let ticking = false;

const scrollHandlerWrapper = (curScrollTop) => {
  if (ticking) return;

  ticking = true;

  requestAnimationFrame(() => {
    scrollHandler(curScrollTop);
    ticking = false;
  });
};

onMounted(() => {
  mitter.on("windowScroll", scrollHandlerWrapper);
});

onUnmounted(() => {
  mitter.off("windowScroll");
});
</script>

<style lang="scss" scoped>
.data-list-grad {
  display: grid;
  grid-gap: 20px;
  padding-bottom: 10px;
}

.reach-bottom {
  text-align: center;
  line-height: 40px;
  color: var(--text3);
}
.bottom-state {
  height: 40px; // ✅ 固定高度（关键）
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text3);

  img {
    width: 20px;
    margin-right: 10px;
  }
}
.bottom-loading {
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text3);
  img {
    width: 20px;
    margin-right: 10px;
  }
}
</style>
