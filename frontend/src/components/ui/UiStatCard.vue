<script setup lang="ts">
interface Props {
  label: string
  value?: string | number
  hint?: string
  mono?: boolean
  iconColor?: string
}

withDefaults(defineProps<Props>(), {
  value: '--',
  hint: '',
  mono: false,
  iconColor: 'var(--primary-color, #6366f1)'
})
</script>

<template>
  <article class="ui-stat-card base-card">
    <!-- 图标插槽：如有图标，提升整体精致感 -->
    <div v-if="$slots.icon" class="card-icon" :style="{ color: iconColor }">
      <slot name="icon"></slot>
    </div>

    <div class="card-main">
      <slot name="label">
        <span class="metric-label">{{ label }}</span>
      </slot>
      
      <slot name="value">
        <div class="metric-value" :class="{ 'mono-text': mono }">
          {{ value }}
        </div>
      </slot>
      
      <slot name="hint">
        <p v-if="hint" class="metric-hint">{{ hint }}</p>
      </slot>
    </div>
  </article>
</template>

<style scoped>
.ui-stat-card {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 20px;
  background: var(--bg-card, #ffffff);
  border: 1px solid var(--border-color, #e2e8f0);
  border-radius: 16px;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  overflow: hidden;
  position: relative;
}

/* 悬浮反馈 */
.ui-stat-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 12px 24px -8px rgba(0, 0, 0, 0.1);
  border-color: #cbd5e1;
}

.card-icon {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: rgba(99, 102, 241, 0.1); /* 靛蓝色底 */
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  flex-shrink: 0;
}

.card-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.metric-label {
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: #64748b;
}

.metric-value {
  font-size: 22px;
  font-weight: 700;
  color: #0f172a;
  line-height: 1.3;
}

.metric-hint {
  font-size: 12px;
  color: #94a3b8;
  margin: 0;
}

.mono-text {
  font-family: 'JetBrains Mono', 'Fira Code', monospace;
  font-size: 20px;
}
</style>