<template>
  <div class="cartoon-characters">
    <!-- 橙色圆形小伙伴 -->
    <div class="character character-orange" :class="{ 'is-closed': isPasswordInput }">
      <div class="character-body">
        <div class="eye left">
          <div class="pupil" :style="pupilStyles[0]"></div>
        </div>
        <div class="eye right">
          <div class="pupil" :style="pupilStyles[1]"></div>
        </div>
        <div class="mouth"></div>
        <div class="blush left"></div>
        <div class="blush right"></div>
      </div>
    </div>

    <!-- 紫色长方形小伙伴 -->
    <div class="character character-purple" :class="{ 'is-closed': isPasswordInput }">
      <div class="character-body">
        <div class="eye left">
          <div class="pupil" :style="pupilStyles[2]"></div>
        </div>
        <div class="eye right">
          <div class="pupil" :style="pupilStyles[3]"></div>
        </div>
        <div class="mouth"></div>
        <div class="highlight"></div>
      </div>
    </div>

    <!-- 黄色椭圆小伙伴 -->
    <div class="character character-yellow" :class="{ 'is-closed': isPasswordInput }">
      <div class="character-body">
        <div class="eye left">
          <div class="pupil" :style="pupilStyles[4]"></div>
        </div>
        <div class="eye right">
          <div class="pupil" :style="pupilStyles[5]"></div>
        </div>
        <div class="mouth"></div>
        <div class="glasses"></div>
        <div class="blush left"></div>
      </div>
    </div>

    <!-- 深色圆角小伙伴 -->
    <div class="character character-dark" :class="{ 'is-closed': isPasswordInput }">
      <div class="character-body">
        <div class="eye left">
          <div class="pupil" :style="pupilStyles[6]"></div>
        </div>
        <div class="eye right">
          <div class="pupil" :style="pupilStyles[7]"></div>
        </div>
        <div class="mouth"></div>
        <div class="highlight"></div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue'

const props = defineProps({
  isPasswordInput: {
    type: Boolean,
    default: false
  }
})

const mouseX = ref(window.innerWidth / 2)
const mouseY = ref(window.innerHeight / 2)

const handleMouseMove = (e) => {
  mouseX.value = e.clientX
  mouseY.value = e.clientY
}

// 使用计算属性确保响应式更新
const pupilStyles = computed(() => {
  const styles = []
  
  for (let i = 0; i < 8; i++) {
    if (props.isPasswordInput) {
      styles.push({
        transform: 'translate(-50%, -50%) scaleY(0.1)'
      })
      continue
    }

    const characters = document.querySelectorAll('.character')
    if (!characters.length) {
      styles.push({})
      continue
    }

    const character = characters[Math.floor(i / 2)]
    if (!character) {
      styles.push({})
      continue
    }
    
    const rect = character.getBoundingClientRect()
    const centerX = rect.left + rect.width / 2
    const centerY = rect.top + rect.height / 2

    const angle = Math.atan2(mouseY.value - centerY, mouseX.value - centerX)
    const distance = Math.min(3, Math.hypot(mouseX.value - centerX, mouseY.value - centerY) / 30)

    const x = Math.cos(angle) * distance
    const y = Math.sin(angle) * distance

    styles.push({
      transform: `translate(calc(-50% + ${x}px), calc(-50% + ${y}px))`
    })
  }
  
  return styles
})

onMounted(() => {
  window.addEventListener('mousemove', handleMouseMove)
})

onUnmounted(() => {
  window.removeEventListener('mousemove', handleMouseMove)
})
</script>

<style scoped>
.cartoon-characters {
  display: flex;
  gap: 20px;
  justify-content: center;
  align-items: flex-end;
  padding: 40px;
}

.character {
  position: relative;
  transition: transform 0.3s ease;
}

.character:hover {
  transform: translateY(-10px);
}

