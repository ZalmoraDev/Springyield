<template>
  <div class="card-wrapper" ref="cardRef" :style="cardStyle">
    <img :src="props.cardType" alt="Interactive Card" class="card-background-image" />
    <img src="/card_overlay.png" alt="Card Chip" class="card-chip" />
    <div class="card-text-overlay company-name text-start" :style="companyNameStyle">{{props.displayName}}</div>
    <div class="card-text-overlay card-details">
      <div class="card-number" :style="cardNumberStyle">{{ props.cardNumber }}</div>
      <div class="card-holder-name" :style="cardHolderNameStyle">{{ props.cardHolderName }}</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed } from 'vue';

const props = defineProps({
  cardNumber: {
    type: String,
    default: '**** **** **** ****'
  },
  cardHolderName: {
    type: String,
    default: 'CARDHOLDER NAME'
  },
  cardType: {
    type: String,
    default: '/card.png' // Default card background image
  },
  displayName: {
    type: String,
    default: 'SpringSpend'
  }
});



const cardRef = ref(null);
const actualCardWidth = ref(1060);

const defaultCardWidth = 1060;
const defaultCardHeight = 668;
const cardAspectRatio = defaultCardWidth / defaultCardHeight;

const baseCompanyNameFontSize = 4.2;
const baseCardNumberFontSize = 2.2;
const baseCardHolderNameFontSize = 2.2;

const cardStyle = computed(() => ({
  width: '100%',
  aspectRatio: `${cardAspectRatio}`,
}));

const scaleFactor = computed(() => {
  if (defaultCardWidth === 0) return 1;
  return actualCardWidth.value / defaultCardWidth;
});

const companyNameStyle = computed(() => ({
  fontSize: `${baseCompanyNameFontSize * scaleFactor.value}rem`
}));

const cardNumberStyle = computed(() => ({
  fontSize: `${baseCardNumberFontSize * scaleFactor.value}rem`
}));

const cardHolderNameStyle = computed(() => ({
  fontSize: `${baseCardHolderNameFontSize * scaleFactor.value}rem`
}));

const settings = {
  maxTilt: 18,
  perspective: 750,
  scale: 1.075,
  speed: 1000,
  easing: "cubic-bezier(.03,.98,.52,.99)",
  extrude: 2,
  shadowBaseY: 8,
  shadowBaseBlur: 20,
  shadowBaseOpacity: 0.2,
};

let handleMouseMove, handleMouseLeave, handleMouseEnter;
let resizeObserver;

onMounted(() => {
  const card = cardRef.value;
  if (!card) return;

  const updateWidth = () => {
    if (cardRef.value) {
      actualCardWidth.value = cardRef.value.offsetWidth;
    }
  };

  resizeObserver = new ResizeObserver(updateWidth);
  resizeObserver.observe(card);
  updateWidth();

  handleMouseEnter = () => {
    card.style.transition = `transform ${settings.speed / 2}ms ${settings.easing}, box-shadow ${settings.speed / 2}ms ${settings.easing}`;
    setTimeout(() => {
      if (card) card.style.transition = '';
    }, settings.speed / 2);
  };

  handleMouseMove = (e) => {
    if (!card) return;
    const rect = card.getBoundingClientRect();
    const componentWidth = card.offsetWidth;
    const componentHeight = card.offsetHeight;

    const mouseX = e.clientX - rect.left - componentWidth / 2;
    const mouseY = e.clientY - rect.top - componentHeight / 2;

    const rotateX = (settings.maxTilt * mouseY / (componentHeight / 2)).toFixed(2);
    const rotateY = -(settings.maxTilt * mouseX / (componentWidth / 2)).toFixed(2);

    const totalRotation = (Math.abs(parseFloat(rotateX)) + Math.abs(parseFloat(rotateY))) / 2;
    const currentExtrude = (totalRotation / settings.maxTilt) * settings.extrude;

    card.style.transform = `perspective(${settings.perspective}px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(${currentExtrude}px) scale3d(${settings.scale}, ${settings.scale}, ${settings.scale})`;

    const pointerX = (e.clientX - rect.left) / componentWidth * 100;
    const pointerY = (e.clientY - rect.top) / componentHeight * 100;
    card.style.setProperty('--pointer-x', `${pointerX}%`);
    card.style.setProperty('--pointer-y', `${pointerY}%`);
    card.style.setProperty('--glare-opacity-dynamic', '1');

    const absRotateX = Math.abs(parseFloat(rotateX));
    card.style.setProperty('--shadow-y', `${settings.shadowBaseY + absRotateX / 2}px`);
    card.style.setProperty('--shadow-blur', `${settings.shadowBaseBlur + absRotateX}px`);
    card.style.setProperty('--shadow-opacity', (settings.shadowBaseOpacity + absRotateX / 50).toFixed(2));
  };

  handleMouseLeave = () => {
    if (!card) return;
    card.style.transition = `transform ${settings.speed}ms ${settings.easing}, box-shadow ${settings.speed}ms ${settings.easing}`;
    card.style.transform = `perspective(${settings.perspective}px) rotateX(0deg) rotateY(0deg) translateZ(0px) scale3d(1, 1, 1)`;
    card.style.setProperty('--glare-opacity-dynamic', '0');
    card.style.setProperty('--shadow-y', `${settings.shadowBaseY}px`);
    card.style.setProperty('--shadow-blur', `${settings.shadowBaseBlur}px`);
    card.style.setProperty('--shadow-opacity', `${settings.shadowBaseOpacity}`);
  };

  card.addEventListener('mouseenter', handleMouseEnter);
  card.addEventListener('mousemove', handleMouseMove);
  card.addEventListener('mouseleave', handleMouseLeave);
});

