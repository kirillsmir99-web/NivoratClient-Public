# CooldownHUD

> Высокопроизводительный клиентский мод для Minecraft **Fabric 1.21.11** (тестовая сборка test3) с кастомным интерфейсом нового поколения, звуковым движком и боевыми модулями.

[![Minecraft](https://img.shields.io/badge/Minecraft-1.21.11-brightgreen.svg)](https://minecraft.net/)
[![Fabric](https://img.shields.io/badge/Loader-Fabric-blue.svg)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://openjdk.org/)
[![Version](https://img.shields.io/badge/Version-v1.0.0--test3-purple.svg)]()
[![Build](https://img.shields.io/badge/Build-test3-yellow.svg)]()
[![Developer](https://img.shields.io/badge/Developer-kt1xW-red.svg)](https://t.me/virionDEV)

---

## 🌟 Ключевые возможности

### 🎨 Интерфейс и графический движок (Dark Glass UI)
- **Стеклянный полупрозрачный дизайн**: динамическая прозрачность окон (30–100%) и карточек (20–100%), адаптивное размытие и стеклянные грани.
- **Пространственная анимация**: плавное открытие/закрытие с масштабным смещением (scale + 	ranslation + lpha) по кривым Безье.
- **Адаптивный респонсивный layout**: поддержка любых масштабов интерфейса (GUI Scale 2/3/4/Auto), экранов малого разрешения и PojavLauncher.
- **Перетаскивание и максимизация**: свободное позиционирование окна мышью с экранным ограничением и сохранением координат, кнопки **Refresh**, **Maximize/Restore**, **Close**.
- **Билингвальный поиск (Ctrl+F)**: мгновенная индексация всех 12 модулей и их параметров на русском и английском языках с автокомплитом и переходом к карточке.
- **Иерархический сайдбар**: раскрывающиеся категории (аккордеон), индикаторы активности модулей, быстрый доступ и пины.

### 🔤 Система типографики (Typography Engine)
- **Горячее переключение шрифтов**: поддержка runtime-переключения без перезапуска клиента:
  - Onest (современный гротеск с полным покрытием кириллицы)
  - Inter
  - Manrope
  - Rubik
  - Minecraft (ванильный пиксельный шрифт)
- **Физическое масштабирование текста**:
  - Маленький (0.90x)
  - Обычный (1.00x)
  - Крупный (1.15x)
  Синхронный пересчёт матриц рендеринга и метрик (UiTextRenderer) исключает обрезание текста и наезды элементов.
- **Безопасный фоллбэк**: четырёхуровневая цепочка резервных шрифтов (TTF -> space -> default -> unifont) исключает появление пустых квадратов.

### 🔊 Мультипрофильная звуковая система (Sound System)
- **3 звуковых профиля**:
  - Nivorat Serene (дефолтный тактильный пак)
  - Nivorat Classic
  - Minecraft (ванильные клики и щелчки)
- **Дискретная трещотка слайдеров ('трррк')**: тактильная аудио-обратная связь с динамическим питчем и аппаратным debounce.
- **Балансировка громкости**: нормализованное усиление калиброванных уровней без клиппинга и звукового спама.

### 🛡️ Безопасность настроек и пресеты
- **5-секундное удержание для сброса (Hold Confirmation)**: модальное окно с динамическим обратным отсчётом и заполнением полосы прогресса (0% -> 100%) при зажатии ЛКМ. Защищает от случайного сброса.
- **Менеджер пресетов**: сохранение, экспорт/импорт настроек через буфер обмена (JSON / Base64).

---

## ⚔️ Модули

| Категория | Модули | Описание |
|---|---|---|
| **Бой (Combat)** | AutoMace, AutoSpear, AutoShieldbreaker, AutoStunSlam | Автоматическая смена оружия, оптимизация ударов булавой, пробитие щитов, стан-комбо |
| **Защита (Defense)** | AutoTotem, AutoCart, AutoAnchor, CartRefill | Моментальный свап тотемов, взрыв вагонеток и якорей, автозакупка и пополнение |
| **Утилиты (Utility)** | HPReaper, AutoTool, AutoGG, ActivityHUD | Автоматический добив, умная смена инструментов, кастомный оверлей активности |

---

## 🛠️ Сборка из исходников

### Требования
- **Java Development Kit (JDK) 21**
- Git

### Команды сборки
`ash
# Клонирование репозитория
git clone https://github.com/kirillsmir99-web/NivoratClient.git
cd NivoratClient

# Сборка JAR мода (Fabric 1.21.1)
./gradlew clean build

# Скомпилированный артефакт появится в:
# build/libs/CooldownHUD.jar
```

---

## 👥 Сообщество и ссылки

- **Разработчик**: kt1xW
- **Telegram канал**: [t.me/virionDEV](https://t.me/virionDEV)
- **Discord**: [discord.gg/qkezDA7tFX](https://discord.gg/qkezDA7tFX)
- **YouTube**: [youtube.com/@Nivorat](https://www.youtube.com/@Nivorat)
- **TikTok**: [tiktok.com/@nivorat](https://www.tiktok.com/@nivorat)
- **Поддержка**: [donationalerts.com/r/nivorat](https://www.donationalerts.com/r/nivorat)
