<template>
  <el-row :gutter="10">
    <el-col :span="16">
      <el-card class="table-data-card">
        <template #header>
          <div class="header">
            <div class="title">Main Categories</div>
            <div class="btn" @click="showEdit({}, 0)">Add Category</div>
          </div>
        </template>
        <Table
          ref="tableInfoRef"
          :columns="columns"
          :fetch="loadDataList"
          :dataSource="tableData"
          :options="tableOptions"
          :extHeight="tableOptions.extHeight"
          :showPagination="false"
          @rowClick="rowClick"
        >
          <template #icon="{ index, row }">
            <div class="cover">
              <Cover
                :source="row.icon"
                defaultImg="default_image.png.png"
              ></Cover>
            </div>
          </template>
          <template #background="{ index, row }">
            <div class="category-background">
              <Cover
                :source="row.background"
                fit="cover"
                defaultImg="default_banner.png"
              ></Cover>
            </div>
          </template>

          <template #slotOperation="{ index, row }">
            <div class="row-op-panel">
              <a
                class="a-link"
                href="javascript:void(0)"
                @click="showEdit(row, 0)"
                >Edit</a
              >
              <el-divider direction="vertical" />
              <a
                class="a-link"
                href="javascript:void(0)"
                @click="delCategory(row)"
                >Delete</a
              >
              <el-divider direction="vertical" />
              <a
                href="javascript:void(0)"
                @click="reorder(0, index, 'up')"
                :class="[index == 0 ? 'disable' : 'a-link']"
                >Up</a
              >
              <el-divider direction="vertical" />
              <a
                href="javascript:void(0)"
                @click="reorder(0, index, 'down')"
                :class="[
                  index === tableData.list.length - 1 ? 'disable' : 'a-link',
                ]"
              >
                Down
              </a>
            </div>
          </template>
        </Table>
      </el-card>
    </el-col>
    <el-col :span="8">
      <el-card class="table-data-card">
        <template #header>
          <div class="header">
            <div class="title">Subcategories</div>
            <div class="btn" @click="showEdit({}, 1)">Add Subcategory</div>
          </div>
        </template>
        <Table
          :columns="columnSub"
          :dataSource="subCategoryData"
          :options="tableOptions"
          :extHeight="tableOptions.extHeight"
          :showPagination="false"
        >
          <template #slotOperation="{ index, row }">
            <div class="row-op-panel">
              <a
                class="a-link"
                href="javascript:void(0)"
                @click="showEdit(row, 1)"
                >Edit</a
              >
              <el-divider direction="vertical" />
              <a
                class="a-link"
                href="javascript:void(0)"
                @click="delCategory(row)"
                >Delete</a
              >
              <el-divider direction="vertical" />
              <a
                href="javascript:void(0)"
                @click="reorder(row.parentCategoryId, index, 'up')"
                :class="[index == 0 ? 'disable' : 'a-link']"
                >Up</a
              >
              <el-divider direction="vertical" />
              <a
                href="javascript:void(0)"
                @click="reorder(row.parentCategoryId, index, 'down')"
                :class="[
                  index === subCategoryData.list.length - 1
                    ? 'disable'
                    : 'a-link',
                ]"
              >
                Down
              </a>
            </div>
          </template>
        </Table>
      </el-card>
    </el-col>
  </el-row>
  <CategoryEdit ref="categoryEditRef" @reload="loadDataList"></CategoryEdit>
</template>

<script setup>
import CategoryEdit from "./CategoryEdit.vue";
import { getCurrentInstance, nextTick, ref } from "vue";
const { proxy } = getCurrentInstance();

const userInfo = ref(
  JSON.parse(sessionStorage.getItem("userInfo")) || { menuList: [] }
);

const searchForm = ref({});

const tableData = ref({ list: [] });
const tableOptions = ref({
  extHeight: 0,
});