onUnmounted(() => {
  const card = cardRef.value;
  if (card) {
    card.removeEventListener('mouseenter', handleMouseEnter);
    card.removeEventListener('mousemove', handleMouseMove);
    card.removeEventListener('mouseleave', handleMouseLeave);
  }
  if (resizeObserver && card) {
    resizeObserver.unobserve(card);
  }
});

</script>

<style scoped>
.card-wrapper {
  --card-radius: 30px;
  --glare-opacity-static: 0.5;
  --glare-opacity-dynamic: 0.1;
  --pointer-x: 50%;
  --pointer-y: 50%;
  --shadow-y-base: 8px;
  --shadow-blur-base: 20px;
  --shadow-opacity-base: 0.2;
  --shadow-y: var(--shadow-y-base);
  --shadow-blur: var(--shadow-blur-base);
  --shadow-opacity: var(--shadow-opacity-base);

  border-radius: var(--card-radius);
  position: relative;
  transform-style: preserve-3d;
  transform: perspective(1000px) rotateX(0deg) rotateY(0deg) scale3d(1,1,1);
  box-shadow: 0px var(--shadow-y) var(--shadow-blur) rgba(0, 0, 0, var(--shadow-opacity));

  overflow: hidden;
}

.card-wrapper img {
  width: 100%;
  height: 100%;
  border-radius: var(--card-radius);
  object-fit: cover;
  user-select: none;
  display: block;
}

.card-wrapper img.card-background-image {
  position: absolute;
  top: 0;
  left: 0;
  z-index: 0;
}

.card-wrapper img.card-chip {
  position: absolute;
  top: 0%;
  left: 0%;
  width: 100%;
  height: auto;
  z-index: 3;
  pointer-events: none;
  opacity: var(--glare-opacity-dynamic);
  mask-image: radial-gradient(
    circle at var(--pointer-x) var(--pointer-y),
    rgba(0, 0, 0, 1) 0%,
    rgba(0, 0, 0, 0) 50%
  );
  -webkit-mask-image: radial-gradient(
    circle at var(--pointer-x) var(--pointer-y),
    rgba(0, 0, 0, 1) 0%,
    rgba(0, 0, 0, 0) 50%
  );
}

.card-text-overlay {
  position: absolute;
  width: 100%;
  text-align: left;
  color: white;
  z-index: 4;
  pointer-events: none;
  text-shadow: 1px 1px 3px rgba(0,0,0,0.5);
}

.company-name {
  left: 0;
  padding: 0 5%;
  top: 10%;
  font-weight: bold;

}

.card-details {
  bottom: 10%;
  left: 0;
  padding: 0 5%;
  box-sizing: border-box;
}

.card-number {
  text-align: left;
  margin-bottom: 2%;
  font-family: 'Courier New', Courier, monospace;
  opacity: 0.5;
}

.card-holder-name {
  text-align: left;
  font-weight: 50;
}
</style>