/* 橙色圆形小伙伴 */
.character-orange .character-body {
  width: 100px;
  height: 90px;
  background: linear-gradient(135deg, #ff9a56 0%, #ff6b6b 100%);
  border-radius: 50% 50% 45% 45%;
  position: relative;
  box-shadow: 
    0 10px 30px rgba(255, 107, 107, 0.3),
    inset -5px -5px 15px rgba(0, 0, 0, 0.1),
    inset 5px 5px 15px rgba(255, 255, 255, 0.3);
}

/* 紫色长方形小伙伴 */
.character-purple .character-body {
  width: 70px;
  height: 110px;
  background: linear-gradient(135deg, #a78bfa 0%, #7c3aed 100%);
  border-radius: 35px;
  position: relative;
  box-shadow: 
    0 10px 30px rgba(124, 58, 237, 0.3),
    inset -5px -5px 15px rgba(0, 0, 0, 0.1),
    inset 5px 5px 15px rgba(255, 255, 255, 0.3);
}

/* 黄色椭圆小伙伴 */
.character-yellow .character-body {
  width: 95px;
  height: 85px;
  background: linear-gradient(135deg, #fcd34d 0%, #fbbf24 100%);
  border-radius: 50%;
  position: relative;
  box-shadow: 
    0 10px 30px rgba(251, 191, 36, 0.3),
    inset -5px -5px 15px rgba(0, 0, 0, 0.1),
    inset 5px 5px 15px rgba(255, 255, 255, 0.4);
}

/* 深色圆角小伙伴 */
.character-dark .character-body {
  width: 85px;
  height: 95px;
  background: linear-gradient(135deg, #475569 0%, #1e293b 100%);
  border-radius: 40% 40% 45% 45%;
  position: relative;
  box-shadow: 
    0 10px 30px rgba(30, 41, 59, 0.4),
    inset -5px -5px 15px rgba(0, 0, 0, 0.3),
    inset 5px 5px 15px rgba(255, 255, 255, 0.1);
}

/* 眼睛样式 */
.eye {
  position: absolute;
  width: 22px;
  height: 22px;
  background: white;
  border-radius: 50%;
  top: 35%;
  box-shadow: inset 0 2px 5px rgba(0, 0, 0, 0.1);
}

.character-orange .eye {
  width: 24px;
  height: 24px;
}

.character-purple .eye {
  width: 18px;
  height: 18px;
  top: 30%;
}

.character-yellow .eye {
  width: 20px;
  height: 20px;
}

.character-dark .eye {
  width: 20px;
  height: 20px;
  background: #f1f5f9;
}

.eye.left {
  left: 22%;
}

.eye.right {
  right: 22%;
}

.character-purple .eye.left {
  left: 18%;
}

.character-purple .eye.right {
  right: 18%;
}

/* 瞳孔 */
.pupil {
  position: absolute;
  width: 10px;
  height: 10px;
  background: #1f2937;
  border-radius: 50%;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  transition: transform 0.1s ease;
}

.character-dark .pupil {
  background: #0f172a;
}

/* 闭眼状态 */
.is-closed .eye {
  background: transparent;
  box-shadow: none;
  border: none;
}

.is-closed .eye::after {
  content: '';
  position: absolute;
  width: 100%;
  height: 3px;
  background: #374151;
  top: 50%;
  left: 0;
  border-radius: 2px;
}

.is-closed .pupil {
  display: none;
}

/* 嘴巴 */
.mouth {
  position: absolute;
  bottom: 25%;
  left: 50%;
  transform: translateX(-50%);
  width: 20px;
  height: 10px;
  border-bottom: 3px solid #374151;
  border-radius: 0 0 20px 20px;
}

.character-dark .mouth {
  border-color: #94a3b8;
}

.is-closed .mouth {
  width: 15px;
  height: 8px;
  border: 3px solid #374151;
  border-radius: 50%;
  background: transparent;
}

/* 腮红 */
.blush {
  position: absolute;
  width: 15px;
  height: 10px;
  background: rgba(255, 107, 107, 0.3);
  border-radius: 50%;
  top: 50%;
}

.blush.left {
  left: 10%;
}

.blush.right {
  right: 10%;
}

/* 眼镜 */
.glasses {
  position: absolute;
  top: 32%;
  left: 50%;
  transform: translateX(-50%);
  width: 70%;
  height: 25px;
}

.glasses::before,
.glasses::after {
  content: '';
  position: absolute;
  width: 24px;
  height: 20px;
  border: 3px solid #4b5563;
  border-radius: 50%;
  top: 0;
}

.glasses::before {
  left: 5px;
}

.glasses::after {
  right: 5px;
}

/* 高光 */
.highlight {
  position: absolute;
  top: 15%;
  right: 20%;
  width: 15px;
  height: 25px;
  background: rgba(255, 255, 255, 0.3);
  border-radius: 50%;
  transform: rotate(-20deg);
}

.character-purple .highlight {
  width: 12px;
  height: 20px;
}

/* 动画效果 */
@keyframes bounce {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-5px); }
}

.character {
  animation: bounce 3s ease-in-out infinite;
}

.character:nth-child(2) {
  animation-delay: 0.5s;
}

.character:nth-child(3) {
  animation-delay: 1s;
}

.character:nth-child(4) {
  animation-delay: 1.5s;
}
</style>
