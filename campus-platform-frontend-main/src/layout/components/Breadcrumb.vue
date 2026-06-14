<template>
  <el-breadcrumb separator="/" class="breadcrumb">
    <el-breadcrumb-item v-for="(item, index) in breadcrumbs" :key="item.path">
      <span
        v-if="index === breadcrumbs.length - 1"
        class="no-redirect"
      >{{ item.meta.title }}</span>
      <a v-else @click.prevent="handleLink(item)">{{ item.meta.title }}</a>
    </el-breadcrumb-item>
  </el-breadcrumb>
</template>

<script setup>
import { ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'

const route = useRoute()
const router = useRouter()
const breadcrumbs = ref([])

const getBreadcrumbs = () => {
  const matched = route.matched.filter(item => item.meta?.title)
  breadcrumbs.value = matched
}

const handleLink = (item) => {
  router.push(item.path)
}

watch(() => route.path, getBreadcrumbs, { immediate: true })
</script>

<style scoped>
.breadcrumb {
  font-size: 14px;
  color: var(--apple-ink-muted-48, #7a7a7a);
}

.breadcrumb :deep(.el-breadcrumb__inner a) {
  color: var(--apple-ink-muted-48, #7a7a7a);
  font-weight: 500;
}

.breadcrumb :deep(.el-breadcrumb__inner a:hover) {
  color: var(--apple-primary, #2563eb);
}

.no-redirect {
  color: var(--apple-ink, #1d1d1f);
  font-weight: 600;
  cursor: text;
}

@media (max-width: 760px) {
  .breadcrumb {
    display: none;
  }
}
</style>