const columns = [
  {
    label: "Icon",
    prop: "icon",
    scopedSlots: "icon",
    width: 70,
  },
  {
    label: "Background",
    prop: "background",
    scopedSlots: "background",
    width: 180,
  },
  {
    label: "Code",
    prop: "categoryCode",
    width: 180,
  },
  {
    label: "Name",
    prop: "categoryName",
  },
  {
    label: "Actions",
    prop: "type",
    scopedSlots: "slotOperation",
    width: 240,
  },
];

const columnSub = columns.slice(columns.length - 3, columns.length);

const tableInfoRef = ref();
const loadDataList = async () => {
  let result = await proxy.Request({
    url: proxy.Api.loadCategory,
  });
  if (!result) {
    return;
  }

  const list = result.data || [];
  tableData.value.list = list;

  // 没有一级分类时，清空右侧二级分类
  if (list.length === 0) {
    currentSelectCategory.value = null;
    subCategoryData.value.list = [];
    return;
  }

  // 当前没有选中项，默认选第一项
  if (!currentSelectCategory.value) {
    currentSelectCategory.value = list[0];
  } else {
    // 重新匹配当前选中的一级分类
    const matched = list.find(
      (item) => item.categoryId === currentSelectCategory.value.categoryId
    );
    currentSelectCategory.value = matched || list[0];
  }

  subCategoryData.value.list = currentSelectCategory.value.children || [];

  nextTick(() => {
    tableInfoRef.value?.setCurrentRow(
      "categoryId",
      currentSelectCategory.value.categoryId
    );
  });
};

const currentSelectCategory = ref(null);
const subCategoryData = ref({ list: [] });

const rowClick = (row) => {
  subCategoryData.value.list = row.children;
  currentSelectCategory.value = row;
};

//新增分类

//删除
const delCategory = (data) => {
  proxy.Confirm({
    message: `Are you sure you want to delete "${data.categoryName}"?`,
    okfun: async () => {
      let result = await proxy.Request({
        url: proxy.Api.delCategory,
        params: {
          categoryId: data.categoryId,
        },
      });
      if (!result) {
        return;
      }

      proxy.Message.success("Operation successful");

      // 如果删的是当前选中的一级分类，先清空右侧二级分类
      if (
        currentSelectCategory.value &&
        currentSelectCategory.value.categoryId == data.categoryId
      ) {
        currentSelectCategory.value = null;
        subCategoryData.value.list = [];
      }

      loadDataList();
    },
  });
};

const reorder = async (parentCategoryId, index, type) => {
  let dataList =
    parentCategoryId == 0
      ? tableData.value.list
      : currentSelectCategory.value.children;
  if (
    (type === "down" && index == dataList.length - 1) ||
    (type == "up" && index == 0)
  ) {
    return;
  }
  let temp = dataList[index];
  let number = type == "down" ? 1 : -1;
  dataList.splice(index, 1);

  dataList.splice(index + number, 0, temp);

  let categoryIds = [];
  dataList.forEach((element) => {
    categoryIds.push(element.categoryId);
  });

  let result = await proxy.Request({
    url: proxy.Api.reorderCategory,
    params: {
      parentCategoryId,
      categoryIds: categoryIds.join(","),
    },
  });
  if (!result) {
    return;
  }
  proxy.Message.success("Reordered successfully");
  loadDataList();
};

const categoryEditRef = ref();
const showEdit = (data, type) => {
  if (type == 1 && !currentSelectCategory.value) {
    proxy.Message.warning("Please create a main category first");
    return;
  }
  if (type == 0) {
    data.parentCategoryId = 0;
  } else if (type == 1 && Object.keys(data).length == 0) {
    data.parentCategoryId = currentSelectCategory.value.categoryId;
  }
  categoryEditRef.value.showEdit(Object.assign({}, data));
};
</script>

<style lang="scss" scoped>
.table-data-card {
  .header {
    display: flex;
    justify-content: space-between;
    .btn {
      cursor: pointer;
      color: var(--blue2);
    }
  }
  .category-background {
    width: 150px;
    height: 80px;
  }
}
</style>
