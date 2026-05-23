<template>
  <div class="content-404">
    <div class="bg"></div>

    <div class="actions">
      <router-link class="gohome" to="/">
        Back to Home
      </router-link>
    </div>
  </div>
</template>

<script setup>
// 不再需要自动跳转逻辑
</script>

<style lang="scss" scoped>
.content-404 {
  text-align: center;

  .bg {
    margin: 0 auto;
    width: 800px;
    height: 500px;
    background-image: url("../../assets/404.png");
    background-repeat: no-repeat;
    background-position: center;
    background-size: contain;
  }

  .actions {
    margin-top: 30px;
  }

  .gohome {
    display: inline-block;
    padding: 10px 20px;
    font-size: 16px;
    color: #fff;
    background: var(--blue);
    border-radius: 6px;
    text-decoration: none;
    transition: 0.2s;

    &:hover {
      opacity: 0.9;
    }
  }
}
</style>